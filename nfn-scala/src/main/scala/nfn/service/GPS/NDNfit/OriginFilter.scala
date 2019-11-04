package nfn.service.GPS.NDNfit

import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.service.{NFNIntValue, NFNService, NFNValue}


class OriginFilter extends NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {
    (args.head, args.tail.head) match{
      case (startTime: NFNIntValue,endTime:NFNIntValue) => {

        // todo
        ???

      }

      case _ => ???

    }
  }
}
