package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddService() extends  NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = Future{
    args match {
      case Seq(l: NFNIntValue, r: NFNIntValue) => {
        NFNIntValue(l.i + r.i)
      }
      case _ =>
        throw new NFNServiceArgumentException(s"$ccnName requires two arguments of type NFNIntValue and not $args")
    }
  }
}
