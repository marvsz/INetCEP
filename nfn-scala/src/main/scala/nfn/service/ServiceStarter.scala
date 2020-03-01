package nfn.service
import akka.actor.ActorRef
import ccn.packet.{CCNName, ServiceStarterInterest}
import nfn.tools.Networking

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ServiceStarter() extends NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = {
    var nodeInfo = interestName.cmps.mkString(" ")
    var nodeName = nodeInfo.substring(nodeInfo.indexOf("/node") + 6, nodeInfo.indexOf("nfn_service") - 1)

   def publishService(service: String) : Future[NFNValue] = Future {

     LogMessage(nodeName,"Started publishing service.")
     var retVal = ""

     service.toLowerCase() match {
       case "window" => {
         Networking.startService(ServiceStarterInterest("window"), ccnApi)
         retVal = "Window Service published Locally"
       }
       case "queryplacement" => {
         Networking.startService(ServiceStarterInterest("window"), ccnApi)
           retVal = "QueryPlacement Service published Locally"
       }
       case "filter" => {
         Networking.startService(ServiceStarterInterest("window"), ccnApi)
         retVal = "Filter Service published Locally"
       }
       case "sequence" => {
         Networking.startService(ServiceStarterInterest("window"), ccnApi)
         retVal = "Sequence Service published Locally"
       }
       case "prediction1" => {
         Networking.startService(ServiceStarterInterest("window"), ccnApi)
         retVal = "Prediction1 Service published Locally"
       }
       case "prediction2" => {
         Networking.startService(ServiceStarterInterest("window"), ccnApi)
         retVal = "Prediction2 Service published Locally"
       }
       case "heatmap" => {
         Networking.startService(ServiceStarterInterest("window"), ccnApi)
         retVal = "Heatmap Service published Locally"
       }
       case "updatenodestate" => {
         Networking.startService(ServiceStarterInterest("window"), ccnApi)
         retVal = "Update Nodestate Service published Locally"
       }
       case "getcontent" => {
         Networking.startService(ServiceStarterInterest("window"), ccnApi)
         retVal = "Get Content Service published Locally"
       }
       case "join" => {
         Networking.startService(ServiceStarterInterest("window"), ccnApi)
         retVal = "Join Service published Locally"
       }
       case _ => retVal = "Service not found"
     }
     NFNStringValue(retVal)
   }

    args match {
      case Seq(timestamp:NFNStringValue, service: NFNStringValue) => publishService(service.str)
      case _ =>throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
    }
  }
}
