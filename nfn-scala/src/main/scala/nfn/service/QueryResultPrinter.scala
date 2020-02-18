package nfn.service
import akka.actor.ActorRef
import ccn.packet.CCNName
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class QueryResultPrinter extends NFNService{
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = {
    var nodeInfo = interestName.cmps.mkString(" ")
    var nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)

    def printQueryResult(result: String): Future[NFNValue] = Future{
      NFNStringValue("Printed Result")
    }

    def initialQueryReslut() : Future[NFNValue] = Future {
      NFNStringValue("Initial Calling")
    }
    args match {
      case Seq(queryResult: NFNStringValue) =>
        printQueryResult(queryResult.str).recover{
          case e => throw e
        }
      case Seq() =>
        initialQueryReslut().recover{
          case e => throw e
        }
      case _ =>
        throw NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
    }
  }
}
