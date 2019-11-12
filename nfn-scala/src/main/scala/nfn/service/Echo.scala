package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName

import scala.collection.mutable
import scala.collection.mutable.Seq
import scala.concurrent.Future

class Echo() extends NFNService {
  override def function(interestName: CCNName, args: mutable.Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = ???

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = Future{
    NFNDataValue(args.map(value => value.toDataRepresentation).reduceLeft(_ ++ _))
  }
}

