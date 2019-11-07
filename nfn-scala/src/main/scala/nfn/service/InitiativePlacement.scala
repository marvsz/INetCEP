package nfn.service

import SACEPICN.{Map, NodeMapping, Paths}
import akka.actor.ActorRef
import myutil.FormattedOutput
import nfn.tools.{EvaluationHandler, Helpers, IOHelpers}

import scala.collection.mutable.ListBuffer
import scala.math.Ordering.Double

class InitiativePlacement(_nodeName: String, _mapping: NodeMapping, _ccnApi: ActorRef, _root: Map, _paths: ListBuffer[Paths], _maxPath: Int, _evalHandler: EvaluationHandler, _opCount: Int) extends Placement {
  override val nodeName: String = _nodeName
  override val mapping: NodeMapping = _mapping
  override val ccnApi: ActorRef = _ccnApi
  override val root: Map = _root
  override val paths: ListBuffer[Paths] = _paths
  override val maxPath: Int = _maxPath
  override val evalHandler: EvaluationHandler = _evalHandler
  override val opCount: Int = _opCount

  override def process(): String = {
    var output = ""
    if (root._stackSize > maxPath)
      LogMessage(nodeName, s"Stack size of node is too big. Stacksize is $opCount, but we can only support $maxPath operators")
    implicit val order: Double.TotalOrdering.type = Ordering.Double.TotalOrdering
    val optimalPath: Paths = paths.filter(x => x.hopCount == opCount).minBy(_.cumulativePathCost)

    evalHandler.selectedPath = optimalPath.pathNodes.mkString("-").toString

    //Getting the cumulative path energy and bdp:

    evalHandler.selectedPathEnergy = FormattedOutput.round(FormattedOutput.parseDouble((optimalPath.cumulativePathEnergy.sum / optimalPath.cumulativePathEnergy.length).toString), 2)

    evalHandler.selectedPathOverhead = FormattedOutput.round(FormattedOutput.parseDouble((optimalPath.cumulativePathBDP.sum / optimalPath.cumulativePathBDP.length).toString), 2)
    evalHandler.overallPlacementOverhead = optimalPath.cumulativePathCost
    //Manage the adaptive path weights that changed over time
    LogMessage(nodeName, s"Checking optimal path energy weights:")
    for (weight <- optimalPath.hopWeights_Energy) {
      LogMessage(nodeName, s"$weight")
    }
    LogMessage(nodeName, s"Checking optimal path BDP weights:")
    for (weight <- optimalPath.hopWeights_BDP) {
      LogMessage(nodeName, s"$weight")
    }

    evalHandler.setStartTime_Placement_Deployment()

    LogMessage(nodeName, s"Operator Placement Started")

    //Here we will get the tree with placement done

    val placementRoot = processPlacementTree(root._root, optimalPath.pathNodes.reverse.toBuffer[String])
    LogMessage(nodeName, s"Operator Placement Completed")

    LogMessage(nodeName, s"Query Deployement Started")

    val deployedRoot = processDeploymentTree(placementRoot)

    evalHandler.setEndTime_Placement_Deployment()

    LogMessage(nodeName, s"Query Deployement Completed")

    //Output is what we send back as the final result:
    output = deployedRoot._value
    output = Helpers.executeInterestQuery(output, nodeName, ccnApi)
    if (output != null && !output.isEmpty)
      output = output.stripSuffix("\n").stripMargin('#')
    else
      output += "No Results!"
    evalHandler.setEndTimeNow()
    LogMessage(nodeName, s"Query Execution Completed")
    LogMessage(nodeName, s"Query Output = $output")
    val outputForPrecision = s"${evalHandler.runID.toString}"
    //Generate Output:
    //Format: runID, Time, ResponseTime, OpTreeTime, NodeDiscoveryTime, Placement_DeploymentTime, Path, CumulativePathEnergy, CumulativePathOverhead (BDP):
    val output_for_Run = s"${evalHandler.runID.toString},${evalHandler.runTime.toString},${evalHandler.getTimeOffset().toString}," +
      s"${evalHandler.getTimeOffset_OpTreeCreation().toString},${evalHandler.getTimeOffset_NodeDiscovery().toString}," +
      s"${evalHandler.getTimeOffset_Placement_Deployment().toString},${evalHandler.selectedPath}," +
      s"${evalHandler.overallPlacementOverhead.toString},${evalHandler.selectedPathEnergy.toString}," +
      s"${evalHandler.selectedPathOverhead.toString}"

    var energyWeightString = ""
    var overheadWeightString = ""
    optimalPath.hopWeights_Energy.foreach {
      case (key, value) => energyWeightString += s"$key-$value "
    }
    energyWeightString.trim()
    optimalPath.hopWeights_BDP.foreach {
      case (key, value) => overheadWeightString += s"$key-$value "
    }
    overheadWeightString.trim()
    val output_for_AdaptiveWeights = s"${evalHandler.runID.toString},${evalHandler.runTime.toString},${evalHandler.getTimeOffset().toString}," +
      s"${evalHandler.selectedPath},$energyWeightString,$overheadWeightString"

    IOHelpers.writeOutputFiles(output_for_Run, output_for_AdaptiveWeights, outputForPrecision, output)
    LogMessage(nodeName, s"Output Files written, Query store should be processed again now.")
    output
  }
}
