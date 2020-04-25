package nfn.service
import INetCEP.StatesSingleton
import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.tools.IOHelpers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class QueryResultPrinter extends NFNService{
  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = {
    val nodeInfo = interestName.cmps.mkString(" ")
    val nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)

    def printQueryResult(result: String): Future[NFNValue] = Future{
      LogMessage(nodeName,s"QueryResultPrinter: Result = $result")
      IOHelpers.writeQueryOutput(nodeName,result)
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
