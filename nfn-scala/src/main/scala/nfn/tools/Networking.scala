package nfn.tools

import java.util.concurrent.TimeoutException

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import ccn.packet.{CCNName, Content, Interest, MetaInfo, NFNInterest, PersistentInterest}
import com.typesafe.scalalogging.LazyLogging
import nfn.NFNApi
import nfn.service._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}



/**
  * Created by blacksheeep on 16/11/15.
  */
object Networking extends LazyLogging{


  /**
    * Try to resolve Redirect by given data.
    *
    * @param    data       data to redirect
    * @param    ccnApi     Actor Reference
    * @param    time       Timeout
    * @return              resloved data
    */
  def resolveRedirect(data: Array[Byte], ccnApi: ActorRef, time: Duration): Option[Array[Byte]] = {

    var str = new String(data)

    if(str.contains("redirect")) {
      str = str.replace("\n", "").trim
      val rname = CCNName(str.splitAt(9)._2.split("/").toList.tail.map(_.replace("%2F", "/").replace("%2f", "/")), None)


      val interest = new Interest(rname)
      val content = fetchContent(interest, ccnApi, time).get
      return Some(content.data)
    }
    return Some(data)
  }

  /**
   *
   * @param persistentInterest the constant interest
   * @param interestedComputation the computation that is interested in the data
   * @param ccnApi the actor ref
   */
  def makePersistentInterest(persistentInterest: String, interestedComputation: CCNName, ccnApi: ActorRef): Unit ={

    makePersistentInterest(CCNName(new String(persistentInterest).split("/").toIndexedSeq: _*), interestedComputation, ccnApi)
  }

  def makePersistentInterest(persistentInterest: CCNName, interestedComputation:CCNName, ccnApi: ActorRef): Unit = {
    makePersistentInterest(PersistentInterest(persistentInterest), interestedComputation, ccnApi)
  }

  def makePersistentInterest(persistentInterest: PersistentInterest, interestedComputation:CCNName, ccnApi: ActorRef): Unit = {
    ccnApi ! NFNApi.CCNSendPersistentInterest(persistentInterest, (interestedComputation.prepend("COMPUTE")).append("NFN"), useThunks = false)
  }

  def subscribeToQuery(interestName: String, interestedComputation: String, ccnApi:ActorRef): Unit = {
    val interestedComputationName = NFNInterest(interestedComputation).name.prepend("COMPUTE")
      //CCNName(new String(interestedComputation).split("/").toIndexedSeq: _*).prepend("COMPUTE").append("NFN")
    if(!interestName.contains("call") && !interestName.contains("window")){
      ccnApi ! makePersistentInterest(interestName,interestedComputationName,ccnApi)
    }
    else{
      val interestComputationName = NFNInterest(interestName).name//.prepend("COMPUTE")
        //CCNName(new String(interestName).split("/").toIndexedSeq: _*).prepend("COMPUTE").append("NFN")
      ccnApi ! NFNApi.CCNSendPersistentInterest(PersistentInterest(interestComputationName), interestedComputationName, useThunks = false)
    }

  }

  def startService(i:Interest, ccnApi:ActorRef)={
    ccnApi ! NFNApi.CCNStartService(i,useThunks = false)
  }

  /**
    * Try to fetch content object by given interest.
    *
    * @param    interest   Interest to send out
    * @param    ccnApi     Actor Reference
    * @param    time       Timeout
    * @return              Content Object (on success)
    */
  def fetchContent(interest: Interest, ccnApi: ActorRef, time: Duration): Option[Content]  = {
    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(time.toMillis,MILLISECONDS)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false,true)).mapTo[Content]
    }

    // try to fetch data and return if successful
    try {
      val futServiceContent: Future[Content] = loadFromCacheOrNetwork(interest)
      Await.result(futServiceContent, time) match {
        case c: Content => Some(c)
        case _ => None  // send keepalive interest
      }
    } catch {
      case e: TimeoutException => logger.error("fetchContent timed out."); None
    }
  }

  def fetchContentRepeatedly(interest: Interest, ccnApi: ActorRef, time: Duration): Option[Content] = {
    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(time.toMillis,MILLISECONDS)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false,true)).mapTo[Content]
    }

    while (true) {
      val futContent = loadFromCacheOrNetwork(interest)
      try {
        val result = Await.result(futContent, time) match {
          case c: Content => Some(c)
          case _ => None
        }
        if (result.isDefined)
          return result
      } catch {
        case e: TimeoutException => logger.error(s"timeout for interest ${interest.toString}, retry")
      }
    }
    None
  }

  def fetchLocalContent(interest: Interest, ccnApi: ActorRef, time: Duration): Option[Content] = {
    // try to fetch local data and return if successful
    try {
      implicit val timeout = Timeout(time.toMillis,MILLISECONDS)
      val futMaybeContent = (ccnApi ? NFNApi.GetFromLocalCache(interest)).mapTo[Option[Content]]
      Await.result(futMaybeContent, time)
    } catch {
      case e: TimeoutException => logger.error("fetchLocalContent timed out."); None
    }
  }

  def fetchRequestsToComputation(interest: Interest, ccnApi: ActorRef): Option[CCNName] = {
    val request = Interest(interest.name.withCompute.withRequest.withoutNFN)
    fetchLocalContent(request, ccnApi, 1 second) match {
      case Some(content) => Some(content.name)
      case None => None
    }
  }

  def fetchContentAndKeepAlive(ccnApi: ActorRef,
                               interest: Interest,
                               timeoutDuration: FiniteDuration = 20 seconds,
                               handleIntermediate: Option[(Int, Content) => Unit] = None): Option[Content] = {

    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(timeoutDuration.toMillis,MILLISECONDS)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false,true)).mapTo[Content]
    }

    def keepaliveInterest(interest: Interest): Interest = Interest(interest.name.makeKeepaliveName)

    def fetchIntermediate(index: Int) = {
      logger.debug(s"Request intermediate #$interest")

      val intermediateInterest = Interest(interest.name.withRequest(s"GIM $index")) // CRASH HERE?
      val intermediateFuture = loadFromCacheOrNetwork(intermediateInterest)
      intermediateFuture onComplete {
        case Success(x) => x match {
          case c: Content => {
            logger.debug("Received intermediate content.")
            val handler = handleIntermediate.get
            handler(index, c)
          }
          case _ => logger.debug(s"Matched something else.")
        }
        case Failure(error) => logger.error(s"Completed with error: $error")
      }
    }

    var isFirstTry = true
    var shouldKeepTrying = true
    val shouldGetIntermediates = handleIntermediate.isDefined
    val intermediateInterval = 100

    if (shouldGetIntermediates) {
      val f = Future {
        var highestRequestedIndex = -1
        val countIntermediatesInterest = Interest(interest.name.withRequest("CIM"))

        Thread.sleep(2000)

        while (shouldKeepTrying) {
          val startTime = System.currentTimeMillis()
          val loadFuture = loadFromCacheOrNetwork(countIntermediatesInterest)
          //          val timeoutFuture = Future {
          //            Thread.sleep(1 * 1000)
          //            throw new TimeoutException("Count intermediates timeout")
          //          }
          //          val future = Future.firstCompletedOf(Seq(loadFuture, timeoutFuture))
          //          println("request intermediates 3")

          Await.result(loadFuture, 3 second) match {
            case c: Content =>
              if (c.data.length > 0) {
                val highestAvailableIndex = new String(c.data).toInt
                logger.debug(s"Highest available intermediate: $highestAvailableIndex")
                var index = highestRequestedIndex + 1
                val endIndex = highestAvailableIndex
                while (index <= highestAvailableIndex) {
                  fetchIntermediate(index)
                  index += 1
                }
                highestRequestedIndex = highestAvailableIndex
              } else {
                logger.debug("No intermediate results available (yet?).")
              }
            case _ => logger.error("Loading intermediate content timed out.")
          }
          val elapsed = System.currentTimeMillis() - startTime
          if (elapsed < intermediateInterval) {
            Thread.sleep(intermediateInterval - elapsed)
          }
        }
      }
    }

    logger.debug("Fetch content and keepalive")

    while (shouldKeepTrying) {
      logger.debug("Try again.")
      val futContent = loadFromCacheOrNetwork(interest)
      val futKeepalive = if (isFirstTry) None else Some(loadFromCacheOrNetwork(keepaliveInterest(interest)))

      try {
        val result = Await.result(futContent, timeoutDuration) match {
          case c: Content => logger.debug("content."); Some(c)
          case _ => logger.error("none."); None
        }
        if (result.isDefined)
          return result
      } catch {
        case e: TimeoutException => logger.error("timeout")
      }

      shouldKeepTrying = isFirstTry || futKeepalive.get.value.isDefined
    }
    None
  }

  // TODO: remove this unused method?
  def requestContentAndKeepAlive(ccnApi: ActorRef,
                                 interest: Interest,
                                 timeoutDuration: FiniteDuration = 20 seconds): Future[Option[Content]] = {

    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(timeoutDuration.toMillis,MILLISECONDS)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false,true)).mapTo[Content]
    }

    def blockingRequest(): Option[Content] = {
      var isFirstTry = true
      var shouldKeepTrying = true
      while (shouldKeepTrying) {
        logger.debug("Try again.")
        val futContent = loadFromCacheOrNetwork(interest)
        val futKeepalive = if (isFirstTry) None else Some(loadFromCacheOrNetwork(Interest(interest.name.makeKeepaliveName)))

        try {
          val result = Await.result(futContent, timeoutDuration) match {
            case c: Content => logger.debug("content."); Some(c)
            case _ => logger.error("none."); None
          }
          if (result.isDefined)
            return result
        } catch {
          case e: TimeoutException => logger.error("timeout")
        }

        shouldKeepTrying = isFirstTry || futKeepalive.get.value.isDefined
      }
      None
    }

    Future { blockingRequest() }
  }

  def requestIntermediates(ccnApi: ActorRef,
                           interest: Interest,
                           timeoutDuration: FiniteDuration = 20 seconds,
                           handleIntermediates: (Content) => Unit): Unit = {

    def loadFromCacheOrNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(timeoutDuration.toMillis,MILLISECONDS)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false,true)).mapTo[Content]
    }

    val f = Future {
      logger.debug("request intermediates 1")
      Thread.sleep(2 * 1000)
      logger.debug("request intermediates 2")
      var highestRequestedIndex = -1
      val countIntermediatesInterest = Interest(interest.name.withRequest("CIM"))
      val loadFuture = loadFromCacheOrNetwork(countIntermediatesInterest)
      val timeoutFuture = Future {
        Thread.sleep(3 * 1000)
        throw new TimeoutException("Future timeout")
      }
      val future = Future.firstCompletedOf(Seq(loadFuture, timeoutFuture))
      logger.debug("request intermediates 3")

      future onComplete {
        case Success(x) => x match {
          case c: Content => {
            val highestAvailableIndex = new String(c.data).toInt
            logger.debug(s"Highest available intermediate: $highestAvailableIndex")
          }
          case _ => logger.debug(s"Matched something else.")
        }
        case Failure(error) => logger.error(s"Completed with error: $error")
      }
    }
  }

  def storeResult(nodeName: String, content: String, contentName: String, ccnApi: ActorRef)={
    val nameOfContentWithoutPrefixToAdd = CCNName(new String(contentName).split("/").toIndexedSeq: _*)
    LogMessage(nodeName, s"Content for $contentName saved to Network")
    ccnApi ! NFNApi.AddDataStreamToCCNCache(Content(nameOfContentWithoutPrefixToAdd, content.getBytes, MetaInfo.empty))
  }

  /**
    * Try to fetch content object by name.
    *
    * @param    name       Name
    * @param    ccnApi     Actor
    * @param    time       Timeout
    * @return              Content Object (on success)
    */
  def fetchContentFromNetwork(name: String, ccnApi: ActorRef, time: Duration): Option[Content] = {
    val i = Interest(CCNName(name.split("/").toIndexedSeq: _*))
    fetchContentFromNetwork(i, ccnApi, time)
  }

  /**
   * Try to fetch content object by given interest.
   *
   * @param    interest   Interest to send out
   * @param    ccnApi     Actor Reference
   * @param    time       Timeout
   * @return              Content Object (on success)
   */
  def fetchContentFromNetwork(interest: Interest, ccnApi: ActorRef, time: Duration): Option[Content]  = {
    def loadFromNetwork(interest: Interest): Future[Content] = {
      implicit val timeout = Timeout(time.toMillis,MILLISECONDS)
      (ccnApi ? NFNApi.CCNSendReceive(interest, useThunks = false,false)).mapTo[Content]
    }

    // try to fetch data and return if successful
    try {
      val futServiceContent: Future[Content] = loadFromNetwork(interest)
      Await.result(futServiceContent, time) match {
        case c: Content => Some(c)
        case _ => None  // send keepalive interest
      }
    } catch {
      case e: TimeoutException => logger.error("fetchContent timed out."); None
    }
  }
  //
  //  def intermediateDataOrRedirect(ccnApi: ActorRef, name: CCNName, data: Array[Byte]): Future[Array[Byte]] = {
  //    if(data.size > CCNLiteInterfaceCli.maxChunkSize) {
  //      name.expression match {
  //        case Some(expr) =>
  //          val redirectName = name.cmps
  //          val content = Content(CCNName(redirectName, None), data)
  //          implicit val timeout = StaticConfig.defaultTimeout
  //          ccnApi ? NFNApi.AddToCCNCache(content) map {
  //            case NFNApi.AddToCCNCacheAck(_) =>
  //              val escapedComponents = CCNLiteInterfaceCli.escapeCmps(redirectName)
  //              val redirectResult: String = "redirect:" + escapedComponents.mkString("/", "/", "")
  //              redirectResult.getBytes
  //            case answer @ _ => throw new Exception(s"Asked for addToCache for $content and expected addToCacheAck but received $answer")
  //          }
  //        case None => throw new Exception(s"Name $name could not be transformed to an expression")
  //      }
  //    } else Future(data)
  //  }

  def intermediateResult(ccnApi: ActorRef, name: CCNName, count: Int, resultValue: NFNValue) = {
    var contentName = name
    //    println("Content Name: " + contentName.toString)

    contentName = contentName.append(CCNName.requestKeyword)
    contentName = contentName.append(s"${CCNName.getIntermediateKeyword} ${count.toString}")
    //    contentName = contentName.append(count.toString)
    //    println("Content Name: " + contentName.toString)

    contentName = contentName.withCompute
    //    println("Content Name: " + contentName.toString)

    contentName = contentName.withNFN
    logger.debug("Content Name: " + contentName.toString)



    //    val futIntermediateData = intermediateDataOrRedirect(ccnApi, contentName, resultValue.toDataRepresentation)
    //    futIntermediateData map {
    //      resultData => Content(contentName, resultData, MetaInfo.empty)
    //    } onComplete {
    //      case Success(content) => {
    //        ccnApi ! NFNApi.AddToCCNCache(content)
    //      }
    //      case Failure(ex) => {
    //
    //      }
    //    }

    val content = Content(contentName, resultValue.toDataRepresentation)
    ccnApi ! NFNApi.AddIntermediateResult(content)


    //    ccnApi ! NFNApi.AddToCCNCache(Content(contentName, resultValue.toDataRepresentation))



    //    resultDataOrRedirect(resultValue.toDataRepresentation, name, ccnServer)

    //    futCallable flatMap { callable =>
    //      val resultValue: NFNValue = callable.exec
    //      val futResultData = resultDataOrRedirect(resultValue.toDataRepresentation, name, ccnServer)
    //      futResultData map { resultData =>
    //        Content(name.withoutThunkAndIsThunk._1, resultData, MetaInfo.empty)
    //
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
}
