package nfn.service.Temperature

import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.service._
import scala.language.postfixOps
import sys.process._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by blacksheeep on 21/01/16.
 */
class ReadSensorData() extends NFNService  {

  val consttemp = 20
  val constpreasure = 1000

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = Future {

    (args.head) match { // sensorname, datapoint
      case (dataPoint: NFNIntValue) => {
          var data = ("cat /sys/bus/w1/devices/28-00043c6106ff/w1_slave" !!)

          NFNDataValue(data.getBytes())
      }
      case _ => ???
    }
  }
}
