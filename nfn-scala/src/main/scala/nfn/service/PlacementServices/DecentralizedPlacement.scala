package nfn.service.PlacementServices

import SACEPICN.{Map, NodeMapping, Paths}
import akka.actor.ActorRef
import nfn.tools.EvaluationHandler

import scala.collection.mutable.ListBuffer

class DecentralizedPlacement(_nodeName:String, _mapping: NodeMapping, _ccnApi: ActorRef, _root: Map, _paths: ListBuffer[Paths], _maxPath: Int, _evalHandler: EvaluationHandler, _opCount: Int) extends Placement{
  override val nodeName: String = _nodeName
  override val mapping: NodeMapping = _mapping
  override val ccnApi: ActorRef = _ccnApi
  override val root: Map = _root
  override val paths:ListBuffer[Paths] = _paths
  override val maxPath: Int = _maxPath
  override val evalHandler: EvaluationHandler = _evalHandler
  override val opCount:Int = _opCount
/*
  var selectedPathDecentral = ""
  var selectedPathEnergyVariance = new mutable.HashMap[String, String]()
  var selectedPathOverheadVariance = new mutable.HashMap[String, String]()
  //2) For this size: Get the path with atleast OpCount - 1 hops and the lowest BDP path:
  //In this case we will try to place an OP on each node directly next to the root.
  //E.g. If node 1 is only connected to node 2. And node 2 has 4 other neighbours, then we will send the query to node 2.
  //So, for all paths, look at the second hop. E.g. Sample paths: 9001-9002, 9001-9002-9003, 9001-9002-9005. Here the second hop is always 9002. Therefore we will send the query to 9002.
  //Else in the case of 9001-9002, 9001-9003-9004, 9001-9005-9006. It means that we have more than 1 node connected to this node. So we could have done the placement on this one.
  //Getting the distinct second hops
  var secondHopNodes = new ArrayBuffer[String]
  for (path <- paths) {
    if (path.pathNodes.length > 1) {
      secondHopNodes += path.pathNodes.reverse(1)
      //The reason why we do not sanitize this and access the array element directly is because we cannot have a Path with 1 node. Hence PathNodes(1) will always contain a node.
      // If this is not the case then this requires a rework of the entire path discovery process. Currently, this is not the case and we are forming proper paths.
    }
  }
  //Get the distinct of the second hops:
  secondHopNodes = secondHopNodes.distinct
  //Remove root from list:
  secondHopNodes -= thisNode
  LogMessage(nodeName, s"Distinct 2nd hop nodes: ${secondHopNodes.mkString(" ")}")

  //Now check if the number of these distinct nodes is equal to or greater than the operator count
  var timeNow_Placement_Deployment = Calendar.getInstance().getTimeInMillis

  if (secondHopNodes.length >= opCount) {
    LogMessage(nodeName, s"Second hop nodes are more than the OP Count. We can explore this node and its neighbors")
    //This node has more network information (more neighboring paths it can explore)
    //We can use this node for placement
    //Select a path with min cumulative path cost with the required number of hops:
    implicit val order = Ordering.Double.TotalOrdering
    var selectedPath = paths.filter(x => x.hopCount == opCount).minBy(_.cumulativePathCost)

    selectedPathDecentral = selectedPath.pathNodes.mkString(" - ").toString
    //Getting the cumulative path energy and bdp:
    evalHandler.selectedPathEnergy = FormattedOutput.round(FormattedOutput.parseDouble((selectedPath.cumulativePathEnergy.sum / selectedPath.cumulativePathEnergy.length).toString()), 2)
    evalHandler.selectedPathOverhead = FormattedOutput.round(FormattedOutput.parseDouble((selectedPath.cumulativePathBDP.sum / selectedPath.cumulativePathBDP.length).toString()), 2)

    //Manage the adaptive path weights that changed over time
    selectedPathEnergyVariance = selectedPath.hopWeights_Energy
    selectedPathOverheadVariance = selectedPath.hopWeights_BDP

    evalHandler.overallPlacementOverhead = selectedPath.cumulativePathCost

    //Take this path and place the queries on it:
    //1) For this we will need to process the tree:

    LogMessage(nodeName, s"The selected path is: $selectedPathDecentral")

    LogMessage(nodeName, s"Operator Placement Started")
  }

  //Here we will get the tree with placement done
  var placementRoot = processPlacementTree(root._root, selectedPath.pathNodes.reverse.toBuffer[String])
  LogMessage(nodeName, s"Operator Placement Completed")

  LogMessage(nodeName, s"Query Deployement Started")*/

  override def process(): String = ???
}
