package monitor

import ccn.packet.Interest
import monitor.Monitor.{ContentInfoLog, InterestInfoLog, NodeLog, PacketLog}

/**
 * Created by basil on 17/04/14.
 */
object TestApp extends App {

  val starttime = System.nanoTime

  Thread.sleep(5)
  val nodes =
    Seq(
      NodeLog("127.0.0.1", 1, Some("docRepo1"), Some("ComputeNode")),
      NodeLog("127.0.0.1", 2, Some("docRepo2"), Some("ComputeNode")),
      NodeLog("127.0.0.1", 3, Some("docRepo3"), Some("NFNNode")),
      NodeLog("127.0.0.1", 4, Some("docRepo4"), Some("NFNNode"))
    )

  val edges = Set(
    (nodes(0), nodes(1)),
    (nodes(1), nodes(0)),
    (nodes(1), nodes(2)),
    (nodes(2), nodes(3)),
    (nodes(3), nodes(0)),
    (nodes(0), nodes(2)),
    (nodes(2), nodes(1))
  )

  val interest = Interest("name")

  val packets = Set(
    new PacketLog(nodes(0), nodes(1), isSent = true, InterestInfoLog("interst", "/interest/name")),
    new PacketLog(nodes(0), nodes(1), isSent = false, InterestInfoLog("interst", "/interest/name")),
    new PacketLog(nodes(1), nodes(0), isSent = true, ContentInfoLog("content", "/interest/name", "testcontent")),
    new PacketLog(nodes(1), nodes(0), isSent = false, ContentInfoLog("content", "/interest/name", "testcontent"))
  )

  OmnetIntegration(nodes.toSet, edges, packets, starttime)()
}
