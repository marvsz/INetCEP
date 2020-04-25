package nfn.service

/**
 * Created by Ali on 06.02.18.
 */

import java.io.FileNotFoundException
import java.util.Calendar

import INetCEP.StatesSingleton
import akka.actor.ActorRef
import nfn.tools.{Helpers, Networking, SensorHelpers}

import scala.concurrent.Future
import scala.io.{BufferedSource, Source}
//Added for contentfetch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

import ccn.packet._
import config.StaticConfig
import myutil.FormattedOutput

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class Window() extends NFNService {

  val sacepicnEnv = StaticConfig.systemPath
  val DateTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
  var relativeTime: LocalTime = null

  override def function(interestName: CCNName, args: Seq[NFNValue], stateHolder: StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = {


    //Interest will always be in the form of: call X /node/nodeX/nfn_service_X
    //Using this we can extract the node for this operation.
    val nodeInfo = interestName.cmps.mkString(" ")
    val nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)


    @deprecated
    def processBoundWindow(deliveryFormat: NFNStringValue, sensor: NFNStringValue, lowerBound: NFNStringValue, upperBound: NFNStringValue): NFNValue = {
      LogMessage(nodeName, s"Window OP Started\n")

      //Working for timestamp
      val output = readBoundSensor(sensor.str, LocalTime.parse(lowerBound.str, DateTimeFormat), LocalTime.parse(upperBound.str, DateTimeFormat), nodeName)
      var contentWindow = NFNStringValue("No Results!")
      if (deliveryFormat.str.toLowerCase == "data") {
        return NFNStringValue(output)
      }
      else if (deliveryFormat.str.toLowerCase == "name") {
        val name = Helpers.storeOutputLocally(nodeName, output, "Window", sensor.str, ccnApi)
        LogMessage(nodeName, s"Inside Window -> WINDOW name: ${name}, WINDOW content: ${output}")
        LogMessage(nodeName, s"Window OP Completed")
        contentWindow = NFNStringValue(name.toString)
      }
      contentWindow
    }

    @deprecated
    def processTimeBoundWindow(deliveryFormat: NFNStringValue, sensor: NFNStringValue, timePeriod: NFNStringValue, timeUnit: NFNStringValue): Future[NFNValue] = Future {
      LogMessage(nodeName, s"Timed Window OP Started");

      val output = readRelativeTimedSensor(sensor.str, timePeriod.str.toLong, timeUnit.str, nodeName)
      var contentWindow = NFNStringValue("No Results!")
      if (deliveryFormat.str.toLowerCase == "data") {
        NFNStringValue(output)
      }
      else if (deliveryFormat.str.toLowerCase == "name") {
        val name = Helpers.storeOutputLocally(nodeName, output, "Window", sensor.str, ccnApi)
        LogMessage(nodeName, s"Inside Window -> WINDOW name: ${name}, WINDOW content: ${output}")
        LogMessage(nodeName, s"Window OP Completed")
        contentWindow = NFNStringValue(name.toString)
      }
      contentWindow
    }

    def placeWindowInterest(stream: String, interestedComputation: CCNName): Future[NFNValue] = Future {
      LogMessage(nodeName, s"Placing Entries in PIT and PQT accordingly: ${interestedComputation} on ${nodeName} is interested in ${stream}")
      Networking.makePersistentInterest(stream, interestedComputation, ccnApi)
      LogMessage(nodeName, s"Paced Window Interest")
      NFNStringValue("Placed Interests")
    }

    def processInitialSlidingEventWindow(deliveryFormat: NFNStringValue, sensor: NFNStringValue, numberOfEvents: NFNStringValue, interestedComputationName: CCNName): Future[NFNValue] = Future {
      LogMessage(nodeName, s"Started Initial Sliding Event Window Computation, without dataStream")
      val setting = sensor.str.split("/")(2).concat("/").concat(numberOfEvents.str)
      val nameOfState = s"/state/SlidingEventWindow/$setting"
      val returnValue = ""
      Helpers.storeState(nodeName, returnValue, "SlidingEventWindow", setting, ccnApi)
      Networking.makePersistentInterest(sensor.str, interestedComputationName, ccnApi)
      LogMessage(nodeName, s"Initial sliding Event Window Content should be empty and is: $returnValue")
      if (deliveryFormat.str.toLowerCase == "data")
        NFNStringValue(returnValue)
      else if (deliveryFormat.str.toLowerCase == "name") {
        NFNStringValue(nameOfState)
      }
      else
        NFNStringValue("Not a matching Return Format, Allowed are data and name")
    }

    def processInitialSlidingTimeWindow(deliveryFormat: NFNStringValue, sensor: NFNStringValue, timerPeriod: NFNStringValue, timeUnit: NFNStringValue, interestedComputationName: CCNName): Future[NFNValue] = Future {
      LogMessage(nodeName, s"Started Initial Sliding Time Window Computation, without dataStream")
      val setting = sensor.str.split("/")(2).concat("/").concat(timerPeriod.str).concat(timeUnit.str)
      val nameOfState = s"/state/SlidingTimeWindow/$setting"
      val returnValue = ""
      Helpers.storeState(nodeName, returnValue, "SlidingTimeWindow", setting, ccnApi)
      Networking.makePersistentInterest(sensor.str, interestedComputationName, ccnApi)
      LogMessage(nodeName, s"Initial sliding Time Window Content should be empty and is: $returnValue")
      if (deliveryFormat.str.toLowerCase == "data")
        NFNStringValue(returnValue)
      else if (deliveryFormat.str.toLowerCase == "name") {
        NFNStringValue(nameOfState)
      }
      else
        NFNStringValue("Not a matching Return Format, Allowed are data and name")
    }

    def processSlidingEventWindow(sensor: String, numberOfEvents: String, dataStream: String): Future[NFNValue] = Future {
      LogMessage(nodeName, s"Started Sliding Event Window Computation, dataStream is $dataStream")
      var stateContent = ""
      val nameOfState = s"/state${interestName.toString}"
      stateContent = stateHolder.getWindowState(nameOfState)
      if (stateContent == "Null") {
        LogMessage(nodeName, "State Content was empty")
        stateContent = ""
      }
      else
        LogMessage(nodeName, "Found State Content")
      /*
      val stateOptional: Option[Content] = Networking.fetchContent(nameOfState.toString,ccnApi,200 milliseconds)
      if(stateOptional.isDefined) {
        LogMessage(nodeName, "Found State Content")
        stateContent = new String(stateOptional.get.data)
      }
      else
        LogMessage(nodeName,"State Content was empty")

       */
      val sensorName = sensor.split("/")(3)
      val header = "Window(" + sensor + "," + numberOfEvents + ") - " + SensorHelpers.getSchema(sensorName) + "\n"
      val newStateContent = slideEventWindow(stateContent, dataStream, numberOfEvents.toInt)
      val retVal = header + newStateContent
      //Helpers.storeState(nodeName,returnValue,nameOfState,ccnApi)
      stateHolder.updateWindowState(nameOfState, newStateContent)
      LogMessage(nodeName, s"Sliding Event Window Content is $retVal")
      NFNStringValue(retVal)
    }

    def processUCLSlidingTimeWindow(sensor: String, timerPeriod: String, timeUnit: String, dataStream: String): Future[NFNValue] = Future {
      LogMessage(nodeName, s"Started Sliding Time Window Computation, dataStream is $dataStream")
      var stateContent = ""
      val nameOfState = s"/state${interestName.toString}"
      stateContent = stateHolder.getWindowState(nameOfState)
      if (stateContent == null) {
        LogMessage(nodeName, "State Content was empty")
        stateContent = ""
      }
      else {
        LogMessage(nodeName, "Found State Content")
      }
      /*
      val stateOptional: Option[Content] = Networking.fetchContent(nameOfState.substring(1).toString,ccnApi,200 milliseconds)
      if(stateOptional.isDefined) {
        LogMessage(nodeName, "Found State Content")
        stateContent = new String(stateOptional.get.data)
      }
      else
        LogMessage(nodeName,"State Content was empty")

       */
      val sensorName = sensor.split("/")(3)
      val header = "Window(" + sensor + "," + timerPeriod + "," + timeUnit + ") - " + SensorHelpers.getSchema(sensorName) + "\n"
      val newStateContent = slideTimedWindow(stateContent, dataStream, timerPeriod.toLong, timeUnit, nodeName)
      val retVal = header + newStateContent
      //Helpers.storeState(nodeName,returnValue,nameOfState,ccnApi)
      stateHolder.updateWindowState(nameOfState, newStateContent)
      LogMessage(nodeName, s"Slinding TIme Window Content is $retVal")
      NFNStringValue(retVal)
    }

    def processPRASlidingEventWindow(sensor: String, eventCount: String, periodicValue: String, predictionUnit: String): Future[NFNValue] = Future {
      LogMessage(nodeName, s"Started sliding Event Window")
      val timeInMills = FormattedOutput.getTimeInMills(periodicValue.toLong, predictionUnit)
      val sb = new StringBuilder
      var counter = 0
      val endCoutner = eventCount.toInt
      val sensorName = sensor.split("/")(3)
      val header = s"Window($sensor,$eventCount) - ${SensorHelpers.getSchema(sensorName)}"
      sb.append(header)
      while (counter < endCoutner) {
        val pre = System.currentTimeMillis()
        val intermediateResult = Networking.fetchContentFromNetwork(sensor, ccnApi, 500 milliseconds)
        if (intermediateResult.isDefined) {
          sb.append("\n")
          sb.append(new String(intermediateResult.get.data))
        }
        else {
          sb.append("\n")
          sb.append("Fail")
        }
        val after = System.currentTimeMillis()
        val delta = timeInMills - (after - pre)
        Thread.sleep(delta)
        counter = counter + 1
      }
      val newDataStreamName = s"node/$nodeName/Window($sensor,$eventCount)/${System.currentTimeMillis()}"
      val output = sb.toString()
      Networking.storeResult(nodeName, output, newDataStreamName, ccnApi)
      LogMessage(nodeName, s"Finished sliding Event Window and storing the data in $newDataStreamName. Result =\n$output")
      NFNStringValue(newDataStreamName)
    }

    def processPRASlidingTimeWindow(sensor: String, timerPeriod: String, timeUnit: String, periodicValue: String, predictionUnit: String): Future[NFNValue] = Future {
      LogMessage(nodeName, s"Started sliding Time Window")
      val timeInMills = FormattedOutput.getTimeInMills(periodicValue.toLong, predictionUnit)
      val sb = new StringBuilder
      val endTime = FormattedOutput.getFutureTime(java.time.LocalTime.now, timerPeriod.toLong, timeUnit)
      val sensorName = sensor.split("/")(3)
      val header = s"Window($sensor,$timerPeriod,$timeUnit) - ${SensorHelpers.getSchema(sensorName)}"
      sb.append(header)
      while (java.time.LocalTime.now.isBefore(endTime)) {
        val pre = System.currentTimeMillis()
        val intermediateResult = Networking.fetchContentFromNetwork(sensor, ccnApi, 500 milliseconds)
        if (intermediateResult.isDefined) {
          sb.append("\n")
          sb.append(new String(intermediateResult.get.data))
        }
        else {
          sb.append("\n")
          sb.append("Fail")
        }
        val after = System.currentTimeMillis()
        val delta = timeInMills - (after - pre)
        Thread.sleep(delta)
      }
      val newDataStreamName = s"node/$nodeName/Window($sensor,$timerPeriod,$timeUnit)/${System.currentTimeMillis()}"
      val output = sb.toString()
      Networking.storeResult(nodeName, output, newDataStreamName, ccnApi)
      LogMessage(nodeName, s"Finished sliding Event Window and storing the data in $newDataStreamName. Result =\n$output")
      NFNStringValue(newDataStreamName)
    }

    args match {


      case Seq(communicationApproach: NFNStringValue, sensor: NFNStringValue, numberOfEvents: NFNStringValue) =>
        placeWindowInterest(sensor.str, interestName).recover {
          case e => throw e
        }

      case Seq(communicationApproach: NFNStringValue, sensor: NFNStringValue, timerPeriodorNumberOfEvents: NFNStringValue, timeUnitOrDataStream: NFNStringValue) =>
        timeUnitOrDataStream.str.toLowerCase() match {
          case "s" => placeWindowInterest(sensor.str, interestName).recover {
            case e => throw e
          }
          case "m" => placeWindowInterest(sensor.str, interestName).recover {
            case e => throw e
          }
          case "h" => placeWindowInterest(sensor.str, interestName).recover {
            case e => throw e
          }
          case _ => processSlidingEventWindow(sensor.str, timerPeriodorNumberOfEvents.str, timeUnitOrDataStream.str).recover {
            case e => throw e
          }
        }

      // ~/INetCEP/ccn-lite/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9998 -w 10 "call 6 /node/nodeA/nfn_service_Window 'someTimeStamp' 'name' '/nodeA/sensor/gps1' '5' 's'" | ~/INetCEP/ccn-lite/bin/ccn-lite-pktdump -f 2
      case Seq(communicationApproach: NFNStringValue, sensor: NFNStringValue, timerPeriod: NFNStringValue, timeUnit: NFNStringValue, dataStream: NFNStringValue) =>
        processUCLSlidingTimeWindow(sensor.str, timerPeriod.str, timeUnit.str, dataStream.str).recover {
          case e => throw e
        }


      case Seq(timeStamp: NFNStringValue, communicationApproach: NFNStringValue, sensor: NFNStringValue, timerPeriod: NFNStringValue, periodicValue: NFNStringValue, periodicUnit: NFNStringValue) =>
        processPRASlidingEventWindow(sensor.str, timerPeriod.str, periodicValue.str, periodicUnit.str).recover {
          case e => throw e
        }

      case Seq(timeStamp: NFNStringValue, communicationApproach: NFNStringValue, sensor: NFNStringValue, timerPeriod: NFNStringValue, timeUnit: NFNStringValue, periodicValue: NFNStringValue, periodicUnit: NFNStringValue) =>
        processPRASlidingTimeWindow(sensor.str, timerPeriod.str, timeUnit.str, periodicValue.str, periodicUnit.str).recover {
          case e => throw e
        }

      case _ =>
        throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
    }


    //)
  }


  def slideTimedWindow(windowContent: String, newTuple: String, timerPeriod: Long, timeUnit: String, nodeName: String): String = {
    val delimiter = SensorHelpers.getDelimiterFromLine(newTuple)
    LogMessage(nodeName, s"delimiter is: $delimiter")
    val datePosition = SensorHelpers.getDatePosition(delimiter)
    LogMessage(nodeName, s"date position is: $datePosition")
    LogMessage(nodeName, s"new Tuple's date is ${newTuple.split(delimiter)(datePosition)}")
    val relativeTime: LocalTime = SensorHelpers.parseTime(newTuple.split(delimiter)(datePosition), delimiter)
    (windowContent.split("\n").filter(purgeOldData(_, relativeTime, timerPeriod, timeUnit)) :+ newTuple).mkString("\n")
  }

  def purgeOldData(tuple: String, timeStamp: LocalTime, timerPeriod: Long, timeUnit: String): Boolean = {
    if (tuple != "") {
      val pastTime = FormattedOutput.getPastTime(timeStamp, timerPeriod, timeUnit)
      val delimiter = SensorHelpers.getDelimiterFromLine(tuple)
      val datePosition = SensorHelpers.getDatePosition(tuple)
      val tupleTime = SensorHelpers.parseTime(tuple.split(delimiter)(datePosition), delimiter)
      if ((tupleTime.isAfter(pastTime) || tupleTime.equals(pastTime)) && (tupleTime.isBefore(timeStamp) || tupleTime.equals(timeStamp)))
        true
      else
        false
    }
    else
      false
  }

  def slideEventWindow(windowContent: String, newTuple: String, numberOfEvents: Int): String = {
    if ((windowContent.split("\n").length + 1) > numberOfEvents)
      (windowContent.split("\n").drop(1) :+ newTuple).mkString("\n")
    else
      (windowContent.split("\n") :+ newTuple).mkString("\n")
  }

  def readRelativeTimedSensor(path: String, timePeriod: Long, timeUnit: String, nodeName: String): String = {
    var bufferedSource: BufferedSource = null
    try {
      bufferedSource = Source.fromFile(s"$sacepicnEnv/sensors/" + path)
    }
    catch
    {
      case fileNotFoundException: FileNotFoundException => handleFileNotFoundException(fileNotFoundException, nodeName)
      case _: Throwable => LogMessage(nodeName, "Got some Kind of Exception")
    }
    val sb: StringBuilder = new StringBuilder
    var output: String = ""
    val lineList = bufferedSource.getLines().toList
    val delimiter: String = SensorHelpers.getDelimiterFromLine(lineList.head)
    val datePosition = SensorHelpers.getDatePosition(delimiter)

    if (relativeTime == null) {
      relativeTime = SensorHelpers.parseTime(lineList.head.split(delimiter)(datePosition), delimiter)
    }
    val futureTime = FormattedOutput.getFutureTime(relativeTime, timePeriod, timeUnit)
    //LogMessage(nodeName, s"Read Sensor from Current Time: ${relativeTime.toString}")
    //LogMessage(nodeName, s"Unitl Future Time: ${futureTime.toString}")
    for (line <- lineList) {
      val timeStamp = SensorHelpers.parseTime(line.split(delimiter)(datePosition), delimiter)
      if ((relativeTime.isBefore(timeStamp) || relativeTime.equals(timeStamp)) && futureTime.isAfter(timeStamp)) {
        sb.append(line + "\n")
      }
    }

    bufferedSource.close()
    relativeTime = futureTime
    output = sb.toString()
    if (output != "")
      output = output.stripSuffix("\n").stripMargin('#')
    else
      output += "No Results!"
    output
  }

  //For actual TimeStamp:
  def readBoundSensor(path: String, lbdate: LocalTime, ubdate: LocalTime, nodeName: String): String = {

    var bufferedSource: BufferedSource = null
    try {
      bufferedSource = Source.fromFile(s"$sacepicnEnv/sensors/" + path)
    }
    catch
    {
      case fileNotFoundException: FileNotFoundException => handleFileNotFoundException(fileNotFoundException, nodeName)
      case _: Throwable => LogMessage(nodeName, "Got some Kind of Exception")
    }
    var output = ""
    val sb = new StringBuilder
    val lineList = bufferedSource.getLines().toList
    val delimiter: String = SensorHelpers.getDelimiterFromLine(lineList.head)
    val datePosition = SensorHelpers.getDatePosition(delimiter)
    //val valuePosition = getValuePosition(delimiter)

    for (line <- lineList) {
      //value part is never used
      //val valuePart = line.split(delimiter)(valuePosition).stripPrefix("(").stripSuffix(")").trim.toInt

      //For TS
      /*
    Added by Johannes
    */
      val timeStamp = SensorHelpers.parseTime(line.split(delimiter)(datePosition), delimiter)
      /*
    End Edit
     */
      if ((lbdate.isBefore(timeStamp) || lbdate.equals(timeStamp)) && (ubdate.isAfter(timeStamp) || ubdate.equals(timeStamp))) {
        //output = output + valuePart.toString + ","
        sb.append(line.toString() + "\n")
      }
    }

    bufferedSource.close
    output = sb.toString()
    if (output != "")
      output = output.stripSuffix("\n").stripMargin('#')
    else
      output += "No Results!"
    output
  }

  //Return number of specified events from the top of the event stream
  def readEventCountSensor(outputFormat: String, path: String, numberOfEvents: Int, nodeName: String): String = {
    val bufferedSource = Source.fromFile(s"$sacepicnEnv/sensors/" + path)

    val sb = new StringBuilder

    bufferedSource.getLines().zipWithIndex.foreach {
      case (line, index) => {
        if (index < numberOfEvents) //Index is the zero'th index, so we only do <, not <=. This will get the top x events from the sensor where x is the number passed to the op.
        sb.append(line + "\n")
      }
    }

    bufferedSource.close

    var output = sb.toString()
    if (output != "")
      output = output.stripSuffix("\n").stripMargin('#')
    else
      output += "No Results!"

    output
  }

  //Time Unit values: 'S', 'M', 'H'
  def readTimedSensor(path: String, timePeriod: Long, timeUnit: String, nodeName: String): String = {
    var bufferedSource: BufferedSource = null
    try {
      bufferedSource = Source.fromFile(s"$sacepicnEnv/sensors/" + path)
    }
    catch {
      case fileNotFoundException: FileNotFoundException => handleFileNotFoundException(fileNotFoundException, nodeName)
      case _: Throwable => LogMessage(nodeName, "Got some Kind of Exception")
    }
    var sb = new StringBuilder
    var output: String = ""
    val lineList = bufferedSource.getLines().toList
    val delimiter: String = SensorHelpers.getDelimiterFromLine(lineList.head)
    val datePosition = SensorHelpers.getDatePosition(delimiter)
    //Get current time:
    var currentTime: LocalTime = LocalTime.now()
    var lbDate = currentTime.format(DateTimeFormat)
    //Time Unit values: 'S', 'M', 'H'

    val pastTime = FormattedOutput.getPastTime(currentTime, timePeriod, timeUnit)
    //LogMessage(nodeName, s"Past Time: ${pastTime.toString}")
    //LogMessage(nodeName, s"Current Time: ${currentTime.toString}")
    for (line <- lineList) {
      //process each event line
      val timeStamp = SensorHelpers.parseTime(line.split(delimiter)(datePosition), delimiter)
      if ((pastTime.isBefore(timeStamp) || pastTime.equals(timeStamp)) && (currentTime.isAfter(timeStamp) || currentTime.equals(timeStamp))) {
        sb.append(line.toString() + "\n")
      }

    }

    bufferedSource.close
    output = sb.toString()
    if (output != "")
      output = output.stripSuffix("\n").stripMargin('#')
    else
      output += "No Results!"
    output
  }

  def handleFileNotFoundException(fileNotFoundException: FileNotFoundException, nodeName: String): Unit = {
    LogMessage(nodeName, fileNotFoundException.toString)
  }

}

