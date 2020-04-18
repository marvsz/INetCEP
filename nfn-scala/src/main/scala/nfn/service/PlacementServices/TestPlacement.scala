package nfn.service.PlacementServices
import INetCEP.{Map, Node, NodeMapping, Operator, Paths}
import akka.actor.ActorRef
import nfn.service.LogMessage
import nfn.tools.{EvaluationHandler, Helpers}

import scala.collection.mutable.ListBuffer

class TestPlacement(_nodeName: String, _mapping: NodeMapping, _ccnApi: ActorRef, _root: Map, _paths: ListBuffer[Paths], _maxPath: Int, _evalHandler: EvaluationHandler, _opCount: Int) extends Placement {
  override val nodeName: String = _nodeName
  override val mapping: NodeMapping = _mapping
  override val ccnApi: ActorRef = _ccnApi
  override val root: INetCEP.Map = _root
  override val paths: ListBuffer[Paths] = _paths
  override val maxPath: Int = _maxPath
  override val evalHandler: EvaluationHandler = _evalHandler
  override val opCount: Int = _opCount

  val thisNode = "9001"

  override def processDeploymentTree(currentNode: Node): Node = {
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
        currentNode._executionNodePort = thisNode
        currentNode._executionNode = "nodeA"

        val name = currentNode._query.replace("nodeQuery", currentNode._executionNode)
        val query = currentNode._type match {
          case Operator.WINDOW => name
          case Operator.FILTER => name//.replace("[Q1]",currentNode.left._query.replace("nodeQuery",currentNode.left._executionNode))
          case Operator.JOIN => name//.replace("[Q1]",currentNode.left._query.replace("nodeQuery",currentNode.left._executionNode)).replace("[Q2]",currentNode.right._query.replace("nodeQuery",currentNode.right._executionNode))//.replace("[Q1]", currentNode.left._value).replace("[Q2]", currentNode.right._value)
          case Operator.AGGREGATION => name//.replace("[Q1]",currentNode.left._query.replace("nodeQuery",currentNode.left._executionNode))
          case Operator.SEQUENCE => name//.replace("[Q1]",currentNode.left._query.replace("nodeQuery",currentNode.left._executionNode))
          case Operator.PREDICT1 => name//.replace("[Q1]",currentNode.left._query.replace("nodeQuery",currentNode.left._executionNode))//.replace("[Q1]",currentNode.left._value)
          case Operator.PREDICT2 => name//.replace("[Q1]",currentNode.left._query.replace("nodeQuery",currentNode.left._executionNode))//.replace("[Q1]",currentNode.left._value)
          case Operator.HEATMAP => name//.replace("[Q1]",currentNode.left._query.replace("nodeQuery",currentNode.left._executionNode))//.replace("[Q1]",currentNode.left._value)
          case _ => name
        }
        currentNode._query = query
        LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - Query: $query")

        //Determine the location (name) where this query writeOutputFiles will be executed:

        val remoteNodeName = currentNode._executionNode//.currentNode._query.substring(currentNode._query.indexOf("/node/node") + 6, currentNode._query.indexOf("nfn_service") - 1)
        //val intermediateResult = Helpers.executeNFNQuery(currentNode._query,remoteNodeName,ccnApi,60)
        var callerQuery = "call 3 /node/nodeQuery/nfn_service_ServiceSubscriber 'Q1' 'Q2'".replace("Q1",currentNode._query.replaceAll("'","|")/*.replaceAll("[(]","").replaceAll("[)]","")*/).replace("nodeQuery", currentNode._executionNode)
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
          case _ => "WindowToDeploy"//Helpers.executeNFNQuery(currentNode._query,remoteNodeName,ccnApi,60)
        }
        currentNode._value = result

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

  override def process(): String = {
    var output = ""

    LogMessage(nodeName, s"Query Deployement Started")
    val deployedRoot = processDeploymentTree(root._root)
    LogMessage(nodeName, s"Query Deployement Completed")

    output = "Built Operator Tree."//deployedRoot._value
    //output = Helpers.executeInterestQuery(output, nodeName, ccnApi)
    LogMessage(nodeName, s"Query Execution Completed")
    output
  }

}
