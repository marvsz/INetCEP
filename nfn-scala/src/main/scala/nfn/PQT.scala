package nfn

import akka.actor.{ActorContext, ActorRef}
import ccn.packet.CCNName
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

case class PQT(context: ActorContext) extends LazyLogging{

  private case class Timeout(name:CCNName, face: ActorRef)

  val pqt = mutable.Map[CCNName, Set[CCNName]]()

  def add(name: CCNName, queryName:CCNName, timeout: FiniteDuration)={
    pqt += name ->(pqt.getOrElse(name,Set()) + queryName)
  }

  def get(name: CCNName): Option[Set[CCNName]] = pqt.get(name)

  def remove(name: CCNName, query: CCNName) = {
    get(name).get -= query
    if(get(name).get.isEmpty)
      pqt.remove(name) match {
        case Some(value) =>
          logger.debug("Removed from the pqt\n")
          Option.apply(value)
        case _ =>
          logger.error(s"Could not remove from the pqt. Stacktrace: " + Thread.currentThread().getStackTrace().toString)
          None
      }
  }

  override def toString()={
    pqt.toString()
  }

}
