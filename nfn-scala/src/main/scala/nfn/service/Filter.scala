package nfn.service

/**
 * Created by Ali on 06.02.18.
 */

import akka.actor.ActorRef
import nfn.tools.{FilterHelpers, Helpers, SensorHelpers}

import scala.collection.mutable.ArrayBuffer

//Added for contentfetch
import ccn.packet._
import config.StaticConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class Filter() extends NFNService {
  val sacepicnEnv = StaticConfig.systemPath

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = Future{
    var nodeInfo = interestName.cmps.mkString(" ")
    var nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)

    //stream: Victims/Survivors
    //filter: 1>20||2=F&&3<40 etc. 1 denotes the first column, 2 denotes the second column.
    //- Here the query consumer must know what they are looking for. Else we will return a generic wrong schema message.
    //- Filter is done on OR (||) and AND (&&).
    //Victim Data: 22:18:38.841/1001/M/50 <- Schema: 1/2/3/4
    //Sample Query: 'Victims' '2=1001&&3=M||4>46'
    //First break OR and then get AND
    def filterStream(source: String, stream: String, filter: NFNStringValue, outputFormat: NFNStringValue): String = {

      //Interest will always be in the form of: call X /node/nodeX/nfn_service_X
      //Using this we can extract the node for this operation.

      LogMessage(nodeName, s"\nFilter OP Started")
      val filterParams = FilterHelpers.parseFilterArguments(filter.str)

      //By this time, we will have two stacks of filters - OR and AND. We will pass this to the handler.
      //return "=" + allANDs.mkString(",") + " - " + allORs.mkString(",");
      filterHandler(source, stream, filterParams(0), filterParams(1), outputFormat.str, nodeName)
    }

    def filterHandler(sensorName: String, stream: String, aNDS: ArrayBuffer[String], oRS: ArrayBuffer[String], outputFormat: String, nodeName: String): String = {
      var output = ""

      LogMessage(nodeName, "Handle Filter Stream")
      val intermediateResult = Helpers.handleNamedInputSource(nodeName, stream, ccnApi)
      //At this point, we will definitely have the intermediate window result.
      LogMessage(nodeName, s"Inside Filter -> Child Operator Result: $intermediateResult")
      val delimiter = SensorHelpers.getDelimiterFromLine(intermediateResult)
      LogMessage(nodeName, s"Delimiter is $delimiter")
      output = filter(sensorName, intermediateResult, aNDS, oRS, delimiter)


      //If outputFormat = name => we will return a named interest
      //If outputFormat = data => we will return the data
      if (outputFormat.toLowerCase == "name") {
        output = Helpers.storeOutput(nodeName, output, "FILTER", "onWindow", ccnApi)
      }
      else {
        LogMessage(nodeName, s"Inside Filter -> FILTER name: NONE, FILTER content: ${output}")
      }
      LogMessage(nodeName, s"Filter OP Completed\n")
      output
    }

    NFNStringValue(
      args match {
        //Output format: Either name (/node/Filter/Sensor/Time) or data (data value directly)
        //[data/sensor][string of data][filter][outputFormat]
        case Seq(timestamp: NFNStringValue, source: NFNStringValue, stream: NFNStringValue, filter: NFNStringValue, outputFormat: NFNStringValue) => filterStream(source.str, stream.str, filter, outputFormat)
        //[content][contentobject][filter][outputFormat]
        case Seq(timestamp: NFNStringValue, source: NFNStringValue, stream: NFNContentObjectValue, filter: NFNStringValue, outputFormat: NFNStringValue) => filterStream(source.str, new String(stream.data), filter, outputFormat)
        //[sensor][string of data][filter]
        case Seq(timestamp: NFNStringValue, stream: NFNStringValue, filter: NFNStringValue, outputFormat: NFNStringValue) => filterStream("sensor", stream.str, filter, outputFormat)
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

