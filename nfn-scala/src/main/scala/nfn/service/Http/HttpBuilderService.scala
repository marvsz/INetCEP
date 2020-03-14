package nfn.service.Http

import java.net.URLEncoder

import SACEPICN.StatesSingleton
import akka.actor.ActorRef
import ccn.packet.CCNName
import nfn.service.{NFNService, NFNServiceArgumentException, NFNStringValue, NFNValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/** Abstract NFNService class that implements a method to concatenate a sequence of arguments (NFNValue type) into a HTTP query string.
  *
  * Author:  Ralph Gasser
  * Date:    03-05-2015
  * Version: 1.0
  */
abstract class HttpQueryStringBuilderService extends NFNService {
  protected [HttpQueryStringBuilderService] def build(parameters : Seq[NFNValue] ) : String = {
    parameters.grouped(2) map {
      case Seq(NFNStringValue(a), NFNStringValue(b)) => URLEncoder.encode(a, "UTF-8") + "=" + URLEncoder.encode(b, "UTF-8")
      case _ => throw new NFNServiceArgumentException("Number of query parameters is not correct.")
    } mkString("&")
  }
}

/** Simple NFN service which takes a base url followed by a sequence of arguments that are treated as key-value pairs, to build a URL with a HTTP GET query string:
  * * Example: http://example.com?key1=value1&key2=value2
  *
  * Author:  Ralph Gasser
  * Date:    07-03-2015
  * Version: 1.0
  */
class HttpGetQueryStringBuilderService extends HttpQueryStringBuilderService {
  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future {
    args match{
      case Seq(NFNStringValue(baseUrl)) => NFNStringValue(baseUrl)
      case Seq(NFNStringValue(baseUrl), parameters @ _*) => {
        val queryString = build(parameters);
        if (queryString.length() > 0) {
          NFNStringValue(baseUrl + "?" + queryString)
        } else {
          NFNStringValue(baseUrl)
        }
      }
      case _ => throw new NFNServiceArgumentException("The provided signature is not supported. Use <baseUrl> [<query-parameters>] instead.")
    }
  }
}

/** Simple NFN service which takes a sequence of arguments and treats them as key-value pairs to build a HTTP POST query string:
  * Example: key1=value1&key2=value2
 *
  * Author:  Ralph Gasser
  * Date:    02-05-2015
  * Version: 1.0
 */
class HttpPostQueryStringBuilderService extends HttpQueryStringBuilderService {
  /**
   *
   * @param args
   * @param ccnApi
   * @return
   */
  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future(NFNStringValue(build(args)))
}

