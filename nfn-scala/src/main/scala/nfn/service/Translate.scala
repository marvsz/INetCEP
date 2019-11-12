package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.Seq
import scala.concurrent.Future

class Translate() extends NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = Future {
    args match {
      case Seq(NFNContentObjectValue(name, data)) =>
        // translating should happen here
        val str = new String(data)
        NFNContentObjectValue(name, ((str + " ")*3).getBytes)
      case _ =>
        throw new NFNServiceArgumentException(s"Translate service requires a single CCNName as a argument and not $args")
    }
  }
}
