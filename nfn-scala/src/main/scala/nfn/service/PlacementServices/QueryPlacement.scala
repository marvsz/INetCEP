package nfn.service.PlacementServices

/**
 * Created by Ali on 06.02.18.
 * This is the centralized query placement while using the fetch based network discovery approach
 */

import SACEPICN.{NodeMapping, _}
import akka.actor.{ActorContext, ActorRef}
import myutil.FormattedOutput
import nfn.service._
import nfn.tools._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.concurrent.duration._

//Added for contentfetch
import java.util.Calendar

import ccn.packet.CCNName

//Added for CCN Command Execution:

class QueryPlacement() extends NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = Future{


    //Algorithm: Centralized or Decentralized
    //RunID: Used primarily for the query service. Client passes runID = 1, query service then increments this. Runs with runID > 1 signify queries coming from the Query Service and not from consumers.
    //SourceOfQuery: Client will use 'Source', Query Service will use 'QS' or 'DQ'.
    //ClientID: Client who requested the query, usually specified by client id. Currently we don't have client identification, so we simply use 'Client1'
    //Query: The complex query to process
    //Region: User Region to hit for sensors (currently unused but can be used in future work)
    //Timestamp: Used to distinguish the time of arrival for the queries
    def processQuery(algorithm: String, processing: String, runID: String, sourceOfQuery: String, clientID: String, query: String, region: String, timestamp: String): String = {
      val algorithmEnum = 0
      var maxPath = 0

      //Get current node from interest:
      val nodeInfo = interestName.cmps.mkString(" ")
      val nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)
      LogMessage(nodeName, s"Query Execution Started")

      //Set Mapping:
      val mapping = new NodeMapping()
      //Get thisNodePort:
      val thisNode = mapping.getPort(nodeName)
      //Log Query for interval based trigger:
      /*if (Helpers.save_to_QueryStore(algorithm, processing, runID, sourceOfQuery, thisNode, clientID, query, region, timestamp)) {
        LogMessage(nodeName, s"Query appended to Query Store - Source $sourceOfQuery")
      } else
        LogMessage(nodeName, s"Query NOT appended to Query Store - Source $sourceOfQuery")*/

      //Initialize ExpressionTree class:

      var et = new OperatorTree()
      val evalHandler: EvaluationHandler = new EvaluationHandler
      evalHandler.runID = runID.toInt
      //Create node stack or Node tree (current: Tree)
      LogMessage(nodeName, s"Operator Tree creation Started")

      evalHandler.setStartTime_OpTreeCreation()

      LogMessage(nodeName,s"Query is $query")
      val root: Map = et.createOperatorTree(query)
      val opCount = root._stackSize
      LogMessage(nodeName,s"Stacksize is $opCount")
      evalHandler.setEndTimeNow_OpTreeCreation()

      LogMessage(nodeName, s"Operator Tree creation Completed")

      //Get current Network Status and Path information:


      var allNodes:ListBuffer[NodeInfo] = null
      var paths: ListBuffer[Paths] = null
      if(!algorithm.toLowerCase.equals("local")) {
        evalHandler.setStartTime_NodeDiscovery()
        allNodes = getNodeStatus(algorithm.toLowerCase, thisNode, nodeName)
        if (allNodes.length < root._stackSize) {
          return "Query processing stopped."
        }
        paths = buildPaths(nodeName, thisNode, allNodes)
        evalHandler.setEndTime_NodeDiscovery()
        LogMessage(nodeName, s"Checking paths:")
        for (path <- paths) {
          LogMessage(nodeName, s"${path.pathNodes.reverse.mkString(" ") + " - BDP: " + path.cumulativePathCost + " - Hops: " + path.hopCount}")
          if (maxPath < path.hopCount) {
            maxPath = path.hopCount
          }
        }
      }

      //Now that we have all the paths we need: Place the queries on these paths:
      //1) Find the number of operators in the query:
      val placement: Placement = Placement(algorithm.toLowerCase, nodeName, mapping, ccnApi, root, paths, maxPath, evalHandler, opCount)

      val resultVal = placement.process()
      LogMessage(nodeName,s"result of Deployment is: $resultVal")
      resultVal
    }

    /**
     * Hier auch Future für nebenläufigkeit
     *
     * @param algorithm
     * @param thisNode
     * @param nodeName
     * @return
     */
    def getNodeStatus(algorithm: String, thisNode: String, nodeName: String): ListBuffer[NodeInfo] = {
      LogMessage(nodeName, s"Get Node Status Started - Algorithm => $algorithm")
      //Get all node information:
      val now = Calendar.getInstance()
      var allNodes = new ListBuffer[NodeInfo]()

      def getCentralizedNodeStatus: Unit ={
        for (e: NodeInformation <- NodeInformationSingleton.nodeInfoList.asScala) {
          val name = s"node/${e._nodeName}/state/nodeState"
          LogMessage(nodeName, s"sending the interest $name")
          //Get content from network
          val intermediateResult = Networking.fetchContent(name,ccnApi,500 milliseconds)
          if(intermediateResult.isDefined) {
            var ni = new NodeInfo(new String(intermediateResult.get.data))
            LogMessage(nodeName, s"Node Added: ${ni.NI_NodeName}")
            allNodes += ni
          }
          else
            LogMessage(nodeName, s"IntermediateResult in getNodeStatus was empty, this should not happen, debug here")
        }
        LogMessage(nodeName, s"Get Node Status Completed")
      }

      def getDecentralizedNodeStatus: Unit ={
        val kHops = Source.fromFile(Helpers.getDecentralizedKHops)
        var K = 0
        kHops.getLines().foreach {
          line: String =>
            K = FormattedOutput.toInt(line)
        }
        LogMessage(nodeName, s"Performing Decentralized lookup with $K hops")
        val decentralizedNodeInfo = DecentralizedNodeInformationSingleton.decentralizedNodeInformationHashMap.get(nodeName).asScala
        for ((k, e) <- decentralizedNodeInfo) {
          val name = s"/${e._port}/" + now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)
          if (e._hops <= K) {
            //Get content from network
            val intermediateResult = Helpers.executeNFNQueryRepeatedly(s"(call 2 /node/${e._nodeName}/nfn_service_GetContent '${name}')", e._nodeName, ccnApi, 15)
            if (intermediateResult != "") {
              var ni = new NodeInfo(intermediateResult)
              LogMessage(nodeName, s"Node Added: ${ni.NI_NodeName}")
              allNodes += ni
            }
          }
        }
        LogMessage(nodeName, s"Get Node Status Completed")
      }

      algorithm match {
        case "centralized" => getCentralizedNodeStatus
        case "decentralized" => getDecentralizedNodeStatus
        case "local" => ""
        case _ => throw NoSuchPlacementException(s"The Placement Strategy $algorithm does not exist\n")
      }
      allNodes
    }

    def buildPaths(nodeName: String, rootNode: String, nodes: ListBuffer[NodeInfo]): ListBuffer[Paths] = {
      LogMessage(nodeName, s"Building Paths Started")
      var paths = new ListBuffer[Paths]
      var root = nodes.filter(x => x.NI_NodeName == rootNode).head
      nodes -= root
      nodes.insert(0, root)

      var endOfPath = false

      var currentRoot = root.NI_NodeName

      var traversedNodes = new ListBuffer[NodeInfo]
      traversedNodes += root

      var traversalNodes: ListBuffer[NodeInfo] = getTraversalNodes(nodeName, root, nodes, new ListBuffer[NodeInfo], traversedNodes)
      LogMessage(nodeName, s"getTraversalNodes -> on Root")

      var firstHopList = new ListBuffer[HopObject]
      var firstpath = new HopObject()
      firstpath.hopName = root.NI_NodeName
      firstpath.hopLatency += 0.0
      firstpath.previousHop = null
      LogMessage(nodeName, s"oneStepTraverse ROOT added -> NULL -> ${firstpath.hopName}")
      firstHopList += firstpath

      //All new paths will now be in hopInfo:
      LogMessage(nodeName, s"oneStepTraverse -> on Root")
      var hopInfo = oneStepTraverse(nodeName, root, root, firstHopList)

      while (traversalNodes.nonEmpty) {
        var next = traversalNodes.head
        LogMessage(nodeName, s"While -> Next -> ${next.NI_NodeName}")

        LogMessage(nodeName, s"While -> Retrieve HopInfo")
        hopInfo.appendAll(oneStepTraverse(nodeName, root, next, hopInfo))

        LogMessage(nodeName, s"While -> Retrieve other traversal nodes")
        traversalNodes.appendAll(getTraversalNodes(nodeName, next, nodes, traversalNodes, traversedNodes))

        LogMessage(nodeName, s"While -> remove current node from traversal")
        traversalNodes = traversalNodes.tail

        traversedNodes += next
      }

      //Remove duplicate paths - since we traverse ALL possible HOPS and add the path and it's head:
      hopInfo = hopInfo.distinct

      //Get the utility function data:
      var multiObjFunction = IOHelpers.getMultiObjectiveFunctionMetrics

      var energyWeight = multiObjFunction(0)
      var bdpWeight = multiObjFunction(1)

      //Initialize adaptive weight assignment for each hop in the path:
      var previousEnergyWeight = 0.0
      var previousBDPWeight = 0.0

      //Recursively print the paths AND store in a path list:
      @tailrec
      def checkPath(hops: ListBuffer[HopObject]): String = {
        if (hops.isEmpty) {
          LogMessage(nodeName, s"Path Finished!")
          return "ok"
        }
        else {
          var current = hops.head
          LogMessage(nodeName, s"checkPath - Current => ${current.hopName}")
          var pathCost: Double = 0.0
          var pathString = ""
          var hopCount: Int = 0
          var pathNodes = new ListBuffer[String]

          var cumulativePathEnergy = new ListBuffer[Double]
          var cumulativePathBDP = new ListBuffer[Double]

          var hopWeights_Energy = scala.collection.mutable.HashMap[String, String]()
          var hopWeights_BDP = scala.collection.mutable.HashMap[String, String]()

          var lastHopEnergy = 0.0
          var lastHopBDP = 0.0

          while (current.previousHop != null) {
            LogMessage(nodeName, s"Current.Prev is not null - This node has a parent node => Path is: Parent -> Node = ${current.previousHop.hopName} -> ${current.hopName}");
            if (current.previousHop.previousHop == null
              || (current.previousHop.previousHop != null && (current.hopName != current.previousHop.previousHop.hopName))
            ) {
              var hopBDP: Double = 0.0
              var nodePower = nodes.filter(x => x.NI_NodeName == current.hopName)
              if (nodePower != null && nodePower.nonEmpty) {
                LogMessage(nodeName, s"Since we have a match - we will apply adaptive weightage")
                //Calculating the utility function for each hop:
                //Link cost = (Energy * Energy Weight) + (BDP * BDP Weight)
                //Adaptive Hop Weight assignment. Vary the adaptive Weights for all hops based on each hop change in Energy and BDP values.
                //Here, we initially start with 0.5,0.5 for both energy and bdp. We use Additive Increase, Additive Decrease to change the weights based on network conditions.
                LogMessage(nodeName, s"Previous Weight values were: Energy=${previousBDPWeight.toString} and BDP=${previousEnergyWeight.toString} ")
                if (previousBDPWeight == 0.0 && previousEnergyWeight == 0.0) {
                  LogMessage(nodeName, s"This is the first hop for adaptive weight application")
                  //Use standard 0.5,0.5
                  hopBDP = FormattedOutput.round(((nodePower.head.NI_Battery * energyWeight) + (current.hopLatency * bdpWeight)), 2)
                  previousBDPWeight = bdpWeight
                  previousEnergyWeight = energyWeight

                  lastHopBDP = current.hopLatency
                  lastHopEnergy = nodePower.head.NI_Battery
                  //By this time, we have the values of the weights and the hop metrics
                }
                else {
                  LogMessage(nodeName, s"This is a subsequent hop/s for adaptive weight application")
                  //This signifies that this is not the first hop in the path and now we should look at the previous hop values to determine whether
                  //we will increase or decrease a weight metric:
                  if (nodePower.head.NI_Battery >= lastHopEnergy && current.hopLatency <= lastHopBDP) {
                    //Additive increase on energy and additive decrease on bdp:
                    if (previousEnergyWeight < 1.00 && previousBDPWeight > 0.00) {
                      previousEnergyWeight = FormattedOutput.round(previousEnergyWeight + 0.1, 2)
                      previousBDPWeight = FormattedOutput.round(previousBDPWeight - 0.1, 2) //Multiplicative Decrease: /2 | Additive Decrease: - 0.1
                    }
                  }
                  //else check the other way around - if bdp is more than the last hop and energy is less.
                  else if (nodePower.head.NI_Battery <= lastHopEnergy && current.hopLatency >= lastHopBDP) {
                    //Additive increase on energy and additive decrease on bdp:
                    if (previousEnergyWeight > 0.00 && previousBDPWeight < 1.00) {
                      previousEnergyWeight = FormattedOutput.round(previousEnergyWeight - 0.1, 2) //Multiplicative Decrease: /2 | Additive Decrease: - 0.1
                      previousBDPWeight = FormattedOutput.round(previousBDPWeight + 0.1, 2)
                    }
                  }

                  //In all other cases, we will not vary these weights since they have to move up or down together.
                  //Now we can assign the new hop BDP based on the new weights:
                  hopBDP = FormattedOutput.round(((nodePower.head.NI_Battery * previousEnergyWeight) + (current.hopLatency * previousBDPWeight)), 2)

                  //Set the metrics for this hop so that it can be used in the next hop:
                  lastHopBDP = current.hopLatency
                  lastHopEnergy = nodePower.head.NI_Battery
                }
                //Adding hop link cost in the overall path cost:
                hopWeights_Energy += s"${current.hopName}" -> s"${previousEnergyWeight.toString}"
                hopWeights_BDP += s"${current.hopName}" -> s"${previousBDPWeight.toString}"

                cumulativePathEnergy += lastHopEnergy
                cumulativePathBDP += lastHopBDP

                pathCost += hopBDP

              }
              else {
                LogMessage(nodeName, s"Hop ${current.hopName} information not found in list of nodes")
              }
              pathString = s" --(BDP: $hopBDP)--> " + current.hopName + " " + pathString
              //Adding a hop
              hopCount = hopCount + 1

              pathNodes += current.hopName
            }

            current = current.previousHop
          }
          //Just for correct visual representation: Not utilized since the latency is always 0 in this case.
          if (current.previousHop == null) {
            pathString = s"NULL --(${current.hopLatency})--> " + current.hopName + " " + pathString
            //This is the root node:
            hopCount = hopCount + 1
            pathNodes += current.hopName
          }

          var path = new Paths()
          path.hopStringRepresentation = pathString
          path.hopCount = hopCount
          path.cumulativePathCost = pathCost
          path.pathNodes = pathNodes.toArray
          path.cumulativePathBDP = cumulativePathBDP.toArray
          path.cumulativePathEnergy = cumulativePathEnergy.toArray
          path.hopWeights_Energy = hopWeights_Energy
          path.hopWeights_BDP = hopWeights_BDP
          paths += path

          LogMessage(nodeName, s"Path: $pathString\n")

          checkPath(hops.tail)
        }
      }

      checkPath(hopInfo)

      //Remove any duplicate paths that were created due to hop-linking:
      paths = paths.distinct

      LogMessage(nodeName, s"Building Paths Completed")
      paths
    }

    def getTraversalNodes(nodeName: String, cNode: NodeInfo, nodes: ListBuffer[NodeInfo], traversalNodes: ListBuffer[NodeInfo], traversedNodes: ListBuffer[NodeInfo]): ListBuffer[NodeInfo] = {
      var retNodes = new ListBuffer[NodeInfo]()
      for (latency <- cNode.NI_Latency) {
        var node = nodes.filter(x => x.NI_NodeName == latency.Lat_Node)

        if (node != null && node.nonEmpty) {
          var _node = nodes.filter(x => x.NI_NodeName == latency.Lat_Node).head
          if (!traversalNodes.contains(_node) && !traversedNodes.contains(_node)) {
            LogMessage(nodeName, s"add new Node in traversal List -> ${_node.NI_NodeName}")
            retNodes += _node
          }
        }

      }
      retNodes
    }

    def oneStepTraverse(nodeName: String, myRoot: NodeInfo, cNode: NodeInfo, cPath: ListBuffer[HopObject]): ListBuffer[HopObject] = {
      var retVal = new ListBuffer[HopObject]()
      for (latency <- cNode.NI_Latency) {
        LogMessage(nodeName, s"oneStepTraverse -> ${latency.Lat_Node} - ${latency.Lat_Latency}")
        //find root node in CPath:
        val pathRoot = cPath.filter(x => x.hopName == cNode.NI_NodeName)
        if (pathRoot != null) {
          var path = new HopObject()
          path.hopName = latency.Lat_Node
          path.hopLatency += latency.Lat_Latency
          path.previousHop = pathRoot.head; //get the first head
          LogMessage(nodeName, s"oneStepTraverse hop added -> ${path.previousHop.hopName} -> ${path.hopName}")
          retVal += path
        }
      }
      retVal
    }

    NFNStringValue(
      args match {
        case Seq(algorithm: NFNStringValue, processing: NFNStringValue, runID: NFNStringValue, sourceOfQuery: NFNStringValue, clientID: NFNStringValue, query: NFNStringValue, region: NFNStringValue, timestamp: NFNStringValue) => processQuery(algorithm.str, processing.str, runID.str, sourceOfQuery.str, clientID.str, query.str, region.str, timestamp.str)
        case _ =>
          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
      }
    )
  }
}
