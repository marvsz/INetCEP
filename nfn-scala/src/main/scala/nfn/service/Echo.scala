package nfn.service

import INetCEP.StatesSingleton
import akka.actor.ActorRef
import ccn.packet.CCNName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Echo() extends NFNService {
  /*override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = {
    Future(NFNDataValue(args.map(value => value.toDataRepresentation).reduceLeft(_ ++ _)))
  }*/
  override def function(interestName: CCNName, args: Seq[NFNValue], stateHolder:StatesSingleton,ccnApi: ActorRef): Future[NFNValue] = {
    Future(NFNDataValue(args.map(value => value.toDataRepresentation).reduceLeft(_ ++ _)))
  }
}

