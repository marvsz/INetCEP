package nfn.service
import akka.actor.ActorRef
import ccn.packet.CCNName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ServiceSubscriber extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = {
    var nodeInfo = interestName.cmps.mkString(" ")
    var nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)
    def subscribeToComputation(queryToExecute: String, queryInterestedIn: String) : Future[NFNValue] = Future{
      var qte= queryToExecute.replaceAll("[|]","'")
      var qii = queryInterestedIn.replaceAll("[|]","'")
      qte = qte.substring(1,qte.length-1)
      qii = qii.substring(1,qii.length-1)
      /*qte = "COMPUTE/".concat(qte)
      qii = "COMPUTE/".concat(qii)*/
      LogMessage(nodeName,"The query to Execute is: "+qte)
      LogMessage(nodeName,"The query interested in  is: "+qii)
      nfn.tools.Networking.subscribeToQuery(qte, qii, ccnApi)
      NFNStringValue(s"Placed Subscriptions for ${qii} to be interested in ${qte}")
    }
    args match {
      case Seq(queryInterestedIn: NFNStringValue, queryToExecute: NFNStringValue) =>
        subscribeToComputation(queryToExecute.str, queryInterestedIn.str).recover {
          case e => throw e
        }
      case _ =>
        throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
    }
  }
}