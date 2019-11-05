package nfn.service
/**
  * Created by Ali on 06.02.18.
  */
import akka.actor.ActorRef
import nfn.NFNApi

//Added for contentfetch
import java.util.Calendar

import ccn.packet.{CCNName, Content, MetaInfo}

import scala.language.postfixOps


class UpdateNodeState() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

    //Very important to remember: Content stored here through NFN Calls is ONLY available to this node.
    def updateNodeState(nodeID: String, content: String, timeOfUpdate: String): String = {

      val now = Calendar.getInstance()
      val name = s"/${nodeID}/" + now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)

      /*val mapping = new NodeMapping();
      val nodename = mapping.getName(nodeID)
      val nodeIP = mapping.getIPbyName(nodename)
      val nodePort = mapping.getPort(nodename);*/

      val nameOfContentWithoutPrefixToAdd = CCNName(new String(name).split("/").tail.toIndexedSeq: _*)
      ccnApi ! NFNApi.AddToLocalCache(Content(nameOfContentWithoutPrefixToAdd, content.getBytes, MetaInfo.empty), prependLocalPrefix = false)
      /*LogMessage(nodename, s"Put Nodestate on Network with address: ${name} and Content: ${nameOfContentWithoutPrefixToAdd}. The IP is ${nodeIP} and the Port is ${nodePort}.")
      LogMessage(nodename, s"Check if it really is put on the network")
      val nodeContent = Helpers.executeInterestQuery(name,nodename,ccnApi)
      LogMessage(nodename, s"Content: ${nodeContent}")
      LogMessage(nodename, s"Content found")*/

      val name1 = s"/${nodeID}/" + now.get(Calendar.HOUR_OF_DAY) + (now.get(Calendar.MINUTE)-1).toString();
      val nameOfContentWithoutPrefixToAdd1 = CCNName(new String(name1).split("/").tail.toIndexedSeq: _*)
      ccnApi ! NFNApi.AddToLocalCache(Content(nameOfContentWithoutPrefixToAdd1, content.getBytes, MetaInfo.empty), prependLocalPrefix = false)
      /*LogMessage(nodename, s"Put Nodestate on Network with address: ${name1} and Content: ${nameOfContentWithoutPrefixToAdd1}. The IP is ${nodeIP} and the Port is ${nodePort}.")
      LogMessage(nodename, s"Check if it really is put on the network")
      val nodeContent1 = Helpers.executeInterestQuery(name1,nodename,ccnApi)
      LogMessage(nodename, s"Content: ${nodeContent1}")
      LogMessage(nodename, s"Content found")*/

      val name2 = s"/${nodeID}/" + now.get(Calendar.HOUR_OF_DAY) + (now.get(Calendar.MINUTE)+1).toString()
      val nameOfContentWithoutPrefixToAdd2 = CCNName(new String(name2).split("/").tail.toIndexedSeq: _*)
      ccnApi ! NFNApi.AddToLocalCache(Content(nameOfContentWithoutPrefixToAdd2, content.getBytes, MetaInfo.empty), prependLocalPrefix = false)
      /*LogMessage(nodename, s"Put Nodestate on Network with address: ${name2} and Content: ${nameOfContentWithoutPrefixToAdd2}. The IP is ${nodeIP} and the Port is ${nodePort}.")
      LogMessage(nodename, s"Check if it really is put on the network")
      val nodeContent2 = Helpers.executeInterestQuery(name2,nodename,ccnApi)
      LogMessage(nodename, s"Content: ${nodeContent2}")
      LogMessage(nodename, s"Content found")*/

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

