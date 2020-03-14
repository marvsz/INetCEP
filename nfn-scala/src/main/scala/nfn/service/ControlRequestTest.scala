package nfn.service

import SACEPICN.StatesSingleton
import akka.actor.ActorRef
import ccn.packet.{CCNName, Interest}
import nfn.tools.Networking._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ControlRequestTest() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future{

    if (args.length != 1) {
      throw NFNServiceArgumentException(s"$ccnName takes a single string argument.")
    }

    val name = args.head match {
      case NFNStringValue(s) => s
      case _ => throw NFNServiceArgumentException(s"$ccnName takes a single string argument.")
    }

    var count = 0
    while (true) {
      println(s"Fetching local content for ${interestName}")
      fetchRequestsToComputation(Interest(interestName), ccnApi) match {
        case Some(request) => intermediateResult(ccnApi, interestName, count, NFNStringValue(request.requestType.toString)); count += 1
        case None => println("No requests to computation found.")
      }
      Thread.sleep(1000)
    }
    NFNIntValue(1)
  }
}

