package nfn

import INetCEP.StatesSingleton
import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import akka.pattern.ask
import ccn.ccnlite.CCNLiteInterfaceCli
import ccn.packet.{CCNName, Content, MetaInfo}
import config.StaticConfig
import nfn.service._

import scala.collection.mutable.Map
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}


object ComputeWorker {
  case class Callable(callable: CallableNFNService)
  case class End()
  case class Cancel(name: CCNName)
}

class e extends Throwable

case class ComputeWorker(ccnServer: ActorRef, nodePrefix: CCNName, stateHolder:StatesSingleton) extends Actor {
  import context.dispatcher

  val logger = Logging(context.system, this)

  var maybeFutCallable: Option[Future[CallableNFNService]] = None

  var futures: Map[CCNName, KlangCancellableFuture[Any]] = Map()

  def receivedContent(content: Content) = {
    // Received content from request (sendrcv)
    logger.error(s"ComputeWorker received content, discarding it because it does not know what to do with it")
  }

  // Received compute request
  // Make sure it actually is a compute request and forward to the handle method
  def prepareCallable(computeName: CCNName, useThunks: Boolean, requestor: ActorRef): Option[Future[CallableNFNService]] = {
    if (computeName.isCompute && computeName.isNFN) {
      logger.debug(s"Received compute request, creating callable for: $computeName")
      val rawComputeName = computeName.withoutCompute.withoutThunk.withoutNFN
      assert(rawComputeName.cmps.size == 1, "Compute cmps at this moment should only have one component")

      val futCallableServ: Future[CallableNFNService] = NFNService.parseAndFindFromName(rawComputeName.cmps.head, stateHolder, ccnServer)
      // send back thunk content when callable service is creating (means everything was available)
      if (useThunks) {
        futCallableServ foreach { callableServ =>
          // TODO: No default value for default time estimate
          requestor ! Content(computeName, callableServ.executionTimeEstimate.fold("")(_.toString).getBytes, MetaInfo.empty)
        }
      }
      maybeFutCallable = Some(futCallableServ)
      maybeFutCallable
    } else {
      logger.error(s"Dropping compute interest $computeName, because it does not begin with ${CCNName.computeKeyword}, end with ${CCNName.nfnKeyword} or is not a thunk, therefore is not a valid compute interest")
      None
    }
  }

  def resultDataOrRedirect(data: Array[Byte], name: CCNName, ccnServer: ActorRef): Future[Array[Byte]] = {
    if(data.size > CCNLiteInterfaceCli.maxChunkSize) {
      name.expression match {
        case Some(expr) =>
          val redirectName = nodePrefix.cmps ++ List(expr)

          val content = Content(CCNName(redirectName, None), data)
          implicit val timeout = StaticConfig.defaultTimeout
          ccnServer ? NFNApi.AddToCCNCache(content) map {
            case NFNApi.AddToCCNCacheAck(_) =>
              val escapedComponents = CCNLiteInterfaceCli.escapeCmps(redirectName)
              val redirectResult: String = "redirect:" + escapedComponents.mkString("/", "/", "")
              logger.debug(s"received AddToCacheAck, returning redirect result $redirectResult")
              redirectResult.getBytes
            case answer @ _ => throw new Exception(s"Asked for addToCache for $content and expected addToCacheAck but received $answer")
          }
        case None => throw new Exception(s"Name $name could not be transformed to an expression")
      }
    } else {
      val fut = Future(data)
      fut
    }
  }

  def executeCallable(futCallable: Future[CallableNFNService], name: CCNName, senderCopy: ActorRef, additionalArgs: Seq[NFNValue]):Unit={
    futCallable foreach { callable =>
      val cancellable = KlangCancellableFuture {
        try {
          val resultValue: NFNValue = Await.result(callable.execWithArgs(additionalArgs),1 seconds)
          val futResultData = resultDataOrRedirect(resultValue.toDataRepresentation, name, ccnServer)
          val resultData = Await.result(futResultData, 1 seconds)
          Content(name.withoutThunkAndIsThunk._1, resultData, MetaInfo.empty)
          /*callable.execWithArgs(additionalArgs).onComplete{
            case Success(resVal) =>
              resultDataOrRedirect(resVal.toDataRepresentation, name, ccnServer).onComplete{
                case Success(resultData) =>
                  Content(name.withoutThunkAndIsThunk._1, resultData, MetaInfo.empty)
              }
          }*/
        } catch {
          case e: Exception =>
            println(s"Catched exception: $e")
        }
      }
      cancellable onComplete {
        case Success(content) => {
          logger.info(s"Finished computation, result: $content")
          senderCopy ! content
        }
        case Failure(ex) => {
          println("failure")
          logger.error(ex, s"Error when executing the service $name. Cause: ${ex.getCause} Message: ${ex.getMessage}")
        }
      }

      futures += name -> cancellable
      logger.info(s"Added to futures: $name")
    }
  }

  def executeCallable(futCallable: Future[CallableNFNService], name: CCNName, senderCopy: ActorRef): Unit = {
    futCallable foreach { callable =>
      val cancellable = KlangCancellableFuture {
        try {
          val resultValue: NFNValue = Await.result(callable.exec,60 seconds)
          val futResultData = resultDataOrRedirect(resultValue.toDataRepresentation, name, ccnServer)
          val resultData = Await.result(futResultData, 1 seconds)
          Content(name.withoutThunkAndIsThunk._1, resultData, MetaInfo.empty)
          /*callable.exec.onComplete{
            case Success(resVal) =>
              resultDataOrRedirect(resVal.toDataRepresentation, name, ccnServer).onComplete{
              case Success(resultData) =>
                Content(name.withoutThunkAndIsThunk._1, resultData, MetaInfo.empty)
              }
          }*/
        } catch {
          case e: Exception =>
            println(s"Catched exception: $e")
        }
      }
      cancellable onComplete {
        case Success(content) => {
          logger.info(s"Finished computation, result: $content")
          senderCopy ! content
        }
        case Failure(ex) => {
          println("failure")
          logger.error(ex, s"Error when executing the service $name. Cause: ${ex.getCause} Message: ${ex.getMessage}")
        }
      }

      futures += name -> cancellable
      logger.info(s"Added to futures: $name")
    }
//      try {
//        logger.error("Cancelling future 1")
//        cancellable.cancel()
//        logger.error("Cancelling future 2")
//      } catch {
//        case e: Exception => logger.error("Future cancelled.")
//      }
//      logger.error("Cancelling future 3")
//    }


//    futCallable flatMap { callable =>
//      val resultValue: NFNValue = callable.exec
//      val futResultData = resultDataOrRedirect(resultValue.toDataRepresentation, name, ccnServer)
//      futResultData map { resultData =>
//        Content(name.withoutThunkAndIsThunk._1, resultData, MetaInfo.empty)
//      }
//    } onComplete {
//      case Success(content) => {
//        logger.info(s"Finished computation, result: $content")
//        senderCopy ! content
//      }
//      case Failure(ex) => {
//        logger.error(ex, s"Error when executing the service $name. Cause: ${ex.getCause} Message: ${ex.getMessage}")
//      }
//    }
  }

  override def receive: Actor.Receive = {
    case ComputeServer.Thunk(name) => {
      prepareCallable(name, useThunks = true, sender)
    }
    case msg @ ComputeServer.Compute(name) => {
      logger.debug(s"started normal computation")
      val senderCopy = sender
      maybeFutCallable match {
        case Some(futCallable) => {
          executeCallable(futCallable, name, senderCopy)
        }
        case None =>
          // Compute request was sent directly without a Thunk message
          // This means we can prepare the callable by directly invoking receivedComputeRequest
          prepareCallable(name, useThunks = false, senderCopy) match {
            case Some(futCallable) => {
              executeCallable(futCallable, name, senderCopy)
            }
            case None => logger.warning(s"Could not prepare a callable for name $name")
          }
      }
    }

      //Added by Johannes
    case msg @ ComputeServer.ComputeDataStream(name: CCNName,additionalArguments: Seq[NFNValue])=>{
      logger.debug(s"started computation with datastream")
      val senderCopy = sender
      maybeFutCallable match {
        case Some(futCallable) => {
          executeCallable(futCallable, name, senderCopy,additionalArguments)
        }
        case None =>
          // Compute request was sent directly without a Thunk message
          // This means we can prepare the callable by directly invoking receivedComputeRequest
          prepareCallable(name, useThunks = false, senderCopy) match {
            case Some(futCallable) => {
              executeCallable(futCallable, name, senderCopy, additionalArguments)
            }
            case None => logger.warning(s"Could not prepare a callable for name $name")
          }
      }
    }
    case ComputeWorker.Cancel(name) => {
      logger.error("ComputeWorker.Cancel received")
      futures(name).cancel()
      futures -= name
    }
    case ComputeWorker.End() => {
      logger.info("Received End message")
      context.stop(self)
//      self ! PoisonPill
    }

    case _ => {
      logger.info("Could not resolve the Compute Message sent to the Comppute Worker")
    }
  }
}
