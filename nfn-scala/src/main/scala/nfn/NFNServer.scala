package nfn

import java.net.InetSocketAddress

import akka.actor._
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import ccn._
import ccn.ccnlite.CCNLiteInterfaceCli
import ccn.packet._
import com.typesafe.scalalogging.LazyLogging
import config.{ComputeNodeConfig, RouterConfig, StaticConfig}
import monitor.Monitor
import monitor.Monitor.PacketLogWithoutConfigs
import network._
import nfn.NFNServer._
import nfn.localAbstractMachine.LocalAbstractMachineWorker
import nfn.service.PlacementServices.QueryPlacement
import nfn.service.{Filter, GetContent, Heatmap, Join, NFNServiceLibrary, NFNStringValue, Prediction1, Prediction2, Sequence, UpdateNodeState, Window}
import node.LocalNode

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}


object NFNServer {


  case class ComputeResult(content: Content)

  case class Exit()

}

object NFNApi {

  case class CCNSendReceive(interest: Interest, useThunks: Boolean, allowLocal: Boolean)

  case class CCNSendPersistentInterest(persistentInterest: PersistentInterest, interestedComputation: CCNName, useThunks: Boolean)

  case class CCNStartService(interest: Interest, useThunks: Boolean)

  case class AddToCCNCache(content: Content)

  case class AddDataStreamToCCNCache(content: Content)

  case class AddToCCNCacheAck(name: CCNName)

  case class AddToLocalCache(content: Content, prependLocalPrefix: Boolean = false)

  case class GetFromLocalCache(interest: Interest)

  case class AddIntermediateResult(content: Content)

}


object NFNServerFactory extends LazyLogging {
  def nfnServer(context: ActorRefFactory, nfnRouterConfig: RouterConfig, computeNodeConfig: ComputeNodeConfig) = {


    val wireFormat = StaticConfig.packetformat

    implicit val execContext = context.dispatcher
    val ccnLiteIf: CCNInterface = CCNLiteInterfaceCli(wireFormat)

    context.actorOf(networkProps(nfnRouterConfig, computeNodeConfig, ccnLiteIf), name = "NFNServer")
  }

  def networkProps(nfnNodeConfig: RouterConfig, computeNodeConfig: ComputeNodeConfig, ccnIf: CCNInterface) =
    Props(classOf[NFNServer], nfnNodeConfig, computeNodeConfig, ccnIf)
}

object UDPConnectionWireFormatEncoder {
  def apply(context: ActorRefFactory, from: InetSocketAddress, to: InetSocketAddress, ccnIf: CCNInterface): ActorRef = {
    context.actorOf(
      Props(
        classOf[UDPConnectionWireFormatEncoder],
        from,
        to,
        ccnIf
      ),
      name = s"udpsocket-${from.getPort}-${to.getPort}"
    )
  }
}

// This class takes out some work from the NFN server.
// It encodes each packet send to the network to the wireformat and it also logs all send messages
class UDPConnectionWireFormatEncoder(local: InetSocketAddress,
                                     target: InetSocketAddress,
                                     ccnLite: CCNInterface) extends UDPConnection(local, Some(target)) {


  implicit val execContext = context.dispatcher

  override def receive = super.receive orElse interestContentReceiveWithoutLog

  def interestContentReceiveWithoutLog: Receive = {
    case p: CCNPacket => {
      val senderCopy = sender
      handlePacket(p, senderCopy)
    }
  }

  override def ready(actorRef: ActorRef) = super.ready(actorRef) orElse interestContentReceive

  def interestContentReceive: Receive = {
    case p: CCNPacket => {
      logPacket(p)
      val senderCopy = sender
      handlePacket(p, senderCopy)
    }
  }

  def logPacket(packet: CCNPacket) = {
    val maybePacketLog = packet match {
      case i: Interest => Some(Monitor.InterestInfoLog("interest", i.name.toString))
      case ci: PersistentInterest => Some(Monitor.PersistentInterestInfoLog("constantInterest",ci.name.toString))
      case rci: RemovePersistentInterest => Some(Monitor.RemovePersistentInterestInfoLog("removeConstantInterest",rci.name.toString))
      case c: Content => Some(Monitor.ContentInfoLog("content", c.name.toString, c.possiblyShortenedDataString))
      case n: Nack => Some(Monitor.ContentInfoLog("content", n.name.toString, ":NACK"))
      case a: AddToCacheAck => None // Not Monitored for now
      case a: AddToCacheNack => None // Not Monitored for now
    }

    maybePacketLog map { packetLog =>
      Monitor.monitor ! new PacketLogWithoutConfigs(local.getHostString, local.getPort, target.getHostString, target.getPort, true, packetLog)
    }
  }

  //Updated by Ali
  def handlePacket(packet: CCNPacket, senderCopy: ActorRef) = {
    packet match {
      case i: Interest =>
        logger.debug(s"handling interest: $packet")
        ccnLite.mkBinaryInterest(i) onComplete {
          case Success(binaryInterest) =>
            logger.debug(s"Sending binary interest for $i to network")
            self.tell(UDPConnection.Send(binaryInterest), senderCopy)
          case Failure(e) => logger.error(e, s"could not create binary interest for $i")
        }
      case ci: PersistentInterest =>
        logger.debug(s"handling constant interest: $packet")
        ccnLite.mkBinaryPersistentInterest(ci) onComplete{
          case Success(binaryPersistentInterest) =>
            logger.debug(s"Sending binary constant interest for $ci to network")
            self.tell(UDPConnection.Send(binaryPersistentInterest), senderCopy)
          case Failure(e) => logger.error(e,s"could not create binary constant interest for $ci")
        }
      case rci: RemovePersistentInterest =>
        logger.debug(s"handling constant interest: $packet")
        ccnLite.mkBinaryRemovePersistentInterest(rci) onComplete{
          case Success(binaryRemovePersistentInterest) =>
            logger.debug(s"Sending binary constant interest for $rci to network")
            self.tell(UDPConnection.Send(binaryRemovePersistentInterest), senderCopy)
          case Failure(e) => logger.error(e,s"could not create binary constant interest for $rci")
        }
      case c: Content =>
        ccnLite.mkBinaryDatastreamContent(c) onComplete {
          case Success(binaryContents) => {
            logger.debug(s"Sending ${binaryContents} binary content objects for $c to network")
            binaryContents foreach { binaryContent =>
              self.tell(UDPConnection.Send(binaryContent), senderCopy)
            }
          }
          case Failure(e) => logger.error(e, s"could not create binary content for $c")
        }
      case n: Nack =>
        ccnLite.mkBinaryContent(Content(n.name, n.content.getBytes, MetaInfo.empty)) onComplete {
          case Success(binaryContents) => {
            binaryContents foreach { binaryContent =>
              self.tell(UDPConnection.Send(binaryContent), senderCopy)
            }
          }
          case Failure(e) => logger.error(e, s"could not create binary nack for $n")
        }
      case a: AddToCacheAck =>
        logger.warning("received AddToCacheAck to send to a UDPConnection, dropping it")
      case a: AddToCacheNack =>
        logger.warning("received AddToCacheNack to send to a UDPConnection, dropping it!")
    }
  }
}

/**
 * The NFNServer is the gateway interface to the CCNNetwork and provides the NFNServer implements the [[NFNApi]].
 * It manages a localAbstractMachine cs form where any incoming content requests are served.
 * It also maintains a pit for received interests. Since everything is akka based, the faces in the pit are [[ActorRef]],
 * this means that usually these refs are to:
 *  - interest from the network with the help of [[UDPConnection]] (or any future connection type)
 *  - the [[ComputeServer]] or [[ComputeWorker]]can make use of the pit
 *  - any interest with the help of the akka "ask" pattern
 * All connection, interest and content request are logged to the [[Monitor]].
 * A NFNServer also maintains a socket which is connected to the actual CCNNetwork, usually an CCNLiteInterfaceWrapper instance encapsulated in a [[CCNLiteProcess]].
 */
//case class NFNServer(maybeNFNNodeConfig: Option[RouterConfig], maybeComputeNodeConfig: Option[ComputeNodeConfig]) extends Actor {
case class NFNServer(routerConfig: RouterConfig, computeNodeConfig: ComputeNodeConfig, ccnIf: CCNInterface) extends Actor {

  implicit val execContext = context.dispatcher

  val logger = Logging(context.system, this)

  val cacheContent: Boolean = true

  val computeServer: ActorRef = context.actorOf(Props(classOf[ComputeServer], computeNodeConfig.prefix), name = "ComputeServer")

  val maybeLocalAbstractMachine: Option[ActorRef] =
    if (computeNodeConfig.withLocalAM)
      Some(context.actorOf(Props(classOf[LocalAbstractMachineWorker], self), "LocalAM"))
    else None

  val defaultTimeoutDuration = StaticConfig.defaultTimeoutDuration
  val cs = ContentStore()
  val nfnGateway: ActorRef =
    UDPConnectionWireFormatEncoder(
      context.system,
      new InetSocketAddress(computeNodeConfig.host, computeNodeConfig.port),
      new InetSocketAddress(routerConfig.host, routerConfig.port),
      ccnIf
    )
  var pit: PIT = PIT(context)
  var pqt: PQT = PQT(context)

  override def preStart() = {
    nfnGateway ! UDPConnection.Handler(self)
  }

  override def receive: Actor.Receive = {
    // received Data from network
    // If it is an interest, start a compute request
    case packet: CCNPacket => {
      val senderCopy = sender
      handlePacket(packet, senderCopy,true)
    }
    case UDPConnection.Received(data, sendingRemote) => {
      val senderCopy = sender
      ccnIf.wireFormatDataToXmlPacket(data) onComplete {
        case Success(packet) => self.tell(packet, senderCopy)
        case Failure(ex) => logger.error(ex, "could not parse data")
      }
    }

    /**
     * [[NFNApi.CCNSendReceive]] is a message of the external API which retrieves the content for the interest and sends it back to the sender actor.
     * The sender actor can be initialized from an ask patter or form another actor.
     * It tries to first serve the interest from the localAbstractMachine cs, otherwise it adds an entry to the pit
     * and asks the network if it was the first entry in the pit.
     * Thunk interests get converted to normal interests, thunks need to be enabled with the boolean flag in the message
     */
    case NFNApi.CCNSendReceive(interest, useThunks, allowLocal) => {
      val senderCopy = sender

      logger.debug(s"API: Sending interest $interest (f=$senderCopy)")
      val maybeThunkInterest =
        if (interest.name.isNFN && useThunks) interest.thunkify
        else interest
      handlePacket(maybeThunkInterest, senderCopy, allowLocal)
    }

    /**
     * [[NFNApi.CCNSendPersistentInterest]] is a message of the external API which retrieves the content for the interest and sends it back to the sender actor.
     * The sender actor can be initialized from an ask patter or form another actor.
     * It tries to first serve the interest from the localAbstractMachine cs, otherwise it adds an entry to the pit
     * and asks the network if it was the first entry in the pit.
     * Thunk interests get converted to normal interests, thunks need to be enabled with the boolean flag in the message
     */
    case NFNApi.CCNSendPersistentInterest(persistentInterest:PersistentInterest, interestedComputation:CCNName, useThunks:Boolean) => {
      val senderCopy = sender
      logger.debug(s"the name of the interested Computation we actually store is ${interestedComputation}")
      logger.debug(s"The Comps of the constant Interest are ${persistentInterest.name.cmps.toString()}")
      logger.debug(s"The Comps of the interested Computation are ${interestedComputation.cmps.toString()}")
      pqt.add(persistentInterest.name,interestedComputation, defaultTimeoutDuration)
      logger.debug(s"API: Sending interest $persistentInterest (f=$senderCopy)")
      val maybeThunkInterest =
        if (persistentInterest.name.isNFN && useThunks) persistentInterest.thunkify
        else persistentInterest
      handlePacket(maybeThunkInterest, senderCopy,false)
    }

    case NFNApi.CCNStartService(interest, useThunks) => {
      val senderCopy = sender
      val maybeThunkInterest =
        if (interest.name.isNFN && useThunks) interest.thunkify
        else interest
      handlePacket(maybeThunkInterest, senderCopy,false)
    }

    case NFNApi.AddToCCNCache(content) => {
      val senderCopy = sender
      logger.info(s"creating add to cache messages for $content")
      cs.add(content)
      ccnIf.addToCache(content, routerConfig.mgmntSocket) onComplete {
        case Success(n) =>
          logger.debug(s"Send $n AddToCache requests for content $content to router ")
          //          logger.debug(s"Name: ${content.name}")
          senderCopy ! NFNApi.AddToCCNCacheAck(content.name)
        case Failure(ex) => logger.error(ex, s"Could not add to CCN cache for $content")
      }
    }

    case NFNApi.AddDataStreamToCCNCache(content) => {
      val senderCopy = sender
      logger.info(s"creating add to cahce messages for $content")
      cs.add(content)
      ccnIf.addDataStreamToCache(content, routerConfig.mgmntSocket) onComplete {
        case Success(n) =>
          logger.debug(s"Send $n AddToCache requests for content $content to router ")
          //          logger.debug(s"Name: ${content.name}")
          senderCopy ! NFNApi.AddToCCNCacheAck(content.name)
        case Failure(ex) => logger.error(ex, s"Could not add to CCN cache for $content")
      }

    }

    case NFNApi.AddToLocalCache(content, prependLocalPrefix) => {
      val contentToAdd =
        if (prependLocalPrefix) {
          Content(computeNodeConfig.prefix.append(content.name), content.data, MetaInfo.empty)
        } else content
      logger.info(s"Adding content for ${contentToAdd.name} to local cache")
      cs.add(contentToAdd)
    }

    case NFNApi.GetFromLocalCache(interest) => {
      val senderCopy = sender
      logger.info(s"Searching local cache for prefix ${interest.name}.")
      val contents = cs.find(interest.name)
      if (contents.isDefined) {
        cs.remove(contents.get.name)
      }
      logger.info(s"Found entries: ${contents.isDefined}")
      senderCopy ! contents
    }

    case NFNApi.AddIntermediateResult(intermediateContent) => {
      logger.info(s"Adding intermediate result for ${intermediateContent.name} to CCN cache.")
      val futIntermediateData = intermediateDataOrRedirect(self, intermediateContent.name, intermediateContent.data)
      futIntermediateData map {
        resultData => Content(intermediateContent.name, resultData, MetaInfo.empty)
      } onComplete {
        case Success(content) => {
          self ! NFNApi.AddToCCNCache(content)
        }
        case Failure(ex) => logger.error(ex, s"Could not add intermediate content to CCN cache for $intermediateContent")
      }
    }

    case Exit() => {
      exit()
      context.system.terminate()
    }
  }

  def handlePacket(packet: CCNPacket, senderCopy: ActorRef, allowLocal: Boolean) = {
    packet match {
      case i: Interest => {
        logger.info(s"Received interest: $i (f=$senderCopy)")
        handleInterest(i, senderCopy, allowLocal)
      }
      case ci: PersistentInterest => {
        logger.info(s"Received constantInterest: $ci (f=$senderCopy)")
        handlePersistentInterest(ci, senderCopy)
      }
      case rci: RemovePersistentInterest => {
        logger.info(s"Received constantInterest: $rci (f=$senderCopy)")
        handleRemovePersistentInterest(rci, senderCopy)
      }
      case c: Content => {
        logger.info(s"Received content: $c (f=$senderCopy)")
        c.name.chunkNum match {
          case Some(chunknum) => handleContentChunk(c, senderCopy)
          case _ => handleContent(c, senderCopy)
        }
      }
      case n: Nack => {
        logger.info(s"Received NAck: $n")
        handleNack(n, senderCopy)
      }
      case a: AddToCacheAck => {
        logger.debug(s"Received AddToCacheAck")
      }
      case a: AddToCacheNack => {
        logger.error(s"Received AddToCacheNack")
      }
    }
  }

  def startDynService(i: Interest, senderCopy: ActorRef) = {

    var debugVal = ""
    val serviceName = i.name.withoutSStart.toString.toLowerCase().drop(1)
    logger.debug(s"Service name is:$serviceName")
    serviceName match {
      case "window" => {
        NFNServiceLibrary.nfnPublishService(new Window(), Some(computeNodeConfig.prefix),self)
        debugVal = "Window Service published Locally"
      }
      case "queryplacement" => {
        NFNServiceLibrary.nfnPublishService(new QueryPlacement(), Some(computeNodeConfig.prefix),self)
        debugVal = "QueryPlacement Service published Locally"
      }
      case "filter" => {
        NFNServiceLibrary.nfnPublishService(new Filter(), Some(computeNodeConfig.prefix),self)
        debugVal = "Filter Service published Locally"
      }
      case "sequence" => {
        NFNServiceLibrary.nfnPublishService(new Sequence(), Some(computeNodeConfig.prefix),self)
        debugVal = "Sequence Service published Locally"
      }
      case "prediction1" => {
        NFNServiceLibrary.nfnPublishService(new Prediction1(), Some(computeNodeConfig.prefix),self)
        debugVal = "Prediction1 Service published Locally"
      }
      case "prediction2" => {
        NFNServiceLibrary.nfnPublishService(new Prediction2(), Some(computeNodeConfig.prefix),self)
        debugVal = "Prediction2 Service published Locally"
      }
      case "heatmap" => {
        NFNServiceLibrary.nfnPublishService(new Heatmap(), Some(computeNodeConfig.prefix),self)
        debugVal = "Heatmap Service published Locally"
      }
      case "updatenodestate" => {
        NFNServiceLibrary.nfnPublishService(new UpdateNodeState(), Some(computeNodeConfig.prefix),self)
        debugVal = "Update Nodestate Service published Locally"
      }
      case "getcontent" => {
        NFNServiceLibrary.nfnPublishService(new GetContent(), Some(computeNodeConfig.prefix),self)
        debugVal = "Get Content Service published Locally"
      }
      case "join" => {
        NFNServiceLibrary.nfnPublishService(new Join(), Some(computeNodeConfig.prefix),self)
        debugVal = "Join Service published Locally"
      }
      case _ => debugVal = "Service not found"
    }
    logger.debug(s"Service name is:$debugVal")
  }

  //Updated by Ali
  private def handleContentChunk(contentChunk: Content, senderCopy: ActorRef): Unit = {
    logger.debug("enter handleContentChunk")

    logger.debug(s"Content Chunk name: ${contentChunk.name}")
    logger.debug(s"Content name without chunk: ${contentChunk.name.withoutChunk}")

    var maybeFace = pit.get(contentChunk.name)
    if (maybeFace.isEmpty) {
      logger.debug(s"maybeFace was empty")
      maybeFace = pit.get(contentChunk.name.withoutChunk)
    }
    if (maybeFace.isEmpty) {
      logger.error(s"content ${contentChunk.name} not found in PIT")
      return
    }
    val face: mutable.Set[ActorRef] = maybeFace match {
      case Some(f) => f
      case None => null
    }
    //val face: Set[ActorRef] = pit.get(contentChunk.name) match {case Some(f) => f}

    logger.debug(s"Face found")

    cs.add(contentChunk)
    cs.getContentCompleteOrIncompletedChunks(contentChunk.name) match {
      case Left(content) =>
        logger.debug(s"unchunkified content $content")
        handleContent(content, senderCopy)
      case Right(chunkNums) => {
        logger.debug(s"Content is in chunks")
        chunkNums match {
          case chunkNum :: _ =>
            val chunkInterest = Interest(CCNName(contentChunk.name.cmps, Some(chunkNum)))
            self ! NFNApi.CCNSendReceive(chunkInterest, contentChunk.name.isThunk,true)
          case _ => logger.warning(s"chunk store was already removed or never existed in contentstore for contentname ${contentChunk.name}")
        }
      }
    }
    pit.get(contentChunk.name) match {
      //      case Some(name) => {pit.add}
      case _ => face foreach {
        pit.add(contentChunk.name,false, _, 10 seconds)
      }
    }
  }

  private def handleContent(content: Content, senderCopy: ActorRef) = {

    if (content.name.isThunk && !content.name.isCompute) {
      logger.debug(s"handle interest thunk content => content name is thunk and content name is not compute")
      handleInterstThunkContent
    } else {
      logger.debug(s"is Thunk? " + content.name.isThunk.toString)
      logger.debug(s"is Compute? " + content.name.isCompute.toString)
      logger.debug(s"handle non thunk content decision")
      handleNonThunkContent
    }

    def handleInterstThunkContent: Unit = {
      def timeoutFromContent: FiniteDuration = {
        val timeoutInContent = new String(content.data)
        if (timeoutInContent != "" && timeoutInContent.forall(_.isDigit)) {
          timeoutInContent.toInt.seconds
        } else {
          defaultTimeoutDuration
        }
      }


      pit.get(content.name) match {
        case Some(pendingFaces) => {
          val (contentNameWithoutThunk, isThunk) = content.name.withoutThunkAndIsThunk

          assert(isThunk, s"handleInterestThunkContent received the content object $content which is not a thunk")

          val interest = Interest(contentNameWithoutThunk)
          logger.debug(s"Received usethunk $content, sending actual interest $interest")
          //          logger.debug(s"Timeout duration: ${timeout.duration}")
          val timeout = Timeout(timeoutFromContent)
          pendingFaces foreach { pf =>
            pit.add(contentNameWithoutThunk, false, pf, timeout.duration)
          }

          nfnGateway ! interest
          pendingFaces foreach {
            pf => pit.remove(content.name,pf)
          }

        }
        case None =>
          logger.error(s"Discarding thunk content $content because there is no entry in pit")
      }
    }

    def handleNonThunkContent: Unit = {
      logger.debug(s"Ich bin hier und jetzt muesste was kommen weil ich nonthunkcontent handle ")
      //FIXME: Version hack for Openmhealth
      val cname = if (content.name.cmps.head == "org" && content.name.cmps.tail.head == "openmhealth" && content.name.cmps.contains("catalog"))
        CCNName(content.name.cmps.reverse.tail.reverse, None) else content.name
      println(pit.toString())
      pit.get(cname) match {
        //FIXME: End of the hack for Openmhealth
        //pit.get(content.name) match { //FIXME: if hack for Openmhealth is removed, uncomment this!
        case Some(pendingFaces) => {
          val isCountIntermediates = content.name.isRequest && content.name.requestType == "CIM"
          if (cacheContent && !content.name.isKeepalive && !isCountIntermediates) {
            logger.debug(s"some(pendingFaces) ist der fall")
            logger.debug(s"content.name = " + content.name.toString)
            cs.add(content)
          }

          val redirect = "redirect:".getBytes
          logger.debug(s"wir sind jetzt nach dem cs.add bzw. haben festgestellt, dass wir ein redirect haben")
          if (content.data.startsWith(redirect)) {
            logger.debug(s"wir wissen, dass das ein redirect ist, jetzt machen wir irgendwas")
            logger.debug(s"redirect: content.name = " + content.name.toString)
            logger.debug(s"redirect content.data = " + new String(content.data))
          }

          // Check if content is a redirect
          // if it is a redirect, send an interest for each pending face with the redirect name
          // otherwise return the ocntent object to all pending faces
          logger.debug(s"wir pruefen, ob der content ein redirect ist")
          if (!content.name.isCompute && content.data.startsWith(redirect)) {
            logger.debug(s"We have established, that the content is a redirect")
            val nameCmps: List[String] = new String(content.data).split("redirect:")(1).split("/").tail.toList

            val unescapedNameCmps = CCNLiteInterfaceCli.unescapeCmps(nameCmps)
            logger.debug(s"unescapednamecmps: " + unescapedNameCmps.toString())
            logger.info(s"Redirect for $unescapedNameCmps")
            implicit val timeout = Timeout(defaultTimeoutDuration)
            (self ? NFNApi.CCNSendReceive(Interest(CCNName(unescapedNameCmps, None)), useThunks = false,true)).mapTo[CCNPacket] map {
              case c: Content => {
                logger.debug(s"fetch content from each pending face")
                pendingFaces foreach { pendingFace => {
                  pendingFace ! c
                  pit.remove(content.name,pendingFace)
                }}

                logger.debug(s"finished fetching content")
              }
              case nonContent@_ =>
                logger.warning(s"Received $nonContent when fetching a redirected content, dropping it")
            }
          } else {
            pendingFaces foreach { pendingFace => {
              pendingFace ! content
              pit.remove(content.name,pendingFace)
            } }

          }
          logger.debug(s"Checking if a pending query interest is present")
          pqt.get(content.name) match {
            case Some(queryIntersts) => {
              logger.debug(s"Found at least one pending query interest")
              queryIntersts foreach {
                qi => computeServer ! ComputeServer.ComputeDataStream(qi,Seq(NFNStringValue(new String(content.data))))
              }
            }
            case None =>{
              logger.info(s"No matching entry in pqt but in pit. Content of the PQT is "+ pqt.toString())
            }
          }
        }
        case None =>
          logger.debug(s"Checking if a pending query interest is present")
          pqt.get(content.name) match {
            case Some(queryIntersts) => {
              logger.debug(s"Found at least one pending query interest")
              queryIntersts foreach {
                qi => computeServer ! ComputeServer.ComputeDataStream(qi,Seq(NFNStringValue(new String(content.data))))
              }
            }
            case None =>{
              logger.warning(s"Discarding content $content because there is no entry in pit " + pit.toString())
              logger.debug(s"Content of the PQT is "+ pqt.toString())
            }

          }
      }
    }
  }

  private def handleInterest(i: Interest, senderCopy: ActorRef, allowLocal: Boolean) = {

    /*if (i.name.isKeepalive) {
      logger.debug(s"Receive keepalive interest: " + i.name)
      val nfnCmps = i.name.cmps.patch(i.name.cmps.size - 2, Nil, 1)
      val nfnName = i.name.copy(cmps = nfnCmps)
      pit.get(nfnName) match {
        case Some(pendingInterest) => logger.debug(s"Found in PIT.")
          senderCopy ! Content(i.name, " ".getBytes)
        case None => logger.debug(s"Did not find in PIT.")
      }
    } else {*/
    logger.debug(s"Handle interest.")
    allowLocal match {
      case true => cs.get(i.name) match {
        /*Hier wird der kram vom lokalen Speicher geholt. wollen wir das? (22.7.2019) Weiterhin ist im Query Store script was komisch, da irgendwann folgendes passiert:

      *[ERROR] [07/22/2019 00:59:44.759] [Sys-node-nodeA-akka.actor.default-dispatcher-11] [akka://Sys-node-nodeA/user/NFNServer/ComputeServer/ComputeWorker-931157519] Added to futures: /COMPUTE/call 9 /node/nodeA/nfn_service_Placement 'Centralized' '1' '' 'QS' 'FILTER(name,WINDOW(name,victims,4,S),3=M&4>30,name)' 'Region1' '16:22:00.200' '00:59:44.563'/NFN
  success

      Also wird hier ein Feld nicht befüllt. schauen, ob das irgendwo probleme gibt.
      */
        case Some(contentFromLocalCS) => {
          logger.debug(s"Served $contentFromLocalCS from local CS")
          senderCopy ! contentFromLocalCS
        }
        case None => {
          val senderFace = senderCopy
          pit.get(i.name) match {
            case Some(pendingFaces) => {
              if (!i.name.isRequest) {
                pit.add(i.name, false, senderFace, defaultTimeoutDuration)
                //                nfnGateway ! i
              }
            }
            case None => {
              if(i.name.isSStart){
                startDynService(i, senderCopy)
              }
              else{
                if (!i.name.isRequest || i.name.requestType == "CIM" || i.name.requestType == "GIM") {
                  pit.add(i.name, false, senderFace, defaultTimeoutDuration)
                }

                // If the interest has a chunknum, make sure that the original interest (still) exists in the pit
                i.name.chunkNum foreach { _ => {
                  logger.info("Try to add content for a specific chunk to the senderFace")
                  pit.add(CCNName(i.name.cmps, None), false, senderFace, defaultTimeoutDuration)

                }

                }



                //Updated by Ali
                // /.../.../NFN
                // nfn interests are either:
                // - send to the compute server if they start with compute
                // - send to a local AM if one exists
                // - forwarded to nfn gateway
                // not nfn interests are always forwarded to the nfn gateway
                logger.info("The interest is NFN? " + i.name.isNFN)
                logger.info("The interest is called :" + i.name)
                if (i.name.isNFN) {
                  // /COMPUTE/call .../.../NFN
                  // A compute flag at the beginning means that the interest is a binary computation
                  // to be executed on the compute server
                  if (i.name.isCompute) {
                    logger.debug(s"Interest is a compute interest: ${i.name}")
                    if (i.name.isThunk) {
                      logger.debug(s"Interest is a compute interest and with Thunks: ${i.name}")
                      computeServer ! ComputeServer.Thunk(i.name)
                    } else if (i.name.isRequest) {
                      i.name.requestType match {
                        case "KEEPALIVE" => {
                          logger.debug(s"Receive keepalive interest: " + i.name)
                          val nfnCmps = i.name.cmps.patch(i.name.cmps.size - 3, Nil, 2)
                          val nfnName = i.name.copy(cmps = nfnCmps)
                          pit.get(nfnName) match {
                            case Some(pendingInterest) => logger.debug(s"Found in PIT.")
                              senderCopy ! Content(i.name, " ".getBytes)
                            case None => logger.debug(s"Did not find in PIT.")
                          }
                        }
                        case "CTRL" => {
                          logger.debug(s"Receive control message: " + i.name + " Save to CS for later retrieval by computation.")
                          val emptyContent = Content(i.name, Array[Byte]())
                          cs.add(emptyContent)
                          senderCopy ! Content(i.name, " ".getBytes)
                        }
                        case _ => {
                          computeServer ! ComputeServer.RequestToComputation(i.name, senderCopy)
                        }
                      }
                    } else {
                      logger.debug(s"the name of the compute interest we have to store is ${i.name}")
                      computeServer ! ComputeServer.Compute(i.name)
                    }
                    // /.../.../NFN
                    // An NFN interest without compute flag means that it must be reduced by an abstract machine
                    // If no local machine is available, forward it to the nfn network
                  } else {
                    logger.debug(s"Interest is a simple NFN interest: ${i.name}")
                    maybeLocalAbstractMachine match {
                      case Some(localAbstractMachine) => {
                        localAbstractMachine ! i
                      }
                      case None => {
                        nfnGateway ! i
                      }
                    }
                  }
                } else {
                  nfnGateway ! i
                }
              }
            }
          }
        }
      }
      case false => {
        val senderFace = senderCopy
        pit.get(i.name) match {
          case Some(pendingFaces) => {
            if (!i.name.isRequest) {
              pit.add(i.name, false, senderFace, defaultTimeoutDuration)
              //                nfnGateway ! i
            }
          }
          case None => {
            if(i.name.isSStart){
              startDynService(i, senderCopy)
            }
            else{
              if (!i.name.isRequest || i.name.requestType == "CIM" || i.name.requestType == "GIM") {
                pit.add(i.name, false, senderFace, defaultTimeoutDuration)
              }

              // If the interest has a chunknum, make sure that the original interest (still) exists in the pit
              i.name.chunkNum foreach { _ => {
                logger.info("Try to add content for a specific chunk to the senderFace")
                pit.add(CCNName(i.name.cmps, None), false, senderFace, defaultTimeoutDuration)

              }

              }



              //Updated by Ali
              // /.../.../NFN
              // nfn interests are either:
              // - send to the compute server if they start with compute
              // - send to a local AM if one exists
              // - forwarded to nfn gateway
              // not nfn interests are always forwarded to the nfn gateway
              logger.info("The interest is NFN? " + i.name.isNFN)
              logger.info("The interest is called :" + i.name)
              if (i.name.isNFN) {
                // /COMPUTE/call .../.../NFN
                // A compute flag at the beginning means that the interest is a binary computation
                // to be executed on the compute server
                if (i.name.isCompute) {
                  logger.debug(s"Interest is a compute interest: ${i.name}")
                  if (i.name.isThunk) {
                    logger.debug(s"Interest is a compute interest and with Thunks: ${i.name}")
                    computeServer ! ComputeServer.Thunk(i.name)
                  } else if (i.name.isRequest) {
                    i.name.requestType match {
                      case "KEEPALIVE" => {
                        logger.debug(s"Receive keepalive interest: " + i.name)
                        val nfnCmps = i.name.cmps.patch(i.name.cmps.size - 3, Nil, 2)
                        val nfnName = i.name.copy(cmps = nfnCmps)
                        pit.get(nfnName) match {
                          case Some(pendingInterest) => logger.debug(s"Found in PIT.")
                            senderCopy ! Content(i.name, " ".getBytes)
                          case None => logger.debug(s"Did not find in PIT.")
                        }
                      }
                      case "CTRL" => {
                        logger.debug(s"Receive control message: " + i.name + " Save to CS for later retrieval by computation.")
                        val emptyContent = Content(i.name, Array[Byte]())
                        cs.add(emptyContent)
                        senderCopy ! Content(i.name, " ".getBytes)
                      }
                      case _ => {
                        computeServer ! ComputeServer.RequestToComputation(i.name, senderCopy)
                      }
                    }
                  } else {
                    logger.debug(s"the name of the compute interest we have to store is ${i.name}")
                    computeServer ! ComputeServer.Compute(i.name)
                  }
                  // /.../.../NFN
                  // An NFN interest without compute flag means that it must be reduced by an abstract machine
                  // If no local machine is available, forward it to the nfn network
                } else {
                  logger.debug(s"Interest is a simple NFN interest: ${i.name}")
                  maybeLocalAbstractMachine match {
                    case Some(localAbstractMachine) => {
                      localAbstractMachine ! i
                    }
                    case None => {
                      nfnGateway ! i
                    }
                  }
                }
              } else {
                nfnGateway ! i
              }
            }
          }
        }
      }
    }

    //}
  }

  private def handlePersistentInterest(i: PersistentInterest, senderCopy: ActorRef) = {

    /*if (i.name.isKeepalive) {
      logger.debug(s"Receive keepalive interest: " + i.name)
      val nfnCmps = i.name.cmps.patch(i.name.cmps.size - 2, Nil, 1)
      val nfnName = i.name.copy(cmps = nfnCmps)
      pit.get(nfnName) match {
        case Some(pendingInterest) => logger.debug(s"Found in PIT.")
          senderCopy ! Content(i.name, " ".getBytes)
        case None => logger.debug(s"Did not find in PIT.")
      }
    } else {*/
    logger.debug(s"Handle persistent interest.")
    cs.get(i.name) match {
      /*Hier wird der kram vom lokalen Speicher geholt. wollen wir das? (22.7.2019) Weiterhin ist im Query Store script was komisch, da irgendwann folgendes passiert:

    *[ERROR] [07/22/2019 00:59:44.759] [Sys-node-nodeA-akka.actor.default-dispatcher-11] [akka://Sys-node-nodeA/user/NFNServer/ComputeServer/ComputeWorker-931157519] Added to futures: /COMPUTE/call 9 /node/nodeA/nfn_service_Placement 'Centralized' '1' '' 'QS' 'FILTER(name,WINDOW(name,victims,4,S),3=M&4>30,name)' 'Region1' '16:22:00.200' '00:59:44.563'/NFN
success

    Also wird hier ein Feld nicht befüllt. schauen, ob das irgendwo probleme gibt.
    */
      /*case Some(contentFromLocalCS) => {
        logger.debug(s"Served $contentFromLocalCS from local CS")
        senderCopy ! contentFromLocalCS
      }*/
      case _ => {
        val senderFace = senderCopy
        pit.get(i.name) match {
          case Some(pendingFaces) => {
            if (!i.name.isRequest) {
              pit.add(i.name, true, senderFace, defaultTimeoutDuration)
              //                nfnGateway ! i
            }
          }
          case None => {
            if (!i.name.isRequest || i.name.requestType == "CIM" || i.name.requestType == "GIM") {
              logger.info(s"Adding ${i.name.toString} to the PIT.")
              pit.add(i.name, true, senderFace, defaultTimeoutDuration)

            }

            // If the interest has a chunknum, make sure that the original interest (still) exists in the pit
            i.name.chunkNum foreach { _ => {
              logger.info("Try to add content for a specific chunk to the senderFace")
              pit.add(CCNName(i.name.cmps, None), true, senderFace, defaultTimeoutDuration)

            }

            }

            //Updated by Ali
            // /.../.../NFN
            // nfn interests are either:
            // - send to the compute server if they start with compute
            // - send to a local AM if one exists
            // - forwarded to nfn gateway
            // not nfn interests are always forwarded to the nfn gateway
            logger.info("The interest is NFN? " + i.name.isNFN)
            logger.info("The interest is called :" + i.name)
            if (i.name.isNFN) {
              // /COMPUTE/call .../.../NFN
              // A compute flag at the beginning means that the interest is a binary computation
              // to be executed on the compute server
              if (i.name.isCompute) {
                logger.debug(s"Interest is a compute interest: ${i.name}")
                if (i.name.isThunk) {
                  logger.debug(s"Interest is a compute interest and with Thunks: ${i.name}")
                  computeServer ! ComputeServer.Thunk(i.name)
                } else if (i.name.isRequest) {
                  i.name.requestType match {
                    case "KEEPALIVE" => {
                      logger.debug(s"Receive keepalive interest: " + i.name)
                      val nfnCmps = i.name.cmps.patch(i.name.cmps.size - 3, Nil, 2)
                      val nfnName = i.name.copy(cmps = nfnCmps)
                      pit.get(nfnName) match {
                        case Some(pendingInterest) => logger.debug(s"Found in PIT.")
                          senderCopy ! Content(i.name, " ".getBytes)
                        case None => logger.debug(s"Did not find in PIT.")
                      }
                    }
                    case "CTRL" => {
                      logger.debug(s"Receive control message: " + i.name + " Save to CS for later retrieval by computation.")
                      val emptyContent = Content(i.name, Array[Byte]())
                      cs.add(emptyContent)
                      senderCopy ! Content(i.name, " ".getBytes)
                    }
                    case _ => {
                      computeServer ! ComputeServer.RequestToComputation(i.name, senderCopy)
                    }
                  }
                } else {
                  computeServer ! ComputeServer.Compute(i.name)
                }
                // /.../.../NFN
                // An NFN interest without compute flag means that it must be reduced by an abstract machine
                // If no local machine is available, forward it to the nfn network
              } else {
                logger.debug(s"Interest is a simple NFN interest: ${i.name}")
                maybeLocalAbstractMachine match {
                  case Some(localAbstractMachine) => {
                    localAbstractMachine ! i
                  }
                  case None => {
                    nfnGateway ! i
                  }
                }
              }
            } else {
              nfnGateway ! i
            }
          }
        }
      }
    }
    //}
  }

  private def handleRemovePersistentInterest(i: RemovePersistentInterest, senderCopy: ActorRef) = {

    /*if (i.name.isKeepalive) {
      logger.debug(s"Receive keepalive interest: " + i.name)
      val nfnCmps = i.name.cmps.patch(i.name.cmps.size - 2, Nil, 1)
      val nfnName = i.name.copy(cmps = nfnCmps)
      pit.get(nfnName) match {
        case Some(pendingInterest) => logger.debug(s"Found in PIT.")
          senderCopy ! Content(i.name, " ".getBytes)
        case None => logger.debug(s"Did not find in PIT.")
      }
    } else {*/
    logger.debug(s"Handle interest.")
    cs.get(i.name) match {
      /*Hier wird der kram vom lokalen Speicher geholt. wollen wir das? (22.7.2019) Weiterhin ist im Query Store script was komisch, da irgendwann folgendes passiert:

    *[ERROR] [07/22/2019 00:59:44.759] [Sys-node-nodeA-akka.actor.default-dispatcher-11] [akka://Sys-node-nodeA/user/NFNServer/ComputeServer/ComputeWorker-931157519] Added to futures: /COMPUTE/call 9 /node/nodeA/nfn_service_Placement 'Centralized' '1' '' 'QS' 'FILTER(name,WINDOW(name,victims,4,S),3=M&4>30,name)' 'Region1' '16:22:00.200' '00:59:44.563'/NFN
success

    Also wird hier ein Feld nicht befüllt. schauen, ob das irgendwo probleme gibt.
    */
      case Some(contentFromLocalCS) => {
        logger.debug(s"Served $contentFromLocalCS from local CS")
        senderCopy ! contentFromLocalCS
      }
      case None => {
        val senderFace = senderCopy
        pit.get(i.name) match {
          case Some(pendingFaces) => {
            if (!i.name.isRequest) {
              pit.add(i.name, false, senderFace, defaultTimeoutDuration)
              //                nfnGateway ! i
            }
          }
          case None => {
            if (!i.name.isRequest || i.name.requestType == "CIM" || i.name.requestType == "GIM") {
              pit.add(i.name, false,senderFace, defaultTimeoutDuration)
            }

            // If the interest has a chunknum, make sure that the original interest (still) exists in the pit
            i.name.chunkNum foreach { _ => {
              logger.info("Try to add content for a specific chunk to the senderFace")
              pit.add(CCNName(i.name.cmps, None),false, senderFace, defaultTimeoutDuration)

            }

            }

            //Updated by Ali
            // /.../.../NFN
            // nfn interests are either:
            // - send to the compute server if they start with compute
            // - send to a local AM if one exists
            // - forwarded to nfn gateway
            // not nfn interests are always forwarded to the nfn gateway
            logger.info("The interest is NFN? " + i.name.isNFN)
            logger.info("The interest is called :" + i.name)
            if (i.name.isNFN) {
              // /COMPUTE/call .../.../NFN
              // A compute flag at the beginning means that the interest is a binary computation
              // to be executed on the compute server
              if (i.name.isCompute) {
                logger.debug(s"Interest is a compute interest: ${i.name}")
                if (i.name.isThunk) {
                  logger.debug(s"Interest is a compute interest and with Thunks: ${i.name}")
                  computeServer ! ComputeServer.Thunk(i.name)
                } else if (i.name.isRequest) {
                  i.name.requestType match {
                    case "KEEPALIVE" => {
                      logger.debug(s"Receive keepalive interest: " + i.name)
                      val nfnCmps = i.name.cmps.patch(i.name.cmps.size - 3, Nil, 2)
                      val nfnName = i.name.copy(cmps = nfnCmps)
                      pit.get(nfnName) match {
                        case Some(pendingInterest) => logger.debug(s"Found in PIT.")
                          senderCopy ! Content(i.name, " ".getBytes)
                        case None => logger.debug(s"Did not find in PIT.")
                      }
                    }
                    case "CTRL" => {
                      logger.debug(s"Receive control message: " + i.name + " Save to CS for later retrieval by computation.")
                      val emptyContent = Content(i.name, Array[Byte]())
                      cs.add(emptyContent)
                      senderCopy ! Content(i.name, " ".getBytes)
                    }
                    case _ => {
                      computeServer ! ComputeServer.RequestToComputation(i.name, senderCopy)
                    }
                  }
                } else {
                  computeServer ! ComputeServer.Compute(i.name)
                }
                // /.../.../NFN
                // An NFN interest without compute flag means that it must be reduced by an abstract machine
                // If no local machine is available, forward it to the nfn network
              } else {
                logger.debug(s"Interest is a simple NFN interest: ${i.name}")
                maybeLocalAbstractMachine match {
                  case Some(localAbstractMachine) => {
                    localAbstractMachine ! i
                  }
                  case None => {
                    nfnGateway ! i
                  }
                }
              }
            } else {
              nfnGateway ! i
            }
          }
        }
      }
    }
    //}
  }

  def handleNack(nack: Nack, senderCopy: ActorRef) = {
    if (StaticConfig.isNackEnabled) {
      implicit val timeout = Timeout(defaultTimeoutDuration)
      pit.get(nack.name) match {
        case Some(pendingFaces) => {
          pendingFaces foreach {
            _ ! nack
          }
          pendingFaces foreach {
            pit.remove(nack.name,_)
          }
        }
        case None => logger.warning(s"Received nack for name which is not in PIT: $nack")
      }
    } else {
      logger.error(s"Received nack even though nacks are disabled!")
    }
  }

  def exit(): Unit = {
    computeServer ! PoisonPill
    nfnGateway ! PoisonPill
  }


  def intermediateDataOrRedirect(ccnApi: ActorRef, name: CCNName, data: Array[Byte]): Future[Array[Byte]] = {
    if (data.size > CCNLiteInterfaceCli.maxChunkSize) {
      name.expression match {
        case Some(expr) =>
          val cmps = computeNodeConfig.prefix.cmps ++ List(expr)
          val redirectName = CCNName(cmps, None).withIntermediate(name.intermediateIndex).withNFN
          val redirectCmps = redirectName.cmps
          //          val redirectCmps
          //      val redirectCmps = name.cmps
          val content = Content(redirectName, data)
          implicit val timeout = StaticConfig.defaultTimeout
          ccnApi ? NFNApi.AddToCCNCache(content) map {
            case NFNApi.AddToCCNCacheAck(_) =>
              val escapedComponents = CCNLiteInterfaceCli.escapeCmps(redirectCmps)
              val redirectResult: String = "redirect:" + escapedComponents.mkString("/", "/", "")
              redirectResult.getBytes
            case answer@_ => throw new Exception(s"Asked for addToCache for $content and expected addToCacheAck but received $answer")
          }
        case None => throw new Exception(s"Name $name could not be transformed to an expression")
      }
    } else Future(data)
  }
}
