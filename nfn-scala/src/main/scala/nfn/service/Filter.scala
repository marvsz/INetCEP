package nfn.service

/**
 * Created by Ali on 06.02.18.
 */

import SACEPICN.StatesSingleton
import akka.actor.ActorRef
import nfn.tools.{FilterHelpers, Helpers, Networking, SensorHelpers}

import scala.collection.mutable.ArrayBuffer

//Added for contentfetch
import ccn.packet._
import config.StaticConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class Filter() extends NFNService {
  val sacepicnEnv = StaticConfig.systemPath

  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future{
    var nodeInfo = interestName.cmps.mkString(" ")
    var nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)

    //stream: Victims/Survivors
    //filter: 1>20||2=F&&3<40 etc. 1 denotes the first column, 2 denotes the second column.
    //- Here the query consumer must know what they are looking for. Else we will return a generic wrong schema message.
    //- Filter is done on OR (||) and AND (&&).
    //Victim Data: 22:18:38.841/1001/M/50 <- Schema: 1/2/3/4
    //Sample Query: 'Victims' '2=1001&&3=M||4>46'
    //First break OR and then get AND

    def placeFilterInterests(): String = {
      /*LogMessage(nodeName, s"Placing Entries in PIT and PQT accordingly: ${interestedComputation} on ${nodeName} is interested in ${stream} from ${interestNodeName}")
      Networking.subscribeToQuery(stream,interestedComputation.toString,ccnApi)*/
      "Initial Filter Operation started --> can I get rid of this in general? Maybe have a Non-returning thing here..."
    }

    def filterStream1(dataStreamName: String, filter: String, dataStream: String):String = {
      LogMessage(nodeName, s"\nFilter OP Started")
      val filterParams = FilterHelpers.parseFilterArguments(filter)
      LogMessage(nodeName,s"Filter($dataStreamName,$filter)")
      //filterHandler(dataStreamName,  filterParams(0), filterParams(1),"data",nodeName,dataStream)
      s"Filter($dataStreamName,$filter)" + "\n" + dataStream
      //"filtered Stream"
    }

    def filterInitialStream(source: String, stream: String, filter: NFNStringValue, outputFormat: NFNStringValue, interestedComputationName: CCNName): String = {
      LogMessage(nodeName, s"Initial Filteroperation startedd")
      val setting = s"/state/Filter/".concat(filter.str).concat(stream)
      Networking.makePersistentInterest(stream.substring(1),interestedComputationName,ccnApi) // remove the first backslash from stream
      setting
    }

    def filterStream(source: String, stream: String, filter: NFNStringValue, outputFormat: NFNStringValue, dataStream: NFNStringValue): String = {

      //Interest will always be in the form of: call X /node/nodeX/nfn_service_X
      //Using this we can extract the node for this operation.
        LogMessage(nodeName, s"\nFilter OP Started")
        val filterParams = FilterHelpers.parseFilterArguments(filter.str)
        //By this time, we will have two stacks of filters - OR and AND. We will pass this to the handler.
        //return "=" + allANDs.mkString(",") + " - " + allORs.mkString(",");
        filterHandler(source, filterParams(0), filterParams(1), outputFormat.str, nodeName, dataStream.str)
    }

    def filterHandler(sensorName: String, aNDS: ArrayBuffer[String], oRS: ArrayBuffer[String], outputFormat: String, nodeName: String, datastream: String): String = {
      var output = ""

      LogMessage(nodeName, "Handle Filter Stream")
      //At this point, we will definitely have the intermediate window result.
      LogMessage(nodeName, s"Inside Filter -> Stream to filter: $datastream")
      val delimiter = SensorHelpers.getDelimiterFromLine(datastream)
      LogMessage(nodeName, s"Delimiter is $delimiter")
      output = filter(sensorName, datastream, aNDS, oRS, delimiter)


      //If outputFormat = name => we will return a named interest
      //If outputFormat = data => we will return the data
      LogMessage(nodeName, s"Inside Filter -> FILTER name: NONE, FILTER content: ${output}")

      LogMessage(nodeName, s"Filter OP Completed")
      output
    }

    NFNStringValue(
      args match {
        //Output format: Either name (/node/Filter/Sensor/Time) or data (data value directly)
        //[data/sensor][string of data][filter][outputFormat]
        case Seq(queryInterest: NFNStringValue, filter:NFNStringValue) => placeFilterInterests()
        case Seq(queryInterest: NFNStringValue, filter:NFNStringValue, dataStream: NFNStringValue) => filterStream1(queryInterest.str, filter.str, dataStream.str)
        //case Seq(timestamp: NFNStringValue, source: NFNStringValue, stream: NFNStringValue, filter: NFNStringValue, outputFormat: NFNStringValue) => filterInitialStream(source.str, stream.str, filter, outputFormat, interestName)
        //case Seq(source: NFNStringValue, stream: NFNStringValue, filter: NFNStringValue, dataStream: NFNStringValue) => filterStream(source.str, stream.str, filter, dataStream)
        //[content][contentobject][filter][outputFormat]
        //case Seq(timestamp: NFNStringValue, source: NFNStringValue, stream: NFNContentObjectValue, filter: NFNStringValue, outputFormat: NFNStringValue) => filterStream(source.str, new String(stream.data), filter, outputFormat)
        //[sensor][string of data][filter]
        //case Seq(timestamp: NFNStringValue, stream: NFNStringValue, filter: NFNStringValue, outputFormat: NFNStringValue) => filterStream("sensor", stream.str, filter, outputFormat)
        case _ =>
          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
      }
    )
  }

  def filter(sensorName: String, sourceValue: String, aNDS: ArrayBuffer[String], oRS: ArrayBuffer[String], delimiter: String): String = {
    var data: List[String] = null
    var output = ""
    data =
      sourceValue.split("\n")
        .toSeq
        .map(_.trim)
        .filter(_ != "").toList

    if (data.nonEmpty) {
      data.foreach(line => {
        var lineAdded = false
        var andConditionisValid = true
        aNDS.foreach(
          and =>
            if (andConditionisValid && !FilterHelpers.conditionHandler(sensorName, and, line, delimiter)) {
              andConditionisValid = false
            })

        if (andConditionisValid && !lineAdded) {
          output += "#" + line.toString + "\n"
          lineAdded = true
        }

        if (!lineAdded) {
          oRS.foreach(
            or => {
              if (!lineAdded && FilterHelpers.conditionHandler(sensorName, or, line, delimiter)) {
                output += "#" + line.toString + "\n"
                lineAdded = true
              }
            }
          )
        }
      })
    }
    if (output != "")
      output = output.stripSuffix("\n").stripMargin('#')
    else
      output += "No Results!"
    output
  }
}

