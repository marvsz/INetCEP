package nfn

import ccn.packet.CCNName
import akka.actor.{ActorContext, ActorRef}
import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable.Set
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

  val pit = mutable.Map[CCNName, Set[(ActorRef,Boolean)]]()



  def add(name: CCNName, isPersistent: Boolean, face: ActorRef, timeout: FiniteDuration) = {
    pit += name -> (pit.getOrElse(name, Set()).union(Set((face,isPersistent))))
  }

  def get(name: CCNName): Option[Set[ActorRef]] = pit.get(name).map(_.map(_._1))

  def remove(name: CCNName, pendingFace: ActorRef): Option[Set[ActorRef]] = {
    pit.get(name).get -= ((pendingFace, false))
    if (pit.get(name).get.isEmpty)
      pit.remove(name) match {
        case Some(value) =>
          logger.debug("Removed from the pit\n")
          Option.apply(value).map(_.map(_._1))
        case _ =>
          logger.error(s"Could not remove from the pit. Stacktrace: " + Thread.currentThread().getStackTrace().toString)
          None
      }
    else
    logger.debug("Did not remove since there were still persistent entries")
      None
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
