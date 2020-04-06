package nfn.service

/**
  * Created by Johannes on 14.01.2019
  */

//Added for contentfetch
import SACEPICN.StatesSingleton
import akka.actor.ActorRef
import nfn.tools.SensorHelpers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

//Added for contentfetch
import ccn.packet.CCNName

//Added for CCN Command Execution:
import config.StaticConfig
import nfn.tools.Helpers

class Heatmap extends NFNService {
  val sacepicnEnv = StaticConfig.systemPath

  override def function(interestName: CCNName, args: Seq[NFNValue], stateHolder:StatesSingleton,ccnApi: ActorRef): Future[NFNValue] = Future{
    val nodeInfo = interestName.cmps.mkString(" ")
    val nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)

    def initialHeatMapHandler(granularity: String, lowerBound: String, upperBound: String, leftBound: String, rightBound: String, streamName: String): String = {
      LogMessage(nodeName,"HEATMAP: Started initial heatmap")
      "HEATMAP: Started initial prediction"
    }

    def heatMapHandler(granularity: String, lowerBound: String, upperBound: String, leftBound: String, rightBound: String, streamName: String, dataStream: String): String = {
      LogMessage(nodeName,"HEATMAP OP started")
      var heatmap:Array[Array[Int]] = null
      //Sensor is not the preferred way to perform a heatmap. Suggested is 'name'
      heatmap = generateHeatmap(SensorHelpers.parseData("data",dataStream), granularity.toDouble, lowerBound.toDouble, upperBound.toDouble, leftBound.toDouble, rightBound.toDouble)
      var output = ""
      if (heatmap != null) {
        output = generateIntermediateHeatmap(heatmap)
      }
      if (output != "")
        output = output.stripSuffix("\n").stripMargin('#')
      else
        output += "No Results!"
      LogMessage(nodeName, s"Heatmap OP Completed")
      LogMessage(nodeName, s"Output is $output")
      output
    }

    NFNStringValue(
      args match {
        case Seq(granularity: NFNStringValue, lowerBound: NFNStringValue, upperBound: NFNStringValue, leftBound: NFNStringValue, rightBound: NFNStringValue, streamName: NFNStringValue) =>
          initialHeatMapHandler(granularity.str, lowerBound.str, upperBound.str, leftBound.str, rightBound.str, streamName.str)
        case Seq(granularity: NFNStringValue, lowerBound: NFNStringValue, upperBound: NFNStringValue, leftBound: NFNStringValue, rightBound: NFNStringValue, streamName: NFNStringValue, dataStream: NFNStringValue) =>
          heatMapHandler(granularity.str, lowerBound.str, upperBound.str, leftBound.str, rightBound.str, streamName.str, dataStream.str)
        case _ =>
          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
      }
    )
  }

  /**
   *
    * @param data the raw data to process
   * @param granularity the lenght and high of one cell in the resulting heatmap.
   * @param minimalLongitude the minimal Longitude value
   * @param maximalLongitude the maximal Longitude value
   * @param minmalLattitude the minimal latitude value
   *
   * @param maximalLattitude the maximale latitude value
   * @return the generated heatmap as a two dimensional array
   */
  def generateHeatmap(data: List[String], granularity: Double, minimalLongitude: Double, maximalLongitude:Double, minmalLattitude: Double, maximalLattitude:Double): Array[Array[Int]] = {
    var output = ""
    val streamName = data.head.split("-")(0).trim
    var streamData = data.drop(1)
    val schema = data.head.split("-")(1).trim
    val horizontalSize = maximalLattitude.toDouble - minmalLattitude.toDouble
    val verticalSize = maximalLongitude.toDouble - minimalLongitude.toDouble
    val numberOfWidths = Math.ceil(horizontalSize / granularity.toDouble)
    val numberOfHeights = Math.ceil(verticalSize / granularity.toDouble)

    val heatmap = Array.ofDim[Int](numberOfWidths.toInt, numberOfHeights.toInt)
    var lat = 0.0
    var long = 0.0
    val delimiter = SensorHelpers.getDelimiterFromLine(schema)
    val longitudePositions = SensorHelpers.getColumnIDs(schema,"longitude",delimiter)
    val latitudePositions = SensorHelpers.getColumnIDs(schema,"latitude",delimiter)
    var correspondingRow = 0
    var correspondingCol = 0
    if (streamData.nonEmpty) {
      for (i <- 0 to longitudePositions.length-1){
        for (line <- streamData) {
          if (line != "No Results!") {
            lat = line.split(delimiter)(latitudePositions(i)).toDouble
            long = line.split(delimiter)(longitudePositions(i)).toDouble
            correspondingCol = Math.ceil((long - minimalLongitude) / granularity).toInt - 1
            correspondingRow = Math.ceil((lat - minmalLattitude) / granularity).toInt - 1
            if (!(correspondingRow < 0 || correspondingRow > heatmap.length))
              if (!(correspondingCol < 0 || correspondingCol > heatmap(correspondingRow).length))
                heatmap(correspondingRow)(correspondingCol) = heatmap(correspondingRow)(correspondingCol) + 1
          }
        }
      }
    }
    heatmap
  }


  def generateIntermediateHeatmap(heatmap:Array[Array[Int]]) ={

    val sb = new StringBuilder
    val width = heatmap(0).length
    val height = heatmap.length
    for(j <- 0 until height -1){
      for (i <- 0 until width -1){
        if(heatmap(j)(i)!=0){
          sb.append(s"$j|$i:${heatmap(j)(i)};")
        }
      }
    }
    sb.toString().stripSuffix(";")
  }
  /**
    *
    * @param heatmap A two-dimensional Array of Integer Values
    * @return A ASCII representation of the input
    */
  def heatmapPrinter(heatmap: Array[Array[Int]]): String = {
    var output = "Heatmap Start\n"
    val width = heatmap(0).length
    val height = heatmap.length
    var i = 0
    var upperAndLowerLines = "+---+"
    for (i <- 1 until width - 1) {
      upperAndLowerLines = upperAndLowerLines + "---+"
    }
    output = output + upperAndLowerLines + "\n"
    var j = 0
    var k = 0
    for (j <- 0 until height - 1) {
      output = output + "|"
      for (k <- 0 until width - 1) {
        val n = heatmap(j)(k)
        if (n == 0) {
          output = output + " 0" + " |"
        }
        else if ((Math.log10(n) + 1).toInt == 1) {
          output = output + " " + heatmap(j)(k).toString + " |"
        }
        else if ((Math.log10(n) + 1).toInt == 2) {
          output = output + " " + heatmap(j)(k).toString + "|"
        }
        else if ((Math.log10(n) + 1).toInt == 3) {
          output = output + "" + heatmap(j)(k).toString + "|"
        }
      }
      output = output + "\n" + upperAndLowerLines + "\n"
    }
    output = output + "Heatmap End\n"
    output
  }
}
