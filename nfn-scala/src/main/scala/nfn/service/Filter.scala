package nfn.service

/**
 * Created by Ali on 06.02.18.
 */

import INetCEP.StatesSingleton
import akka.actor.ActorRef
import nfn.tools.{FilterHelpers, Helpers, Networking, SensorHelpers}

import scala.collection.mutable.ArrayBuffer

//Added for contentfetch
import ccn.packet._
import config.StaticConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
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

    def initialUCLFilter(): String = {
      /*LogMessage(nodeName, s"Placing Entries in PIT and PQT accordingly: ${interestedComputation} on ${nodeName} is interested in ${stream} from ${interestNodeName}")
      Networking.subscribeToQuery(stream,interestedComputation.toString,ccnApi)*/
      "Initial Filter Operation started --> can I get rid of this in general? Maybe have a Non-returning thing here..."
    }

    def filterUCLStream(dataStreamName: String, filter: String, dataStream: String):String = {
      LogMessage(nodeName, s"\nFilter OP Started")
      val filterParams = FilterHelpers.parseFilterArguments(filter)
      //LogMessage(nodeName,s"Filter($dataStreamName,$filter)")
      val streamHeader = dataStream.split("\n")(0)
      val streamSchema = streamHeader.toString().split("-")(1)
      val newHeader = s"Filter($dataStreamName,$filter)" + " - " + streamSchema + "\n"
      LogMessage(nodeName,s"Schema is $streamSchema")
      val returnVal = newHeader +  filterHandler(streamSchema,  filterParams(0), filterParams(1),"data",nodeName,dataStream)
      LogMessage(nodeName, s"Filter OP Completed")
      returnVal
      //"filtered Stream"
    }

    def FilterPRAStream(dataStreamName: String, filter: String):String = {
      LogMessage(nodeName, s"\nFilter OP Started")
      val filterParams = FilterHelpers.parseFilterArguments(filter)
      var dataStream = ""
      val intermediateResult = Networking.fetchContentFromNetwork(dataStreamName, ccnApi, 500 milliseconds)
      if(intermediateResult.isDefined){
        dataStream = new String(intermediateResult.get.data)
      }
      else{
        dataStream = "Fail"
        LogMessage(nodeName, s"Filter OP Completed")
        val newDataStreamName = s"node/$nodeName/Filter($dataStreamName,$filter)/${System.currentTimeMillis()}"
        Networking.storeResult(nodeName,dataStream,newDataStreamName,ccnApi)
        return newDataStreamName
      }
      val streamHeader = dataStream.split("\n")(0)
      val streamSchema = streamHeader.toString().split("-")(1)
      val newHeader = s"Filter($dataStreamName,$filter)" + " - " + streamSchema + "\n"
      LogMessage(nodeName,s"Schema is $streamSchema")
      val returnVal = newHeader +  filterHandler(streamSchema,  filterParams(0), filterParams(1),"data",nodeName,dataStream)
      LogMessage(nodeName, s"Filter OP Completed")
      val newDataStreamName = s"node/$nodeName/Filter($dataStreamName,$filter)/${System.currentTimeMillis()}"
      Networking.storeResult(nodeName,returnVal,newDataStreamName,ccnApi)
      newDataStreamName
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



    def filterHandler(streamSchema: String, aNDS: ArrayBuffer[String], oRS: ArrayBuffer[String], outputFormat: String, nodeName: String, datastream: String): String = {
      var output = ""

      LogMessage(nodeName, "Handle Filter Stream")
      //At this point, we will definitely have the intermediate window result.
      LogMessage(nodeName, s"Inside Filter -> Stream to filter: $datastream")
      val delimiter = SensorHelpers.getDelimiterFromLine(datastream)
      LogMessage(nodeName, s"Delimiter is $delimiter")
      output = filter(streamSchema, datastream, aNDS, oRS, delimiter)


      //If outputFormat = name => we will return a named interest
      //If outputFormat = data => we will return the data
      LogMessage(nodeName, s"Inside Filter -> \nFILTER content: ${output}")
      output
    }

    NFNStringValue(
      args match {
        //Output format: Either name (/node/Filter/Sensor/Time) or data (data value directly)
        //[data/sensor][string of data][filter][outputFormat]
          // Hier unterscheidung für communicationappraoch
        case Seq(communicationApproach: NFNStringValue, dataStreamName: NFNStringValue, filter:NFNStringValue) =>
        initialUCLFilter()

        case Seq(communicationApproach: NFNStringValue, dataStreamName: NFNStringValue, filter:NFNStringValue, dataStream: NFNStringValue) =>
          communicationApproach.str.toLowerCase() match{
            case "ucl" => filterUCLStream(dataStreamName.str, filter.str, dataStream.str)
            case _ =>FilterPRAStream(filter.str, dataStream.str)
          }

        case _ =>
          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
      }
    )
  }

  def filter(streamSchema: String, dataStream: String, aNDS: ArrayBuffer[String], oRS: ArrayBuffer[String], delimiter: String): String = {
    var data: List[String] = null
    var output = ""
    data =
      dataStream.split("\n")
        .toSeq
        .map(_.trim)
        .filter(_ != "").toList.drop(1)

    if (data.nonEmpty) {
      data.foreach(line => {
        var lineAdded = false
        var andConditionisValid = true
        aNDS.foreach(
          and =>
            if (andConditionisValid && !FilterHelpers.conditionHandler(streamSchema, and, line, delimiter)) {
              andConditionisValid = false
            })

        if (andConditionisValid && !lineAdded) {
          output += "#" + line.toString + "\n"
          lineAdded = true
        }

        if (!lineAdded) {
          oRS.foreach(
            or => {
              if (!lineAdded && FilterHelpers.conditionHandler(streamSchema, or, line, delimiter)) {
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
      output += "Empty!"
    output
  }
}

