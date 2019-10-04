package nfn.service
/**
  * Created by Ali on 06.02.18.
  */
import SACEPICN.NodeMapping
import nfn.NFNApi
import nfn.service._
import nfn.tools.Networking._
import nfn.tools.Helpers
import akka.actor.ActorRef

import scala.io.Source
import scala.util.control._
import scala.collection.mutable
import scala.List
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.Vector
import scala.util.control.Exception._

//Added for contentfetch
import lambdacalculus.parser.ast.{Constant, Str}
import scala.concurrent.duration._
import scala.language.postfixOps
import java.util.Calendar
import java.time
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import ccn.packet.{CCNName, Content, MetaInfo, NFNInterest, Interest}


class UpdateNodeState() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    //Very important to remember: Content stored here through NFN Calls is ONLY available to this node.
    def updateNodeState(nodeID: String, content: String, timeOfUpdate: String): String = {
      val mapping = new NodeMapping();
      val now = Calendar.getInstance()
      val name = s"/${nodeID}/" + now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)

      val nodename = mapping.getName(nodeID)
      val nodeIP = mapping.getIPbyName(nodename)
      val nodePort = mapping.getPort(nodename);

      val nameOfContentWithoutPrefixToAdd = CCNName(new String(name).split("/").tail: _*)
      ccnApi ! NFNApi.AddToLocalCache(Content(nameOfContentWithoutPrefixToAdd, content.getBytes, MetaInfo.empty), prependLocalPrefix = false)
      LogMessage(nodename, s"Put Nodestate on Network with address: ${name} and Content: ${nameOfContentWithoutPrefixToAdd}. The IP is ${nodeIP} and the Port is ${nodePort}.")
      LogMessage(nodename, s"Check if it really is put on the network")
      val nodeContent = Helpers.executeInterestQuery(name,nodename,ccnApi)
      LogMessage(nodename, s"Content: ${nodeContent}")
      LogMessage(nodename, s"Content found")

      val name1 = s"/${nodeID}/" + now.get(Calendar.HOUR_OF_DAY) + (now.get(Calendar.MINUTE)-1).toString();
      val nameOfContentWithoutPrefixToAdd1 = CCNName(new String(name1).split("/").tail: _*)
      ccnApi ! NFNApi.AddToLocalCache(Content(nameOfContentWithoutPrefixToAdd1, content.getBytes, MetaInfo.empty), prependLocalPrefix = false)
      LogMessage(nodename, s"Put Nodestate on Network with address: ${name1} and Content: ${nameOfContentWithoutPrefixToAdd1}. The IP is ${nodeIP} and the Port is ${nodePort}.")
      LogMessage(nodename, s"Check if it really is put on the network")
      val nodeContent1 = Helpers.executeInterestQuery(name1,nodename,ccnApi)
      LogMessage(nodename, s"Content: ${nodeContent1}")
      LogMessage(nodename, s"Content found")

      val name2 = s"/${nodeID}/" + now.get(Calendar.HOUR_OF_DAY) + (now.get(Calendar.MINUTE)+1).toString()
      val nameOfContentWithoutPrefixToAdd2 = CCNName(new String(name2).split("/").tail: _*)
      ccnApi ! NFNApi.AddToLocalCache(Content(nameOfContentWithoutPrefixToAdd2, content.getBytes, MetaInfo.empty), prependLocalPrefix = false)
      LogMessage(nodename, s"Put Nodestate on Network with address: ${name2} and Content: ${nameOfContentWithoutPrefixToAdd2}. The IP is ${nodeIP} and the Port is ${nodePort}.")
      LogMessage(nodename, s"Check if it really is put on the network")
      val nodeContent2 = Helpers.executeInterestQuery(name2,nodename,ccnApi)
      LogMessage(nodename, s"Content: ${nodeContent2}")
      LogMessage(nodename, s"Content found")

      name
    }

    NFNStringValue(
    args match {
      case Seq(nodeID: NFNStringValue, content: NFNStringValue, timeOfUpdate:NFNStringValue) => updateNodeState(nodeID.str, content.str, timeOfUpdate.str)
      case _ =>
        throw NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
    }
    )
  }
}

