package nfn.service
import java.io.IOException
import java.nio.file.{Files, Paths, StandardOpenOption}

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
     var nodeNumber = 0
     var contentToWrite = ""
     nodeName match {
       case "nodeA" => nodeNumber = 28
       case "nodeB" => nodeNumber = 29
       case "nodeC" => nodeNumber = 30
       case "nodeD" => nodeNumber = 31
       case "nodeE" => nodeNumber = 32
       case "nodeF" => nodeNumber = 33
       case "nodeG" => nodeNumber = 34
       case _ => nodeNumber = 0
     }

     val path = Paths.get(s"/home/johannes/INetCEP/VM-Startup-Scripts/$nodeNumber/DeployedOperators.txt")
     var retVal = ""

     service.toLowerCase() match {
       case "window" => {
         Networking.startService(ServiceStarterInterest("window"), ccnApi)
         try{
           Files.createFile(path)
           contentToWrite = "window"
         }
         catch {
           case e: IOException => {
             LogMessage(nodeName,"File already existed, appending")
             contentToWrite = ",window"
           }
           case _: Throwable => LogMessage(nodeName,"Got some other kind of Throwable exception")
         }
         Files.write(path,contentToWrite.getBytes(),StandardOpenOption.APPEND)
         retVal = "Window Service published Locally"
       }
       case "queryplacement" => {
         Networking.startService(ServiceStarterInterest("queryplacement"), ccnApi)
         try{
           Files.createFile(path)
           contentToWrite = "queryplacement"
         }
         catch {
           case e: IOException => {
             LogMessage(nodeName,"File already existed, appending")
             contentToWrite = ",queryplacement"
           }
           case _: Throwable => LogMessage(nodeName,"Got some other kind of Throwable exception")
         }
         Files.write(path,contentToWrite.getBytes(),StandardOpenOption.APPEND)
           retVal = "QueryPlacement Service published Locally"
       }
       case "filter" => {
         Networking.startService(ServiceStarterInterest("filter"), ccnApi)
         try{
           Files.createFile(path)
           contentToWrite = "filter"
         }
         catch {
           case e: IOException => {
             LogMessage(nodeName,"File already existed, appending")
             contentToWrite = ",filter"
           }
           case _: Throwable => LogMessage(nodeName,"Got some other kind of Throwable exception")
         }
         Files.write(path,contentToWrite.getBytes(),StandardOpenOption.APPEND)
         retVal = "Filter Service published Locally"
       }
       case "sequence" => {
         Networking.startService(ServiceStarterInterest("sequence"), ccnApi)
         try{
           Files.createFile(path)
           contentToWrite = "sequence"
         }
         catch {
           case e: IOException => {
             LogMessage(nodeName,"File already existed, appending")
             contentToWrite = ",sequence"
           }
           case _: Throwable => LogMessage(nodeName,"Got some other kind of Throwable exception")
         }
         Files.write(path,contentToWrite.getBytes(),StandardOpenOption.APPEND)
         retVal = "Sequence Service published Locally"
       }
       case "prediction1" => {
         Networking.startService(ServiceStarterInterest("prediction1"), ccnApi)
         try{
           Files.createFile(path)
           contentToWrite = "prediction1"
         }
         catch {
           case e: IOException => {
             LogMessage(nodeName,"File already existed, appending")
             contentToWrite = ",prediction1"
           }
           case _: Throwable => LogMessage(nodeName,"Got some other kind of Throwable exception")
         }
         Files.write(path,contentToWrite.getBytes(),StandardOpenOption.APPEND)
         retVal = "Prediction1 Service published Locally"
       }
       case "prediction2" => {
         Networking.startService(ServiceStarterInterest("prediction2"), ccnApi)
         try{
           Files.createFile(path)
           contentToWrite = "prediction2"
         }
         catch {
           case e: IOException => {
             LogMessage(nodeName,"File already existed, appending")
             contentToWrite = ",prediction2"
           }
           case _: Throwable => LogMessage(nodeName,"Got some other kind of Throwable exception")
         }
         Files.write(path,contentToWrite.getBytes(),StandardOpenOption.APPEND)
         retVal = "Prediction2 Service published Locally"
       }
       case "heatmap" => {
         Networking.startService(ServiceStarterInterest("heatmap"), ccnApi)
         try{
           Files.createFile(path)
           contentToWrite = "heatmap"
         }
         catch {
           case e: IOException => {
             LogMessage(nodeName,"File already existed, appending")
             contentToWrite = ",heatmap"
           }
           case _: Throwable => LogMessage(nodeName,"Got some other kind of Throwable exception")
         }
         Files.write(path,contentToWrite.getBytes(),StandardOpenOption.APPEND)
         retVal = "Heatmap Service published Locally"
       }
       case "updatenodestate" => {
         Networking.startService(ServiceStarterInterest("updatenodestate"), ccnApi)
         try{
           Files.createFile(path)
           contentToWrite = "updatenodestate"
         }
         catch {
           case e: IOException => {
             LogMessage(nodeName,"File already existed, appending")
             contentToWrite = ",updatenodestate"
           }
           case _: Throwable => LogMessage(nodeName,"Got some other kind of Throwable exception")
         }
         Files.write(path,contentToWrite.getBytes(),StandardOpenOption.APPEND)
         retVal = "Update Nodestate Service published Locally"
       }
       case "getcontent" => {
         Networking.startService(ServiceStarterInterest("getcontent"), ccnApi)
         try{
           Files.createFile(path)
           contentToWrite = "getcontent"
         }
         catch {
           case e: IOException => {
             LogMessage(nodeName,"File already existed, appending")
             contentToWrite = ",getcontent"
           }
           case _: Throwable => LogMessage(nodeName,"Got some other kind of Throwable exception")
         }
         Files.write(path,contentToWrite.getBytes(),StandardOpenOption.APPEND)
         retVal = "Get Content Service published Locally"
       }
       case "join" => {
         Networking.startService(ServiceStarterInterest("join"), ccnApi)
         try{
           Files.createFile(path)
           contentToWrite = "join"
         }
         catch {
           case e: IOException => {
             LogMessage(nodeName,"File already existed, appending")
             contentToWrite = ",join"
           }
           case _: Throwable => LogMessage(nodeName,"Got some other kind of Throwable exception")
         }
         Files.write(path,contentToWrite.getBytes(),StandardOpenOption.APPEND)
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
