package nfn.service

import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.tools.SensorHelpers

class RemoveSensor extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {

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

