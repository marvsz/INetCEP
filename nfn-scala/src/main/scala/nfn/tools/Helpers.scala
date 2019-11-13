package nfn.tools

import java.io.{File, FileOutputStream, PrintWriter}
import java.util.Calendar

import SACEPICN.NodeMapping
import akka.actor.ActorRef
import ccn.packet._
import config.StaticConfig
import nfn.NFNApi
import nfn.service.LogMessage
import nfn.tools.Networking.{fetchContent, fetchContentRepeatedly}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.sys.process._

/**
 * Created by blacksheeep on 17/11/15.
 * Updated by Ali on 22/09/2018
 * Updated by Johannes on 31/01/2019
 */
object Helpers {
  val sacepicnEnv: String = StaticConfig.systemPath



  @deprecated("Was used for the old representatino of the querystore. use the new one instead since we also store the placement algorithm in it", "4.11.2018")
  def save_to_QueryStore(runID: String, sourceOfQuery: String, interestOrigin: String, clientID: String, query: String, region: String, timestamp: String): Boolean = {
    //Source is not QueryStore and DecentralizeQuery
    if (sourceOfQuery != "QS" && sourceOfQuery != "DQ") {
      var filename = s"$sacepicnEnv/nodeData/queryStore"
      val file = new File(filename)
      file.getParentFile.mkdirs()
      file.createNewFile()

      //clear the file for old queries
      var w = new PrintWriter(file)
      w.close()

      val pw = new PrintWriter(new FileOutputStream(file, true))

      var now = Calendar.getInstance()
      var q_TimeStamp = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)

      var queryToStore = s"QID:${clientID}_$q_TimeStamp $runID $interestOrigin $clientID $query $region $timestamp"
      pw.println(queryToStore)
      pw.close()

      return true
    }

    false
  }


  //Overloaded method for QS Storage. The queryService.sh script will use this version and will read queries as per this signature. (11/03/18)
  /**
   * Stores the initial query in the query store
   *
   * @param algorithm      the algorithm of the placement service
   * @param runID          the id of the run (the one given in the publish remotely script I guess)
   * @param sourceOfQuery  the client on which the query runs
   * @param interestOrigin not used
   * @param clientID       the id of the client
   * @param query          the actual query
   * @param region         the region given in the query
   * @param timestamp      the timestamp given in the query
   * @return true if the source of the query was not the queryStore (so we do not flood it) or a decentralized query, false otherwise
   */
  def save_to_QueryStore(algorithm: String, runID: String, sourceOfQuery: String, interestOrigin: String, clientID: String, query: String, region: String, timestamp: String): Boolean = {
    //Source is not QueryStore and DecentralizeQuery
    if (sourceOfQuery != "QS" && sourceOfQuery != "DQ") {
      var filename = s"$sacepicnEnv/nodeData/queryStore"
      val file = new File(filename)
      file.getParentFile.mkdirs()
      file.createNewFile()

      //clear the file with old queries
      var w = new PrintWriter(file)
      w.close()

      val pw = new PrintWriter(new FileOutputStream(file, true))

      var now = Calendar.getInstance()
      var q_TimeStamp = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)

      var queryToStore = s"QID:${clientID}_$q_TimeStamp $algorithm $runID $sourceOfQuery $clientID $query $region $timestamp"
      pw.println(queryToStore)
      pw.close()

      return true
    }

    false
  }

  //Overloaded method for QS Storage. The queryService.sh script will use this version and will read queries as per this signature. (11/03/18)
  /**
   * Stores the initial query in the query store
   *
   * @param algorithm      the algorithm of the placement service
   * @param processing     processing type: centralized or decentralized
   * @param runID          the id of the run (the one given in the publish remotely script I guess)
   * @param sourceOfQuery  the client on which the query runs
   * @param interestOrigin not used
   * @param clientID       the id of the client
   * @param query          the actual query
   * @param region         the region given in the query
   * @param timestamp      the timestamp given in the query
   * @return true if the source of the query was not the queryStore (so we do not flood it) or a decentralized query, false otherwise
   */
  def save_to_QueryStore(algorithm: String, processing: String, runID: String, sourceOfQuery: String, interestOrigin: String, clientID: String, query: String, region: String, timestamp: String): Boolean = {
    //Source is not QueryStore and DecentralizeQuery
    if (sourceOfQuery != "QS" && sourceOfQuery != "DQ") {
      var filename = s"$sacepicnEnv/nodeData/queryStore"
      val file = new File(filename)
      file.getParentFile.mkdirs()
      file.createNewFile()

      //clear the file with old queries
      var w = new PrintWriter(file)
      w.close()

      val pw = new PrintWriter(new FileOutputStream(file, true))

      var now = Calendar.getInstance()
      var q_TimeStamp = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)

      var queryToStore = s"QID:${clientID}_$q_TimeStamp $algorithm $processing $runID $sourceOfQuery $clientID $query $region $timestamp"
      pw.println(queryToStore)
      pw.close()

      return true
    }

    false
  }



  /**
   * @deprecated not used anymore
   *             Returns the path for the node Information file
   * @return the path for the node Information file
   */
  def getNodeInformationPath: String = {
    s"$sacepicnEnv/nodeData/nodeInformation"
  }

  /**
   * Returns the path for the decentralized k-hop value file
   *
   * @return the path for the decentralilzed k-hop value file
   */
  def getDecentralizedKHops: String = {
    s"$sacepicnEnv/nodeData/Decentralized_KHop"
  }

  /**
   * @deprecated not used anymore since
   *             Returns the decentralized nodeinformation for the given node name
   * @param nodeName the name of the node we want to have the decentralized node information for
   * @return the decentralized node information for the given node name
   */
  def getDecentralizedNodeInformation(nodeName: String): String = {
    s"$sacepicnEnv/nodeData/$nodeName"
  }

  /**
   * Was used to create a CCNQuery and the execute it on the command line
   *
   * @param nodeName the nodename that executes the command, used for debugging purposes only
   * @param query    the query to execute
   * @param port     the port of the remote node
   * @param IP       the ip address of the remote node
   * @return the result of the execution
   */
  @deprecated("Was used to bypass the face but is not used anymore. Use executeNFNQuery instead.", "02.02.2019")
  def createAndExecCCNQuery(nodeName: String, query: String, port: String, IP: String): String = {
    //var cmd:String = getValueOrDefault("CCN.NFNPeek", "echo No Result!")

    var cmd = """$CCNL_HOME/bin/ccn-lite-peek -s ndn2013 -u #IP#/#PORT# -w 30 "" "#QUERY#/NFN""""
    var cmdPacketFormatter = "$CCNL_HOME/bin/ccn-lite-pktdump -f 2"
    //Replace IP PORT and QUERY
    //With this we can run the remote queries on the remote nodes:
    cmd = cmd.replace("#IP#", s"${IP}").replace("#PORT#", s"${port}").replace("#QUERY#", query);
    LogMessage(nodeName, s"Query sent to network node: ${cmd} | ${cmdPacketFormatter}");
    var result = execcmd(cmd, cmdPacketFormatter)
    LogMessage(nodeName, s"Query Result from network node: ${result}");

    if (result.contains("timeout") || result.contains("interest") || result == "")
      result = "No Result!"
    return result.trim().stripLineEnd;
  }

  @deprecated("Was used to execute a comamnd in the command line. Not used anymore because we now use other ways to get data.", "02.02.2019")
  def execcmd(cmd1: String, cmd2: String): String = {
    val result = Seq("/bin/sh", "-c", s"${cmd1} | ${cmd2}").!!
    return result
  }

  /**
   * Not used at the moment
   * Is responsible to resolve a redirect and fetch larger content
   *
   * @param nodeName the nodename that executes the command, used for debugging purposes only
   * @param query    the query to execute
   * @param port     the port of the remote node
   * @param IP       the ip address of the remote node
   * @return the result of the execution
   */
  def createAndExecCCNQueryForRedirect(nodeName: String, query: String, port: String, IP: String): String = {
    var cmd = """$CCNL_HOME/bin/ccn-lite-fetch -s ndn2013 -u #IP#/#PORT# "#QUERY#""""
    cmd = cmd.replace("#IP#", s"${IP}").replace("#PORT#", s"${port}").replace("#QUERY#", query);
    LogMessage(nodeName, s"Query sent to network node: ${cmd}")
    var result = execcmdwithoutformatter(cmd)
    LogMessage(nodeName, s"Query Result form network node: ${result}")
    if (result.contains("timeout") || result.contains("interest") || result == "")
      result = "No Result!"
    return result.trim().stripLineEnd
  }

  /**
   * Not used at the moment
   * Exevutes a single command
   *
   * @param cmd1 the command to execute
   * @return the result of the executed commaned
   */
  def execcmdwithoutformatter(cmd1: String): String = {
    val result = cmd1.!!
    return result
  }


  /**
   * Handles a named interest which can be either a nested query or a named address. Resolves either and returns the wanted data
   *
   * @param nodeName The name of the node that has to handle the nfn call
   * @param stream   The stream of data that is to be handled
   * @param ccnApi   The actorRef of the caller
   * @return A string containing the resulting data
   */
  def handleNamedInputSource(nodeName: String, stream: String, ccnApi: ActorRef) = {
    var intermediateResult = ""
    if (stream.contains("[") && stream.contains("]")) {
      //This means, filter contains RAW window query (named-function): first resolve the window query:
      val interest = stream.replace("[", "").replace("]", "").replace("{", "\'").replace("}", "\'").replace("(", "").replace(")", "")
      LogMessage(nodeName, s"Intermediate Named-Function is: ${interest}")
      intermediateResult = Helpers.executeNFNQueryRepeatedly(interest, nodeName, ccnApi, 25)
      LogMessage(nodeName, s"Intermediate Named-Function Result is: ${intermediateResult}")

      if (intermediateResult.contains("/") && intermediateResult.split("/").length > 1) {
        intermediateResult = executeInterestQuery(intermediateResult, nodeName, ccnApi)
      }
    }
    else {
      //This means that a named-interest was passed containing the intermediate window result. Just get it
      intermediateResult = fetchFromRemoteNode(nodeName, stream, ccnApi)
      LogMessage(nodeName, s"Intermediate Result: $intermediateResult")
    }
    if (intermediateResult.contains("redirect")) {
      LogMessage(nodeName, "Intermediate result contained redirect, Not trying to resolve it - does not work.")
      /*val str = intermediateResult.replace("\n","").trim
      LogMessage(nodeName, s"trimmed and stripped intermediateResult: ${str}")
      val rname = CCNName(str.splitAt(9)._2.split("/").toList.tail.map(_.replace("%2F", "/").replace("%2f", "/")), None)
      LogMessage(nodeName, s"new call is: ${rname}")
      val interest = new Interest(stream)
      LogMessage(nodeName, s"look up interest $interest")
      intermediateResult = executeInterestQuery(stream,nodeName,ccnApi)*/
      LogMessage(nodeName, s"unresolved redirect: ${intermediateResult}")
    }
    intermediateResult
  }

  /**
   * Hier müsste man mit einem Future ansetzen um nebenläufigkeit zu erhalten
   * @param query
   * @param nodeName
   * @param ccnApi
   * @return
   */
  def executeInterestQuery(query: String, nodeName: String, ccnApi: ActorRef): String = {
    val nameOfContentWithoutPrefixToAdd = CCNName(new String(query).split("/").tail.toIndexedSeq: _*)
    LogMessage(nodeName, s"execute Interest query ${query} called with ${nodeName}")
    var result = new String(fetchContentRepeatedly(
      Interest(nameOfContentWithoutPrefixToAdd),
      ccnApi,
      15 seconds).get.data)
    LogMessage(nodeName, s"Query Result from network node: ${result}")
    if (result.contains("timeout") || result.contains("interest") || result == "")
      result = "No Result!"
    return result
  }

  /**
   *
   * @param nodeName The current nodename that wants to fetch the data (for debugging pruposes only)
   * @param address  the address/named interest to get
   * @param ccnApi   the actorRef of the current node
   * @return A string with the result
   */
  def fetchFromRemoteNode(nodeName: String, address: String, ccnApi: ActorRef) = {
    val mapping = new NodeMapping()
    LogMessage(nodeName, s"Intermediate Named-Interest is: ${address}")
    val intermediateResult = Helpers.executeNFNQueryRepeatedly(s"(call 2 /node/${mapping.getName(address.split("/")(1))}/nfn_service_GetContent '${address}')", mapping.getName(address.split("/")(1)), ccnApi, 20)
    LogMessage(nodeName, s"Result after fetching: ${intermediateResult}")
    intermediateResult
  }

  /**
   *
   * @param query    the query to execute
   * @param nodeName the nodename for debugging purposes
   * @param ccnApi   the actorRef
   * @return the result of the execution of the NFNQuery
   */
  def executeNFNQueryRepeatedly(query: String, nodeName: String, ccnApi: ActorRef, timeoutAfter: Int): String = {

    LogMessage(nodeName, s"execute NFN query ${query} repeatedly called with ${nodeName}")
    var result = new String(fetchContentRepeatedly(
      NFNInterest(query), ccnApi,
      timeoutAfter seconds).get.data)
    LogMessage(nodeName, s"Query Result from network node: ${result}");

    if (result.contains("timeout") || result.contains("interest") || result == "")
      result = "No Result!"
    return result
  }

  def executeNFNQuery(query: String, nodeName: String, ccnApi: ActorRef, timeoutAfter: Int):String = {
    LogMessage(nodeName, s"execute NFN query ${query} called with ${nodeName}")
    val intermediate: Option[Content] = fetchContent(
      NFNInterest(query), ccnApi,
      timeoutAfter seconds)
    var result = ""
    if(intermediate != None)
      result = new String(intermediate.get.data)
    LogMessage(nodeName, s"Query Result from network node: ${result}");
    if (result.contains("timeout") || result.contains("interest") || result == "")
      result = "No Result!"
    result
  }

  /**
   * Stores content in the cache of a node
   *
   * @param nodeName  The name of the node
   * @param input     The content to store
   * @param ccnApi    The Actor ref
   * @param operation The operation that took place
   * @param onWhat    On what the opeartion was performed on
   * @return An named address that indicates where to find the content
   */
  def storeOutputLocally(nodeName: String, input: String, operation: String, onWhat: String, ccnApi: ActorRef) = {
    //Return a name that contains the window data. This is a cached content.
    val now = Calendar.getInstance()
    //Get the correct mapped port for this node so that we can publish content through this..
    val prefixOfNode = new NodeMapping().getPort(nodeName)
    LogMessage(nodeName, s"${operation} OP: Prefix of node is: " + prefixOfNode)
    //This will be something like /900X/operation/onWhat/....
    val name = s"/$prefixOfNode/${operation}/${onWhat}/" + now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) + now.get(Calendar.SECOND)

    val nameOfContentWithoutPrefixToAdd = CCNName(new String(name).split("/").tail.toIndexedSeq: _*)
    ccnApi ! NFNApi.AddToLocalCache(Content(nameOfContentWithoutPrefixToAdd, input.getBytes, MetaInfo.empty), prependLocalPrefix = false)
    LogMessage(nodeName, s"Inside ${operation} -> ${operation} name: ${name}, ${operation} content: ${input}")
    name
  }

  def storeState(nodeName: String, input: String, operator: String, settings:String, ccnApi: ActorRef)={
    val name = s"state/$operator/$settings"
    val nameOfContentWithoutPrefixToAdd = CCNName(new String(name).split("/").toIndexedSeq: _*)
    LogMessage(nodeName, s"State for $name saved to Network")
    ccnApi ! NFNApi.AddToCCNCache(Content(nameOfContentWithoutPrefixToAdd, input.getBytes, MetaInfo.empty))
  }


}
