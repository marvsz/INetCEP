package nfn

import ccn.packet.CCNName
import akka.actor.{ActorContext, ActorRef}
import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable
import scala.concurrent.duration._

//case class PendingInterest(name: CCNName, faces: List[ActorRef], timeout: Long) extends Ordered[PendingInterest] {
//
//  val startTime = System.nanoTime
//
//  def timer = startTime + (timeout * 1000000)
//
//  override def compare(that: PendingInterest): Int = (this.timer - that.timer).toInt
//}

case class PIT(context: ActorContext) extends LazyLogging {

  private case class Timeout(name: CCNName, face: ActorRef)

  val pit = mutable.Map[CCNName, Set[ActorRef]]()

  val pqt = mutable.Map[CCNName, Set[CCNName]]()

  def add(name: CCNName, face: ActorRef, timeout: FiniteDuration) = {
    pit += name -> (pit.getOrElse(name, Set()) + face)
  }

  def add(name: CCNName, queryName:CCNName, timeout: FiniteDuration)={
    pqt += name ->(pqt.getOrElse(name,Set()) + queryName)
  }

  def get(name: CCNName): Option[Set[ActorRef]] = pit.get(name)

  def getPendingQueries(name: CCNName): Option[Set[CCNName]] = pqt.get(name)

  def remove(name: CCNName): Option[Set[ActorRef]] = {
    pit.remove(name) match {
      case Some(value) =>
        logger.debug("Removed from the pit\n")
        Option.apply(value)
      case _ =>
        logger.error(s"Could not remove from the pit. Stacktrace: " + Thread.currentThread().getStackTrace().toString)
        None
    }
  }

  override def toString() = {
    pit.toString()
  }

  //  def case Timeout(name, face) => {
  //      pit.get(name) map { pendingFaces =>
  //        logger.warning(s"Timing out interest: $name to face $face")
  //        pit += name -> (pendingFaces - face)
  //      }
  //    }
}
