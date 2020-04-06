package nfn.service

import INetCEP.StatesSingleton
import akka.actor.ActorRef
import ccn.packet.CCNName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
 * Simple service which takes a sequence of [[NFNIntValue]] and sums them to a single [[NFNIntValue]]
 */
class SumService() extends NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future {
    NFNIntValue(
      args.map({
        case i: NFNIntValue => i.i
        case _ => throw  new NFNServiceArgumentException(s"SumService requires a sequence of NFNIntValue's and not $args")
      }).sum
    )
  }
}
