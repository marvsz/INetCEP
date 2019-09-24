package nfn.service

/**
 * Created by Johannes on 31.8.2019
 */

import akka.actor.ActorRef
import nfn.tools.Helpers

//Added for contentfetch
import ccn.packet.CCNName

import scala.language.postfixOps

//Added for CCN Command Execution:
import config.StaticConfig

class Join2() extends NFNService {
  val sacepicnEnv = StaticConfig.systemPath

  def joinStreams(left: String, right: String)={
    var output = ""
    val sb = new StringBuilder
    val leftTrimmed = Helpers.trimData(left)
    val rightTrimmed = Helpers.trimData(right)
    val leftDelimiter = Helpers.getDelimiterFromLine(leftTrimmed)
    val rightDelimiter = Helpers.getDelimiterFromLine(rightTrimmed)
    val leftTrimmedSplit : Array[String] = leftTrimmed.split("\n")
    val rightTrimmedSplit : Array[String] = rightTrimmed.split("\n")
    //if the delimiters are different we need to choose one, in this case the delimiter from the left data stream
    if (leftDelimiter != rightDelimiter) {
      rightTrimmed.replace(rightDelimiter,leftDelimiter)
    }
    // We need to check whether we have equally long lines.
    val size = leftTrimmedSplit.size
    val lineDifference = leftTrimmedSplit.size - rightTrimmedSplit.size
    if(lineDifference == 0){
      for(i <- 0 to leftTrimmedSplit.size-1){
        sb.append(leftTrimmedSplit(i) + leftDelimiter + rightTrimmedSplit(i)+"\n")
      }
    }
    output = sb.toString()
    if(output == "")
      output = "No Results!"
    else
      output = output.stripSuffix("\n").stripMargin('#')
    output
  }

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    val nodeInfo = interestName.cmps.mkString(" ")
    var nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)

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
        return joinStreams(left, right)
      }
      if (outputFormat == "name") {
        output = Helpers.storeOutput(nodeName, output, "JOIN", "onOperators", ccnApi)
      }
      else {
        LogMessage(nodeName, s"Inside Join -> JOIN name: NONE, JOIN content: ${output}")
      }
      LogMessage(nodeName, s"Join OP Completed\n")
      output
    }

    NFNStringValue(
      args match {
        case Seq(timestamp: NFNStringValue, inputSource: NFNStringValue, outputFormat: NFNStringValue, left: NFNStringValue, right: NFNStringValue) => processJoin(inputSource.str, left.str, right.str, outputFormat.str)
        case _ =>
          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
      }
    )
  }

}
