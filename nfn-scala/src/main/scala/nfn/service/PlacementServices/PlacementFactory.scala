package nfn.service.PlacementServices

import INetCEP.{Map, Node, NodeMapping, Operator, Paths}
import akka.actor.ActorRef
import nfn.service.LogMessage
import nfn.tools.{EvaluationHandler, Helpers}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

trait Placement {
  val nodeName: String
  val mapping: NodeMapping
  val ccnApi: ActorRef
  val root: Map
  val paths: ListBuffer[Paths]
  val maxPath: Int
  val evalHandler: EvaluationHandler
  val opCount : Int

  def processPlacementTree(currentNode: Node, optimalPath: mutable.Buffer[String]):Node = {
    if (currentNode._Cprocessed) {
      LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - Placement complete.")
      currentNode
    }
    else {
      if (currentNode.right != null && !currentNode.right._Cprocessed) {
        LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - Right Exists. Process Right")
        processPlacementTree(currentNode.right, optimalPath)
      }
      else if (currentNode.left != null && !currentNode.left._Cprocessed) {
        LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - Left Exists. Process Left")
        processPlacementTree(currentNode.left, optimalPath)
      }
      else {
        if (optimalPath.nonEmpty) {
          LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - Processing Placement")

          currentNode._executionNodePort = optimalPath.last
          currentNode._executionNode = mapping.getName(currentNode._executionNodePort)

          var name = currentNode._query.replace("nodeQuery", currentNode._executionNode)
          val query = currentNode._type match {
            case Operator.WINDOW => name
            case Operator.FILTER => name
            case Operator.JOIN => name
            case Operator.AGGREGATION => name
            case Operator.SEQUENCE => name
            case Operator.PREDICT1 => name
            case Operator.PREDICT2 => name
            case Operator.HEATMAP => name
            case _ => name
          }
          currentNode._query = query
          LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - Query: $query")
          LogMessage(nodeName, s"Current Optimal Path ${optimalPath.mkString(" ")}")

          optimalPath -= optimalPath.last

          //This is the deployment part - we will do it in the next tree iteration:
          //currentNode._value = new String(NFNDataValue(fetchContent(NFNInterest(s"${name}"), ccnApi, 30 seconds).get.data).toDataRepresentation);
          //LogMessage(s"computed ${currentNode._value}\n")
          //currentNode._value = "Temp"
          currentNode._Cprocessed = true
          LogMessage(nodeName, s"CurrentNode: Doing recursion, back to Parent!")

          if (currentNode.parent == null)
            currentNode
          else
            processPlacementTree(currentNode.parent, optimalPath);
        }
        else {
          currentNode
        }
      }
    }
  }
  def processDeploymentTree(currentNode: Node): Node = {
    if (currentNode._Vprocessed) {
      LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - is Deployed.")
      currentNode
    }
    else {
      if (currentNode.right != null && !currentNode.right._Vprocessed) {
        LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - Right Exists. Process Right")
        processDeploymentTree(currentNode.right)
      }
      else if (currentNode.left != null && !currentNode.left._Vprocessed) {
        LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - Left Exists. Process Left")
        processDeploymentTree(currentNode.left)
      }
      else {
        LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - Deploying Operator")

        val name = currentNode._query
        val query = currentNode._type match {
          case Operator.WINDOW => name
          case Operator.FILTER => name //.replace("[Q1]",currentNode.left._value)
          case Operator.JOIN => name//.replace("[Q1]", currentNode.left._value).replace("[Q2]", currentNode.right._value)
          case Operator.AGGREGATION => name
          case Operator.SEQUENCE => name
          case Operator.PREDICT1 => name//.replace("[Q1]", currentNode.left._value)
          case Operator.PREDICT2 => name //.replace("[Q1]",currentNode.left._value)
          case Operator.HEATMAP => name//.replace("[Q1]", currentNode.left._value)
          case _ => name
        }

        currentNode._query = query
        LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - Query: $query")
        //currentNode._value = new String(fetchContentRepeatedly(NFNInterest(s"${currentNode._query}"), ccnApi, 30 seconds).get.data);
        //currentNode._value = executeNFNQuery(currentNode._query)

        //Determine the location (name) where this query wwriteOutputFilesill be executed:
        val remoteNodeName = currentNode._query.substring(currentNode._query.indexOf("/node/node") + 6, currentNode._query.indexOf("nfn_service") - 1)


        //In order to simulate network results (which can fail due to node availability or etc - we will comment out actual deployment and introduce a delay of 1.5 seconds which is the average query response time for a distributed network node.
        //This delay is based on the average delay noted during the last 50 runs. Log information is present in NodeA_Log.
        //var intermediateResult = createAndExecCCNQuery(remoteNodeName, currentNode._query, mapping.getPort(remoteNodeName), mapping.getIPbyName(remoteNodeName))
        //val intermediateResult = Helpers.executeNFNQueryRepeatedly(currentNode._query, remoteNodeName, ccnApi, 60)
        val callerQuery = "call 3 /node/nodeQuery/nfn_service_ServiceSubscriber 'Q1' 'Q2'".replace("Q1",currentNode._query.replaceAll("'","|")/*.replaceAll("[(]","").replaceAll("[)]","")*/).replace("nodeQuery", currentNode._executionNode)
        val result = currentNode._type match {
          case Operator.JOIN =>{
            Helpers.executeNFNQuery(callerQuery.replace("Q2",currentNode.right._query.replaceAll("'","|")/*.replaceAll("[(]","").replaceAll("[)]","")*/),remoteNodeName,ccnApi,60)
            Helpers.executeNFNQuery(callerQuery.replace("Q2",currentNode.left._query.replaceAll("'","|")/*.replaceAll("[(]","").replaceAll("[)]","")*/),remoteNodeName,ccnApi,60)
          }
          case Operator.PREDICT2 => {
            Helpers.executeNFNQuery(callerQuery.replace("Q2",currentNode.left._query.replaceAll("'","|")/*.replaceAll("[(]","").replaceAll("[)]","")*/),remoteNodeName,ccnApi,60)
          }
          case Operator.HEATMAP => {
            Helpers.executeNFNQuery(callerQuery.replace("Q2",currentNode.left._query.replaceAll("'","|")/*.replaceAll("[(]","").replaceAll("[)]","")*/),remoteNodeName,ccnApi,60)
          }
          case Operator.FILTER => {
            Helpers.executeNFNQuery(callerQuery.replace("Q2",currentNode.left._query.replaceAll("'","|")/*.replaceAll("[(]","").replaceAll("[)]","")*/),remoteNodeName,ccnApi,60)
          }
          case _ => "to Execute was a window, will be executed by the upper operator."//Helpers.executeNFNQuery(currentNode._query,remoteNodeName,ccnApi,60)
        }

        currentNode._value = result
        //currentNode._value = "TemporaryDeploymentValue";

        LogMessage(nodeName, s"Deployment result: ${currentNode._value}\n")
        currentNode._Vprocessed = true
        LogMessage(nodeName, s"CurrentNode: Execution completed. Doing recursion, back to Parent!")

        if (currentNode.parent == null) {
          val callerQuery = "call 3 /node/nodeQuery/nfn_service_ServiceSubscriber '(call 1 /node/nodeQuery/nfn_service_QueryResultPrinter)' 'Q2'".replace("nodeQuery",remoteNodeName).replace("Q2",currentNode._query.replaceAll("'","|"))
          val deploymentResult = Helpers.executeNFNQuery(callerQuery,remoteNodeName,ccnApi,60)
          LogMessage(nodeName,deploymentResult)
          currentNode
          // Here is the last one, call the printer of the result.
        } else
          processDeploymentTree(currentNode.parent)
      }
    }
  }
  def process():String
  /*def deployOperators(currentNode: Node, optimalPath: mutable.Buffer[String]):Node = {
    currentNode
  }*/
}

final case class NoSuchPlacementException(private val message: String = "", private val cause: Throwable = None.orNull) extends Exception(message, cause)

object Placement {

  def apply(s: String,_nodeName:String, _mapping: NodeMapping, _ccnApi: ActorRef, _root: Map, _paths: ListBuffer[Paths], _maxPath: Int, _evalHandler: EvaluationHandler, _opCount: Int): Placement = {
      s match {
        case "centralized" =>  new InitiativePlacement(_nodeName:String, _mapping: NodeMapping, _ccnApi: ActorRef, _root: Map, _paths: ListBuffer[Paths], _maxPath: Int, _evalHandler: EvaluationHandler, _opCount: Int)
        case "decentraliized" => new DecentralizedPlacement(_nodeName:String, _mapping: NodeMapping, _ccnApi: ActorRef, _root: Map, _paths: ListBuffer[Paths], _maxPath: Int, _evalHandler: EvaluationHandler, _opCount: Int)
        case "local" => new TestPlacement(_nodeName:String, _mapping: NodeMapping, _ccnApi: ActorRef, _root: Map, _paths: ListBuffer[Paths], _maxPath: Int, _evalHandler: EvaluationHandler, _opCount: Int)
        case _ => throw NoSuchPlacementException(s"The Placement Strategy $s does not exist\n")
      }
  }

}
