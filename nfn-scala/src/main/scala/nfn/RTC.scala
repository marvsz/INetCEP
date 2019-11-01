package nfn

import ccn.packet.CCNName
import com.typesafe.scalalogging.LazyLogging

// Request to computation
case class RTC(computeName: CCNName, request: String, params: List[String]) extends LazyLogging {

}
