package nfn.service

/**
  * Created by Ali on 06.02.18.
  */
import akka.actor.ActorRef
import nfn.tools.Networking._

//Added for contentfetch
import ccn.packet.{CCNName, ConstantInterest}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class GetContent() extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = Future{

    def getValue(name: String): String = {
      val nameOfContentWithoutPrefixToAdd = CCNName(new String(name).split("/").tail.toIndexedSeq: _*)
      val nameOfContentWithPrefix = CCNName(new String(name).split("/").toIndexedSeq: _*)
      var intermediateResult = ""
      try{
        //intermediateResult = new String(fetchContent(Interest(nameOfContentWithPrefix), ccnApi, 15 seconds).get.data)
        intermediateResult = new String(fetchContent(ConstantInterest(nameOfContentWithPrefix), ccnApi, 15 seconds).get.data)
      }
      catch {
        case e : NoSuchElementException => intermediateResult = "Timeout"
      }

      // Extended for redirect by Johannes
      /*LogMessage("GetContent",s"trying to look into the result:"+intermediateResult)
      if(intermediateResult.contains("redirect")){
        LogMessage("GetContent",s"result contains redirect, trying to resolve it")
        var str = intermediateResult.replace("\n", "").trim
        val rname = CCNName(str.splitAt(9)._2.split("/").toList.tail.map(_.replace("%2F", "/").replace("%2f", "/")), None)
        val interest = new Interest(rname)
        intermediateResult = new String(fetchContent(interest,ccnApi,30 seconds).get.data)
      }*/

      intermediateResult
    }

    NFNStringValue(
    args match {
      case Seq(name: NFNStringValue) => getValue(name.str)
      case _ =>
        throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
    }
    )
  }
}

