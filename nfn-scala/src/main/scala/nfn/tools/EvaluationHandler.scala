package nfn.tools

import java.text.SimpleDateFormat
import java.util.Calendar

class EvaluationHandler {
  var runTime: String = s"${new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Calendar.getInstance.getTime)}"
  var runID = 0
  var selectedPath = ""
  var selectedPathEnergy = 0.0
  var selectedPathOverhead = 0.0
  var overallPlacementOverhead = 0.0
  private var startTime: Long = 0
  private var endTime: Long = 0
  private var timeOffset:Long = 0
  private var startTime_OpTreeCreation: Long = 0
  private var endTime_OpTreeCreation: Long = 0
  private var timeOffset_OpTreeCreation: Long = 0
  private var startTime_NodeDiscovery: Long = 0
  private var endTime_NodeDiscovery: Long = 0
  private var timeOffset_NodeDiscovery: Long = 0
  private var startTime_Placement_Deployment: Long = 0
  private var endTime_Placement_Deployment: Long = 0
  private var timeOffset_Placement_Deployment: Long = 0

  def setStartTime_Placement_Deployment(): Unit={
    startTime_Placement_Deployment = Calendar.getInstance().getTimeInMillis
  }

  def setEndTime_Placement_Deployment(): Unit={
    endTime_Placement_Deployment = Calendar.getInstance().getTimeInMillis
    timeOffset_Placement_Deployment = endTime_Placement_Deployment - startTime_Placement_Deployment
  }

  def getTimeOffset_Placement_Deployment:Long={
    timeOffset_Placement_Deployment
  }

  def setStartTimeNow(): Unit ={
    startTime = Calendar.getInstance().getTimeInMillis
  }

  def setEndTimeNow():Unit ={
    endTime = Calendar.getInstance().getTimeInMillis
    timeOffset = endTime - startTime
  }

  def getTimeOffset:Long={
    timeOffset
  }

  def setStartTime_OpTreeCreation(): Unit={
    startTime_OpTreeCreation = Calendar.getInstance().getTimeInMillis
  }

  def setEndTimeNow_OpTreeCreation(): Unit ={
    endTime_OpTreeCreation = Calendar.getInstance().getTimeInMillis
    timeOffset_OpTreeCreation = endTime_OpTreeCreation - startTime_OpTreeCreation
  }

  def getTimeOffset_OpTreeCreation:Long={
    timeOffset_OpTreeCreation
  }

  def setStartTime_NodeDiscovery(): Unit={
    startTime_NodeDiscovery = Calendar.getInstance().getTimeInMillis
  }

  def setEndTime_NodeDiscovery(): Unit={
    endTime_NodeDiscovery = Calendar.getInstance().getTimeInMillis
    timeOffset_NodeDiscovery = endTime_NodeDiscovery - startTime_NodeDiscovery
  }

  def getTimeOffset_NodeDiscovery:Long={
    timeOffset_NodeDiscovery
  }

}
