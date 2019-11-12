package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.tools.SensorHelpers
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.Seq
import scala.concurrent.Future

class RemoveSensor extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = Future {

    def removeSensor(filename: String): String = {
      SensorHelpers.removeSensor(filename)
      //Start the script for continuously reading the sensor here
      "Sensor removed from node"
    }

    NFNStringValue(
      args match{
        case Seq(filename: NFNStringValue) => removeSensor(filename.str)
      }
    )
  }
}

