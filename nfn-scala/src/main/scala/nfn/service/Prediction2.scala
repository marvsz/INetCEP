package nfn.service

import INetCEP.StatesSingleton
import nfn.tools.{Helpers, SensorHelpers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
//Added for contentfetch
import akka.actor.ActorRef

import scala.language.postfixOps

//Added for contentfetch
import ccn.packet.CCNName

//Added for CCN Command Execution:
import config.StaticConfig

class Prediction2 extends NFNService {
  val sacepicnEnv = StaticConfig.systemPath
  //var historyArray: Array[Double] = null
  var currentGranularity = -1
  var currentAverage = 0.0

  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future{
    val nodeInfo = interestName.cmps.mkString(" ")
    val nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1);

    def predictionHandler(granularity: String, streamName: String, dataStream: String): String = {
      LogMessage(nodeName,"PREDICTION: Started prediction")
      val nameOfState = s"/state${interestName.toString}"
      val historyArray: Array[Double] = stateHolder.getPredictionState(nameOfState)
      var output = predict(SensorHelpers.parseData("data",dataStream), granularity,historyArray, nameOfState,stateHolder)
      if (output != "")
        output = output.stripSuffix("\n").stripMargin('#')
      else
        output += "No Results!"
      LogMessage(nodeName,"PREDICTION: Finished prediction")
      LogMessage(nodeName, s"RREDICTION: output; $output")
      output
    }

    def initialPredictionHandler(granularity:String, streamName: String): String ={
      LogMessage(nodeName,"PREDICTION: Started initial prediction")
      "PREDICTION: Started initial prediction"
    }

    NFNStringValue(
      args match {
        case Seq(granularity: NFNStringValue, streamName: NFNStringValue, dataStream: NFNStringValue) => predictionHandler(granularity.str, streamName.str, dataStream.str)
        case Seq(granularity: NFNStringValue, streamName: NFNStringValue) => initialPredictionHandler(granularity.str, streamName.str)
        case _ =>
          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
      }
    )
  }

  /**
    *
    * @param data                 the data on which to perform the prediction on
    * @param granularity the granularity at which rate predictions are made
    * @return a string with predictions written in it. each prediction is in a new line and is a new tuple of information
    */
  def predict(data: List[String], granularity: String, stateArray: Array[Double], nameOfState:String,stateHolder:StatesSingleton): String = {
    var historyArray: Array[Double] = stateArray
    val streamName = data.head.split("-")(0).trim
    val streamData = data.drop(1)
    val schema = data.head.split("-")(1).trim
    val granularityInSeconds = granularity.takeRight(1).toLowerCase() match {
      case "s" => granularity.dropRight(1).toInt
      case "m" => granularity.dropRight(1).toInt * 60
      case "h" => granularity.dropRight(1).toInt * 60 * 60
      case _ => 0
    }
    val historyGranularity = Math.round(86400 / granularityInSeconds)
    if (historyArray == null) {
      historyArray = Array.ofDim[Double](historyGranularity)
    }
    val output = new StringBuilder
    val newSchema = "date,window_id,house_id,household_id,plug_id,value"
    val newStreamName = s"PREDICT2(${granularity},${streamName})"
    output.append(newStreamName + " - " + newSchema + "\n")
    if (data.nonEmpty && !data.head.contains("No Results!")) {
      val delimiter = SensorHelpers.getDelimiterFromLine(schema)
      val valuePositions = SensorHelpers.getColumnIDs(schema,"value",delimiter)
      val datePosition = SensorHelpers.getColumnIDs(schema,"date",delimiter)
      val propertyPositions = SensorHelpers.getColumnIDs(schema,"property",delimiter)
      val plugIdPositions = SensorHelpers.getColumnIDs(schema,"Plug_ID",delimiter)
      val householdIdPositions = SensorHelpers.getColumnIDs(schema,"Household_ID",delimiter)
      val houseIdPositions = SensorHelpers.getColumnIDs(schema,"House_ID",delimiter)

      val initialSecondsOfDay = SensorHelpers.parseTime(streamData.head.split(delimiter)(datePosition.head).stripPrefix("(").stripSuffix(")").trim, delimiter)
      val initialTimeStamp = initialSecondsOfDay.getHour * 60 * 60 + initialSecondsOfDay.getMinute * 60 + initialSecondsOfDay.getSecond
      if(currentGranularity == -1){
        currentGranularity = Math.round(initialTimeStamp / granularityInSeconds)
      }

      for (line <- streamData) {
        //Iterate through each line
        if (line != "") {
          val timeStamp = SensorHelpers.parseTime(line.split(delimiter)(datePosition.head).stripPrefix("(").stripSuffix(")").trim, delimiter) // get the time stamp of one tuple
          val secondsOfTheDay = timeStamp.getHour * 60 * 60 + timeStamp.getMinute * 60 + timeStamp.getSecond // calculates how many seconds have passed since midnight
          val correspondingGranularity = Math.round(secondsOfTheDay / granularityInSeconds) // maps the timestamp to a corresponding granularity in the history Array.

          //set the boundaryPassed variable to check if a time window in the granularity is passed and we have to emit a prediction
          if (currentGranularity < correspondingGranularity) { // if it is time for a new prediction
            val windowIdForPrediction = (correspondingGranularity + 2) % historyArray.size // the time we make a prediction for
            val hId = line.toString.split(delimiter)(houseIdPositions.head).trim.toInt
            val hhId = line.toString.split(delimiter)(householdIdPositions.head).trim.toInt
            val plgId = line.toString.split(delimiter)(plugIdPositions.head).trim.toInt
            //we now iterate through all houses, households and plugs and make a prediction for each and everyone.

              if (historyArray(windowIdForPrediction) != 0) {
                output.append(timeStamp.toString + "," + windowIdForPrediction + "," + hId + "," + hhId + "," + plgId + "," + ((historyArray(windowIdForPrediction) + currentAverage) / 2) + "\n")
              }
              else {
                output.append(timeStamp.toString + "," + windowIdForPrediction + "," + hId + "," + hhId + "," + plgId + "," + currentAverage +"\n")
              }

            historyArray(correspondingGranularity) = (historyArray(correspondingGranularity) + currentAverage) / 2
            currentGranularity = (currentGranularity + 1) % historyArray.size
            currentAverage = 0
          }

          // check if the property is the work or the load. 1 is load 0 is work, we are only interested in accumulating the load
          if (line.toString.split(delimiter)(propertyPositions.head).trim.toInt == 1) {
            // if it is not time for a prediction we store the data in between predictions in a temporary array. We use it later for when it is time for a prediction
            currentAverage = (currentAverage + line.toString.split(delimiter)(valuePositions.head).trim.toDouble) / 2
          }
        }

      }
      stateHolder.updatePrediction2State(nameOfState,historyArray)
      output.toString()
    }
    else {
      stateHolder.updatePrediction2State(nameOfState,historyArray)
      output.toString()
    }
  }

  def handlePrediction() = {

  }


}
