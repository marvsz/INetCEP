package nfn.service.GPS.NDNfit

import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.service.{NFNIntValue, NFNService, NFNValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OriginFilter extends NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = Future {
    (args.head, args.tail.head) match{
      case (startTime: NFNIntValue,endTime:NFNIntValue) => {

        // todo
        ???

      }

      case _ => ???

    }
  }
}
