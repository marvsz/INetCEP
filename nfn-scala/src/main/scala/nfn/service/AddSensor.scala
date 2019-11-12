package nfn.service
import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.tools.SensorHelpers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddSensor extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = Future{

    def addSensor(filename: String, delimiter: String): String = {
      SensorHelpers.addSensor(filename,delimiter)
      //Start the script for continuously reading the sensor here
      "Sensor added to node"
    }

    NFNStringValue(
      args match{
        case Seq(filename: NFNStringValue, granularity: NFNStringValue, delimiter: NFNStringValue) => addSensor(filename.str, delimiter.str)
      }
    )
  }
}
