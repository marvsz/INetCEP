package nfn.service

/**
 * Created by Johannes on 31.8.2019
 */

import java.util
import java.util.{LinkedHashSet, Set}

import INetCEP.StatesSingleton
import akka.actor.ActorRef
import nfn.tools.{FilterHelpers, Helpers, Networking, SensorHelpers}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
//Added for contentfetch
import ccn.packet.CCNName

import scala.language.postfixOps
import scala.concurrent.duration._
//Added for CCN Command Execution:
import config.StaticConfig

class Join() extends NFNService {
  val sacepicnEnv = StaticConfig.systemPath

  override def function(interestName: CCNName, args: Seq[NFNValue], stateHolder: StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future {
    val nodeInfo = interestName.cmps.mkString(" ")
    val nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)

    def processInitialJoinOn(): String = {
      LogMessage(nodeName, s"Initial Join")
      "Initial Join"
    }

    def processUCLJoinOn(left: String, right: String, joinOn: String, conditions: String, joinType: String, dataStream: String): String = {
      LogMessage(nodeName, s"\n JoinOn OP Started")
      LogMessage(nodeName, s"Data stream is:\n $dataStream")
      LogMessage(nodeName, s"Left is: $left")
      LogMessage(nodeName, s"Right is: $right")
      var output = ""
      val side = determineLeftOrRight(left, right, dataStream, nodeName)
      if (side == "left")
        stateHolder.updateWindowState(left, dataStream)
      else if (side == "right")
        stateHolder.updateWindowState(right, dataStream)
      else {
        LogMessage(nodeName, s"something went wring")
      }
      var leftSide = stateHolder.getWindowState(left)
      var rightSide = stateHolder.getWindowState(right)
      var leftSchema = ""
      var rightSchema = ""
      if (leftSide == null) {
        LogMessage(nodeName, "JOIN: Left was empty")
        leftSide = ""
      }
      else {
        LogMessage(nodeName, s"JOIN: Left was $leftSide")
        leftSchema = leftSide.split("\n").head.split("-")(1).trim
        LogMessage(nodeName, s"JOIN: Left schema is $leftSchema")
      }
      if (rightSide == null) {
        LogMessage(nodeName, "JOIN: Right was empty")
        rightSide = ""
      }
      else {
        LogMessage(nodeName, s"JOIN: Right was $rightSide")
        rightSchema = rightSide.split("\n").head.split("-")(1).trim
        LogMessage(nodeName, s"JOIN: Right schema is $rightSchema")
      }
      if (!(rightSide != null && leftSide != null))
        output = "Empty Join" //
      else
        output = joinStreamsOn(leftSide.split("\n").drop(1).mkString("\n"), leftSchema, left, rightSide.split("\n").drop(1).mkString("\n"), rightSchema, right, joinOn, conditions, joinType, nodeName)
      LogMessage(nodeName, s"Output is: $output")
      LogMessage(nodeName, s"Join OP Completed\n")
      output
    }

    def processPRAJoinOn(left: String, right: String, joinOn: String, conditions: String, joinType: String):String = {
      LogMessage(nodeName, s"\n JoinOn OP Started")
      LogMessage(nodeName, s"Left is: $left")
      LogMessage(nodeName, s"Right is: $right")
      val joinQuery = s"JOIN($left,$right,$joinOn,$conditions,$joinType)"
      var leftSide = ""
      var rightSide = ""
      val leftIntermediateResult = Networking.fetchContentFromNetwork(left, ccnApi, 500 milliseconds)
      if(leftIntermediateResult.isDefined){
        leftSide = new String(leftIntermediateResult.get.data)
      }
      else{
        leftSide = "Fail"
        LogMessage(nodeName,"PREDICTION: Finished prediction")
        val newDataStreamName = s"node/$nodeName/$joinQuery"
        Networking.storeResult(nodeName,newDataStreamName,newDataStreamName,ccnApi)
        return newDataStreamName
      }
      val rightIntermediateResult = Networking.fetchContentFromNetwork(right, ccnApi, 500 milliseconds)
      if(rightIntermediateResult.isDefined){
        leftSide = new String(rightIntermediateResult.get.data)
      }
      else{
        rightSide = "Fail"
        LogMessage(nodeName,"PREDICTION: Finished prediction")
        val newDataStreamName = s"node/$nodeName/$joinQuery/${System.currentTimeMillis()}"
        Networking.storeResult(nodeName,newDataStreamName,newDataStreamName,ccnApi)
        return newDataStreamName
      }
      var leftSchema = ""
      var rightSchema = ""
      LogMessage(nodeName, s"JOIN: Left was $leftSide")
      leftSchema = leftSide.split("\n").head.split("-")(1).trim
      LogMessage(nodeName, s"JOIN: Left schema is $leftSchema")
      LogMessage(nodeName, s"JOIN: Right was $rightSide")
      rightSchema = rightSide.split("\n").head.split("-")(1).trim
      LogMessage(nodeName, s"JOIN: Right schema is $rightSchema")
      val output = joinStreamsOn(leftSide.split("\n").drop(1).mkString("\n"), leftSchema, left, rightSide.split("\n").drop(1).mkString("\n"), rightSchema, right, joinOn, conditions, joinType, nodeName)
      val newDataStreamName = s"node/$nodeName/$joinQuery/${System.currentTimeMillis()}"
      Networking.storeResult(nodeName,output,newDataStreamName,ccnApi)
      newDataStreamName
    }

    NFNStringValue(
      args match {
        //case Seq(timestamp: NFNStringValue, inputSource: NFNStringValue, outputFormat: NFNStringValue, left: NFNStringValue, right: NFNStringValue) => processJoin(inputSource.str, left.str, right.str, outputFormat.str)
        // Hier unterscheidung für communicationappraoch
        case Seq(communicationApproach: NFNStringValue, left: NFNStringValue, right: NFNStringValue, joinOn: NFNStringValue, conditions: NFNStringValue, joinType: NFNStringValue) =>
          processInitialJoinOn()

        case Seq(communicationApproach: NFNStringValue, left: NFNStringValue, right: NFNStringValue, joinOn: NFNStringValue, conditions: NFNStringValue, joinType: NFNStringValue, dataStream: NFNStringValue) =>
          communicationApproach.str.toLowerCase() match{
            case "ucl" => processUCLJoinOn(left.str, right.str, joinOn.str, conditions.str, joinType.str, dataStream.str)
            case _ => processPRAJoinOn(right.str, joinOn.str, conditions.str, joinType.str, dataStream.str)
          }

        case _ =>
          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
      }
    )
  }

  def determineLeftOrRight(left: String, right: String, dataStream: String, nodeName: String): String = {
    var etVal = "none"
    val dataStreamToCheck = dataStream.split("\n")(0).toLowerCase().split("-")(0).trim
    LogMessage(nodeName, s"To Check is = ${dataStreamToCheck}")
    if (dataStreamToCheck == left.toLowerCase())
      etVal = "left"
    else if (dataStreamToCheck == right.toLowerCase())
      etVal = "right"
    etVal
  }

  /**
   * Joins the content of two windows by matching each event of one window to all events in the other window on the given condition.
   * The output are all events generated for all the matching event pairs.
   *
   * @param left        the left stream
   * @param leftSchema  the name of the left stream
   * @param right       the right stream
   * @param rightSchema the name of the right stream
   * @param joinOn      the column on which the streams are matched
   * @param conditions  a list of conditions that is applied
   * @param joinType    the type of the join. [innerJoin,leftOuterJoin,rightOuterJoin,fullOuterJoin]
   * @return the output generated by the specific join type.
   */
  def joinStreamsOn(left: String, leftSchema: String, leftStreamName: String, right: String, rightSchema: String, rightStreamName: String, joinOn: String, conditions: String, joinType: String, nodeName: String) = {
    val leftTrimmed = SensorHelpers.trimData(left)
    val rightTrimmed = SensorHelpers.trimData(right)
    val leftDelimiter = SensorHelpers.getDelimiterFromLine(leftSchema)
    val rightDelimiter = SensorHelpers.getDelimiterFromLine(rightSchema)
    val joinOnPosLeft = SensorHelpers.getColumnIDs(leftSchema, joinOn, leftDelimiter).head
    val joinOnPosRight = SensorHelpers.getColumnIDs(rightSchema, joinOn, rightDelimiter).head
    var leftTrimmedSplit: Array[String] = null
    var rightTrimmedSplit: Array[String] = null
    if (conditions != "" && conditions != "none") {
      leftTrimmedSplit = FilterHelpers.applyConditions(leftSchema, leftTrimmed.split("\n"), conditions, leftDelimiter)
      rightTrimmedSplit = FilterHelpers.applyConditions(rightSchema, rightTrimmed.split("\n"), conditions, rightDelimiter)
    }
    else {
      leftTrimmedSplit = leftTrimmed.split("\n")
      rightTrimmedSplit = rightTrimmed.split("\n")
    }
    //if the delimiters are different we need to choose one, in this case the delimiter from the left data stream
    if (leftDelimiter != rightDelimiter) {
      rightTrimmed.replace(rightDelimiter, leftDelimiter)
    }
    val sb = new StringBuilder
    val joinQuery = s"JOIN($leftStreamName,$rightStreamName,$joinOn,$conditions,$joinType)"
    val joinedSchema: String = joinSchema(joinOn, leftSchema, rightSchema, leftDelimiter)
    LogMessage(nodeName, s"First Line is ${joinQuery + " - " + joinedSchema}")
    sb.append(joinQuery + "-" + joinedSchema)
    joinType.toLowerCase() match {
      case "innerjoin" => innerjoin(leftTrimmedSplit, joinOnPosLeft, rightTrimmedSplit, joinOnPosRight, sb).stripSuffix("\n")
      case "leftouterjoin" => leftOuterJoinOn(leftTrimmedSplit, joinOnPosLeft, rightTrimmedSplit, joinOnPosRight, sb).stripSuffix("\n")
      case "rightouterjoin" => rightOuterJoinOn(leftTrimmedSplit, joinOnPosLeft, rightTrimmedSplit, joinOnPosRight, sb).stripSuffix("\n")
      case "fullouterjoin" => fullOuterJoinOn(leftTrimmedSplit, joinOnPosLeft, rightTrimmedSplit, joinOnPosRight, sb).stripSuffix("\n")
      case _ => "Join Type not Supported"
    }
  }

  /**
   * Takes two sensor schemas and returns them joined on the specified column
   *
   * @param joinOn      The column Name to join on
   * @param leftSchema  the left sensor schema
   * @param rightSchema the right sensor schema
   * @param delimiter   the delimiter of the sensor tuples
   * @return the joined schema
   */
  def joinSchema(joinOn: String, leftSchema: String, rightSchema: String, delimiter: String): String = {
    val columnNames1: Array[String] = leftSchema.split(delimiter)
    val columnNames2: Array[String] = rightSchema.split(delimiter)
    var returnVal = ""
    if (columnNames1 != null && columnNames2 != null)
      if ((columnNames1.contains(joinOn.toLowerCase) && columnNames2.contains(joinOn.toLowerCase))) {
        for (cName <- columnNames1) {
          returnVal = returnVal + cName + delimiter
        }
        for (cName <- columnNames2) {
          if (!(cName == joinOn.toLowerCase)) returnVal = returnVal + cName + delimiter
        }
      }
    returnVal.dropRight(1)
  }

  /**
   * The output is generated only if there is a matching event in both the streams
   *
   * @param left           the left data stream
   * @param joinOnPosLeft  the tuple position in the left data stream on which to join on
   * @param right          the right data stream
   * @param joinOnPosRight the tuple position in the right data stream on which to join on
   * @return the generated output
   */
  def innerjoin(left: Array[String], joinOnPosLeft: Int, right: Array[String], joinOnPosRight: Int, sb: StringBuilder) = {
    val delimiter = SensorHelpers.getDelimiterFromLine(left(0))
    for (leftLine <- left) {
      for (rightLine <- right) {
        if (leftLine.split(delimiter)(joinOnPosLeft).equals(rightLine.split(delimiter)(joinOnPosRight))) {
          sb.append(leftLine).append(delimiter).append(deleteJoinedOn(rightLine, joinOnPosRight, delimiter)).append("\n")
        }
      }
    }
    sb.toString()
  }

  /**
   * It returns all the events on the left stream even if there are no matching events on the right stream by having null values at the right stream
   *
   * @param left          the left data stream
   * @param joinOnPosLeft the tuple position in the left data stream on which to join on
   * @param right         the right data stream
   * @param joinOnPosRight
   * @return the generated output
   */
  def leftOuterJoinOn(left: Array[String], joinOnPosLeft: Int, right: Array[String], joinOnPosRight: Int, sb: StringBuilder) = {
    var joinFound = false
    var rightColumn = 0
    val delimiter = SensorHelpers.getDelimiterFromLine(left(0))
    for (leftLine <- left) {
      for (rightLine <- right) {
        rightColumn = rightLine.split(delimiter).size - 1
        if (leftLine.split(delimiter)(joinOnPosLeft).equals(rightLine.split(delimiter)(joinOnPosRight))) {
          sb.append(leftLine).append(delimiter).append(deleteJoinedOn(rightLine, joinOnPosRight, delimiter)).append("\n")
          joinFound = true
        }
      }
      if (!joinFound) {
        sb.append(leftLine).append(delimiter).append(generateNullLines(rightColumn, delimiter)).append("\n")
      }
      joinFound = false
    }
    sb.toString()
  }

  /**
   * It returns all the events in the right stream even if there are no matching events on the left stream by having null values at the left stream
   *
   * @param left          the left data stream
   * @param joinOnPosLeft the tuple position in the left data stream on which to join on
   * @param right         the right data stream
   * @param joinOnPosRight
   * @return the generated output
   */
  def rightOuterJoinOn(left: Array[String], joinOnPosLeft: Int, right: Array[String], joinOnPosRight: Int, sb: StringBuilder) = {
    var joinFound = false
    var leftColumn = 0
    val delimiter = SensorHelpers.getDelimiterFromLine(left(0))
    for (rightLine <- right) {
      for (leftLine <- left) {
        leftColumn = leftLine.split(delimiter).size - 1
        if (rightLine.split(delimiter)(joinOnPosLeft).equals(leftLine.split(delimiter)(joinOnPosRight))) {
          sb.append(leftLine).append(delimiter).append(deleteJoinedOn(rightLine, joinOnPosRight, delimiter)).append("\n")
          joinFound = true
        }
      }
      if (!joinFound) {
        sb.append(generateNullLines(leftColumn, delimiter)).append(delimiter).append(rightLine).append("\n")
      }
      joinFound = false
    }
    sb.toString()
  }

  /**
   * removes a column from a given string
   *
   * @param line           one event
   * @param joinOnPosRight the position in the event tuple to delete
   * @param delimiter      the delimiter which separates the columns
   * @return the event without the column to delete
   */
  def deleteJoinedOn(line: String, joinOnPosRight: Int, delimiter: String) = {
    val newLine = line.split(delimiter).toSeq.patch(joinOnPosRight, Nil, 1)
    newLine.mkString(delimiter)
  }

  /**
   * Generates a row with a null values
   * This is used for outer Joins where there are mismatches
   *
   * @param columns   : the number of required null values
   * @param delimiter the delimiter which separates the columns
   * @return the row filled with x -1 null values where x is the number of columns
   */
  def generateNullLines(columns: Int, delimiter: String) = {
    Array.fill[String](columns)("Null").mkString(delimiter)
  }

  /**
   * Combines the result of the left outer join and the right outer join. Output is generated for each incoming event, even if there are no matching events in the other stream
   *
   * @param left           the left data stream
   * @param joinOnPosLeft  the tuple position in the left data stream on which to join on
   * @param right          the right data stream
   * @param joinOnPosRight the tuple position in the right data stream on which to join on
   * @return the generated output
   */
  def fullOuterJoinOn(left: Array[String], joinOnPosLeft: Int, right: Array[String], joinOnPosRight: Int, sb: StringBuilder) = {
    var joinFound = false
    var columnCount = 0
    val delimiter = SensorHelpers.getDelimiterFromLine(left(0))
    for (leftLine <- left) {
      for (rightLine <- right) {
        columnCount = rightLine.split(delimiter).size - 1
        if (leftLine.split(delimiter)(joinOnPosLeft).equals(rightLine.split(delimiter)(joinOnPosRight))) {
          sb.append(leftLine).append(delimiter).append(deleteJoinedOn(rightLine, joinOnPosRight, delimiter)).append("\n")
          joinFound = true
        }
      }
      if (!joinFound) {
        sb.append(leftLine).append(delimiter).append(generateNullLines(columnCount, delimiter)).append("\n")
      }
      joinFound = false
    }

    for (rightLine <- right) {
      for (leftLine <- left) {
        columnCount = leftLine.split(delimiter).size - 1
        if (rightLine.split(delimiter)(joinOnPosLeft).equals(leftLine.split(delimiter)(joinOnPosRight))) {
          val maybeElement = leftLine.concat(delimiter).concat(deleteJoinedOn(rightLine, joinOnPosRight, delimiter)).concat("\n")
          if (!sb.toString().contains(maybeElement))
            sb.append(maybeElement)
          joinFound = true
        }
      }
      if (!joinFound) {
        sb.append(generateNullLines(columnCount, delimiter)).append(delimiter).append(rightLine).append("\n")
      }
      joinFound = false
    }
    sb.toString()
  }

}
