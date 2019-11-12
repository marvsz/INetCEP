package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MinusService() extends  NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] =Future {
    args match {
      case Seq(l: NFNIntValue, r: NFNIntValue) => {
        val res = NFNIntValue(l.i - r.i)
        res
      }
      case Seq(l: NFNFloatValue, r: NFNFloatValue) => {
        val res = NFNFloatValue(l.f - r.f)
        res
      }
      case _ =>
        throw new NFNServiceArgumentException(s"$ccnName requires to arguments of type NFNIntValue and not $args")
    }
  }
}
