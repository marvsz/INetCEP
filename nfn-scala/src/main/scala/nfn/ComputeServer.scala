package nfn

import SACEPICN.StatesSingleton
import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.event.Logging
import ccn.packet.{CCNName, Content}
import nfn.service.NFNValue

import scala.concurrent.duration._
import scala.language.postfixOps

object ComputeServer {

  case class Compute(name: CCNName)

  case class ComputeDataStream(name: CCNName, additionalArguments: Seq[NFNValue])

  case class Thunk(name: CCNName)

  case class RequestToComputation(name: CCNName, senderCopy: ActorRef)

  /**
   * Message to finish computation on compute server
   * @param name
   */
  case class ComputationFinished(name: CCNName)

  case class EndComputation(name: CCNName)
}

case class ComputeServer(nodePrefix: CCNName) extends Actor {
  val logger = Logging(context.system, this)
  val stateHolder = StatesSingleton.getInstance()

  private def createComputeWorker(name: CCNName, ccnServer: ActorRef, nodePrefix: CCNName, stateHolder: StatesSingleton): ActorRef =
    context.actorOf(Props(classOf[ComputeWorker], ccnServer, nodePrefix: CCNName, stateHolder), s"ComputeWorker-${name.hashCode}")

  var computeWorkers = Map[CCNName, ActorRef]()

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 0, withinTimeRange = 1 minute) {
      case _: ArithmeticException      => Stop
      case _: NullPointerException     => Stop
      case _: IllegalArgumentException => Stop
      case _: Exception                => Stop
    }

  override def receive: Actor.Receive = {
    /**
     * Check if computation is already running, if not start a new computation, otherwise do nothing
     */
    case computeMsg @ ComputeServer.Thunk(name: CCNName) => {
      if(name.isCompute) {
        val nameWithoutThunk = name.withoutThunk
        if(!computeWorkers.contains(nameWithoutThunk)) {
          logger.info(s"Started new computation with thunks for $nameWithoutThunk")
          val computeWorker = createComputeWorker(nameWithoutThunk, sender, nodePrefix, stateHolder)
          computeWorkers += nameWithoutThunk -> computeWorker
          computeWorker.tell(computeMsg, sender)
        } else {
          logger.debug(s"Computation for $name is already running")
        }
      } else {
        logger.error(s"Interest was not a compute interest, discarding it")
      }
    }

    case computeMsg @ ComputeServer.Compute(name: CCNName) => {
      if(!name.isThunk) {
        computeWorkers.get(name) match {
          case Some(worker) => {
            logger.info(s"Received Compute for $name, forwarding it to running compute worker")
            worker.tell(computeMsg, sender)
          }
          case None => {
            logger.info(s"Started new computation without thunks for $name")
            val computeWorker = createComputeWorker(name, sender, nodePrefix, stateHolder)
            computeWorkers += name -> computeWorker

            // forward the compute message to the newly created compute worker
            computeWorker.tell(computeMsg, sender)
          }
        }
      }
      else {
        logger.error(s"Compute message must contain the name of the final interest and not a thunk interest: $name")
      }
    }

    case cds @ ComputeServer.ComputeDataStream(name: CCNName, additionalArguments: Seq[NFNValue]) =>{
      if(!name.isThunk) {
        computeWorkers.get(name) match {
          case Some(worker) => {
            logger.info(s"Received Compute for $name with additionalArguments ${additionalArguments.toString()}, forwarding it to running compute worker")
            worker.tell(cds, sender)
          }
          case None => {
            logger.info(s"Started new computation without thunks for $name with additionalArguments ${additionalArguments.toString()}")
            val computeWorker = createComputeWorker(name, sender, nodePrefix, stateHolder)
            computeWorkers += name -> computeWorker

            // forward the compute message to the newly created compute worker
            computeWorker.tell(cds, sender)
          }
        }
      }
      else {
        logger.error(s"Compute message must contain the name of the final interest and not a thunk interest: $name")
      }
    }

    case rtc @ ComputeServer.RequestToComputation(name: CCNName, senderCopy: ActorRef) => {
      if (name.isRequest) {
        logger.debug(s"Received request (${name.requestType}) for $name.")
        name.requestType match {
          case "KEEPALIVE" => {
            logger.debug(s"Matched KA")
          }
          case "CANCEL" => {
            logger.debug(s"Matched CANCEL")
            val computeName = name.withoutNFN.withoutRequest.withNFN
            computeWorkers.get(computeName) match {
              case Some(computeWorker) => {
                logger.debug(s"Found matching compute worker.")
                computeWorker.tell(ComputeWorker.Cancel(computeName), sender)
                computeWorkers -= computeName
                senderCopy ! Content(name, " ".getBytes) // empty response signifies successful cancellation
//                val workers = computeWorkers(computeName)
//                logger.debug(s"Remaining workers for $computeName: $workers")
              }
              case None => logger.warning(s"Received CANCEL request for computation which does not exist: $name")
            }
          }
          case _ => {
            logger.debug(s"Unknown request type ${name.requestType}.")
          }
        }
      }
    }

    case nack @ ComputeServer.EndComputation(name) => {
      computeWorkers.get(name) match {
        case Some(computeWorker) => {
          computeWorker ! ComputeWorker.End
        }
        case None => logger.warning(s"Received nack for computation which does not exist: $name")
      }

    }

//    case ComputeServer.ComputationFinished(name) => {
//      computeWorkers.get(name) match {
//        case Some(computeWorkerToRemove) => {
//          computeWorkerToRemove ! PoisonPill
//          computeWorkers -= name
//        }
//        case None => logger.warning(s"Received ComputationFinished for $name, but computation doesn't exist anymore")
//      }
//    }
  }
}