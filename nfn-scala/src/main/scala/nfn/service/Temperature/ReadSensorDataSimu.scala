package nfn.service.Temperature

import INetCEP.StatesSingleton
import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.service.{NFNIntValue, NFNService, NFNStringValue, NFNValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by blacksheeep on 13/11/15.
 */
class ReadSensorDataSimu() extends NFNService  {

  val consttemp = 20
  val constpreasure = 1000

  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future {

    (args.head, args.tail.head) match { // sensorname, datapoint
      case (sensorname: NFNStringValue, datapoint: NFNIntValue) => {

        sensorname.str match {
          case "Temperature" => {
            NFNIntValue(
              consttemp + (if (datapoint.i % 2 == 0) datapoint.i else (-datapoint.i))
            )
          }
          case "Pressure" => {
            NFNIntValue(
              constpreasure + (if (datapoint.i % 2 == 0) datapoint.i else (-datapoint.i))
            )
          }
        }
      }
      case _ => ???
    }
  }
}
