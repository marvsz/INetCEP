package filterAccess.service.permission

import INetCEP.StatesSingleton
import filterAccess.service.Channel
import nfn.service._
import akka.actor.ActorRef
import ccn.packet.CCNName
import filterAccess.tools.Exceptions._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
 * Created by Claudio Marxer <marxer@claudio.li>
 *
 * Used implement classes to set up the services for the permission channel (storage as well as proxy).
 *
 */
abstract class PermissionChannel extends Channel {

  /**
   *
   * This function is called by entry point of this service to handle the actual work.
   *
   * @param    rdn       Relative data name
   * @param    ccnApi    Akka Actor
   * @return             JSON Object
   */
  def processPermissionChannel(rdn: String, ccnApi: ActorRef): Option[String]


  /** Pin this service */
  override def pinned: Boolean = false

  /**
   * Entry point of this service.
   *
   * @param    args     Function arguments
   * @param    ccnApi   Akka Actor
   * @return            Functions result
   */
  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future {

    args match {
      case Seq(NFNStringValue(rdn)) =>
        processPermissionChannel(rdn, ccnApi) match {
          case Some(res) => NFNStringValue(res)
          case None => throw new noReturnException("No return. Possibly caused by: Data not found")
        }

      case _ =>
        throw new NFNServiceArgumentException(s"PermissionChannel: Argument mismatch.")
    }

  }

}
