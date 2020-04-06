package nfn.service

import INetCEP.StatesSingleton
import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.tools.Networking._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class IntermediateTest() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future{

    for ( i <- 0 to 10) {
      if (!Thread.currentThread().isInterrupted) {
        intermediateResult(ccnApi, interestName, i, NFNStringValue("intermediate test " + i))
        Thread.sleep(1 * 1000)
      }
    }
    NFNStringValue("this is the final result")
  }
}

