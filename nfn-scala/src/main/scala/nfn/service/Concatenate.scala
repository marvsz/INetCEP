package nfn.service

/**
  * Created by Ali on 06.02.18.
  */

import INetCEP.StatesSingleton
import akka.actor.ActorRef
import nfn.tools.{Helpers, SensorHelpers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

//Added for contentfetch
import ccn.packet.CCNName

import scala.language.postfixOps

//Added for CCN Command Execution:
import config.StaticConfig

class Concatenate() extends NFNService {
  val sacepicnEnv = StaticConfig.systemPath

  def joinStreams(right: String, left: String) = {
    var output = ""
    var sb = new StringBuilder
    sb.append(SensorHelpers.trimData(left))
    sb.append(SensorHelpers.trimData(right))
    output = sb.toString()
    if(output == "")
      output = "No Results!"
    else
      output = output.stripSuffix("\n").stripMargin('#')
    output
  }

  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future{
    val nodeInfo = interestName.cmps.mkString(" ")
    val nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)

    def joinStrings(left: String, right: String) = "[" + left + "]" + "," + "[" + right + "]"

    def processJoin(inputSource: String, left: String, right: String, outputFormat: String): String = {

      LogMessage(nodeName, s"\nJoin OP Started")
      var output = ""
      if (inputSource == "name") {
        LogMessage(nodeName, "Handle left stream")
        val intermediateResultLeft = Helpers.handleNamedInputSource(nodeName, left, ccnApi)
        LogMessage(nodeName, "Handle right stream")
        val intermediateResultRight = Helpers.handleNamedInputSource(nodeName, right, ccnApi)
        output = joinStreams(intermediateResultLeft, intermediateResultRight)
      }
      else if (inputSource == "data") {
        return joinStrings(left, right)
      }
      //sort the output to be in timely order
      //TODO: make this work for every time stamp as an input.
      if (output.contains(",")) {
        output = sortByDateTime(output)
      }
      if (outputFormat == "name") {
        output = Helpers.storeOutputLocally(nodeName, output, "JOIN", "onOperators", ccnApi)
      }
      else {
        LogMessage(nodeName, s"Inside Join -> JOIN name: NONE, JOIN content: ${output}")
      }
      LogMessage(nodeName, s"Join OP Completed\n")
      output
    }

    NFNStringValue(
      args match {
        //join strings => Pass strings to join
        case Seq(l: NFNIntValue, r: NFNIntValue) => joinStrings(l.i.toString, r.i.toString)
        case Seq(doc1: NFNContentObjectValue, doc2: NFNContentObjectValue) => joinStrings(new String(doc1.data), new String(doc2.data))
        case Seq(string1: NFNStringValue, string2: NFNStringValue) => joinStrings(string1.str, string2.str)
        //**Obsolete calls:
        //ProcessFiles => Process sensor values. Options are applied directly on sensors
        //case Seq(left: NFNStringValue, right: NFNStringValue, options: NFNStringValue) => processFiles(left, right, options)

        //These cases are used to resolve content objects if present in the query:
        //Options => Pass as NULL
        //InputSource => 'name' <- in this case, filter must be passed with name. Window by default returns name.
        //Case: JOIN('name' (name1) (name2) options)
        case Seq(timestamp: NFNStringValue, inputSource: NFNStringValue, outputFormat: NFNStringValue, left: NFNStringValue, right: NFNStringValue) => processJoin(inputSource.str, left.str, right.str, outputFormat.str)
        case _ =>
          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
      }
    )
  }

  def sortByDateTime(input: String) = {
    val linelist = input.split("\n").toList.sortWith(_.split(",")(1).toInt < _.split(",")(1).toInt)
    val output = new StringBuilder
    for (line <- linelist) {
      output.append(line + "\n")
    }
    output.toString()
  }
}