package nfn.service
import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.service.PlacementServices.QueryPlacement
import node.LocalNode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ServiceStarter(node: LocalNode) extends NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue] = {

   def publishService(service: String) : Future[NFNValue] = Future {

     var retVal = ""

     service.toLowerCase() match {
       case "window" => {
         node.publishServiceLocalPrefix(new Window())
         retVal = "Window Service published Locally"
       }
       case "queryplacement" => {
         node.publishServiceLocalPrefix(new QueryPlacement())
           retVal = "QueryPlacement Service published Locally"
       }
       case "filter" => {
         node.publishServiceLocalPrefix(new Filter())
         retVal = "Filter Service published Locally"
       }
       case "sequence" => {
         node.publishServiceLocalPrefix(new Sequence())
         retVal = "Sequence Service published Locally"
       }
       case "prediction1" => {
         node.publishServiceLocalPrefix(new Prediction1)
         retVal = "Prediction1 Service published Locally"
       }
       case "prediction2" => {
         node.publishServiceLocalPrefix(new Prediction2)
         retVal = "Prediction2 Service published Locally"
       }
       case "heatmap" => {
         node.publishServiceLocalPrefix(new Heatmap)
         retVal = "Heatmap Service published Locally"
       }
       case "updatenodestate" => {
         node.publishServiceLocalPrefix(new UpdateNodeState)
         retVal = "Update Nodestate Service published Locally"
       }
       case "getcontent" => {
         node.publishServiceLocalPrefix(new GetContent())
         retVal = "Get Content Service published Locally"
       }
       case "join" => {
         node.publishServiceLocalPrefix(new Join())
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
