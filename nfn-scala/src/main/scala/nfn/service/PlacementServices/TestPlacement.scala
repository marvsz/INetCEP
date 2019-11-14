package nfn.service.PlacementServices
import SACEPICN.{Map, Node, NodeMapping, Operator, Paths}
import akka.actor.ActorRef
import nfn.service.LogMessage
import nfn.tools.{EvaluationHandler, Helpers}

import scala.collection.mutable.ListBuffer

class TestPlacement(_nodeName: String, _mapping: NodeMapping, _ccnApi: ActorRef, _root: Map, _paths: ListBuffer[Paths], _maxPath: Int, _evalHandler: EvaluationHandler, _opCount: Int) extends Placement {
  override val nodeName: String = _nodeName
  override val mapping: NodeMapping = _mapping
  override val ccnApi: ActorRef = _ccnApi
  override val root: SACEPICN.Map = _root
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
          case Operator.FILTER => name.replace("[Q1]",currentNode.left._value)
          case Operator.JOIN => name.replace("[Q1]", currentNode.left._value).replace("[Q2]", currentNode.right._value)
          case Operator.AGGREGATION => name
          case Operator.SEQUENCE => name
          case Operator.PREDICT1 => name.replace("[Q1]",currentNode.left._value)
          case Operator.PREDICT2 => name.replace("[Q1]",currentNode.left._value)
          case Operator.HEATMAP => name.replace("[Q1]",currentNode.left._value)
          case _ => name
        }
        currentNode._query = query
        LogMessage(nodeName, s"CurrentNode: ${currentNode._type} - Query: $query")

        //Determine the location (name) where this query wwriteOutputFilesill be executed:
        val remoteNodeName = currentNode._query.substring(currentNode._query.indexOf("/node/node") + 6, currentNode._query.indexOf("nfn_service") - 1)
        val intermediateResult = Helpers.executeNFNQuery(currentNode._query,remoteNodeName,ccnApi,60)
        currentNode._value = intermediateResult

        LogMessage(nodeName, s"Deployment result: ${currentNode._value}\n")
        currentNode._Vprocessed = true
        LogMessage(nodeName, s"CurrentNode: Execution completed. Doing recursion, back to Parent!")

        if (currentNode.parent == null)
          currentNode
        else
          processDeploymentTree(currentNode.parent)
      }
    }
  }

  override def process(): String = {
    var output = ""

    LogMessage(nodeName, s"Query Deployement Started")
    val deployedRoot = processDeploymentTree(root._root)
    LogMessage(nodeName, s"Query Deployement Completed")

    output = deployedRoot._value
    output = Helpers.executeInterestQuery(output, nodeName, ccnApi)
    LogMessage(nodeName, s"Query Execution Completed")
    output
  }

}