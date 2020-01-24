package nfn.service
import akka.actor.ActorRef
import ccn.packet.CCNName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ServiceSubscriber extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = {
    def subscribeToComputation(queryToExecute: String, queryInterestedIn: String) : Future[NFNValue] = Future{
      //nfn.tools.Networking.subscribeToQuery(queryToExecute.replaceAll("[|]","'"),queryInterestedIn.replaceAll("[|]","'"), ccnApi)
      NFNStringValue("Placed Subscriptions")
    }
    args match {
      case Seq(queryToExecute: NFNStringValue, queryInterestedIn: NFNStringValue) =>
        subscribeToComputation(queryToExecute.str, queryInterestedIn.str).recover {
          case e => throw e
        }
      case _ =>
        throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
    }
  }
}
