package nfn.service

import INetCEP.StatesSingleton
import akka.actor.ActorRef
import ccn.packet.CCNName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
 * Created by basil on 17/06/14.
 */
class NackServ extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], stateHolder:StatesSingleton,ccnApi: ActorRef): Future[NFNValue] = Future{
    throw new NFNServiceExecutionException("Provoking a nack")
  }
}
