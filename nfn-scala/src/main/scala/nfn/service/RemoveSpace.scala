package nfn.service

import SACEPICN.StatesSingleton
import akka.actor.ActorRef
import ccn.packet.CCNName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
 * Created by blacksheeep on 01.12.14.
 */
class RemoveSpace extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future {
    args match {
      case Seq(NFNStringValue(arg1)) => NFNStringValue(arg1.filter(_ != ' '))
      case _ => ???
    }
  }
}
