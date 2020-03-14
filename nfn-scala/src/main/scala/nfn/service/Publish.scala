package nfn.service

import SACEPICN.StatesSingleton
import akka.actor.ActorRef
import ccn.packet.{CCNName, Content, MetaInfo}
import nfn.NFNApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Publish() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], stateHolder:StatesSingleton,ccnApi: ActorRef): Future[NFNValue] = Future {
    args match {
      case Seq(NFNContentObjectValue(contentName, contentData), NFNContentObjectValue(_, publishPrefixNameData), _) => {
        val nameOfContentWithoutPrefixToAdd = CCNName(new String(publishPrefixNameData).split("/").tail.toIndexedSeq:_*)
        ccnApi ! NFNApi.AddToLocalCache(Content(nameOfContentWithoutPrefixToAdd, contentData, MetaInfo.empty), prependLocalPrefix = true)
        NFNEmptyValue()
      }
      case _ =>
        throw new NFNServiceArgumentException(s"$ccnName can only be applied to arguments of type CCNNameValue and not: $args")
    }
  }
}
