package nfn.service.NBody

import SACEPICN.StatesSingleton
import akka.actor._
import ccn.packet.CCNName
import nfn.service.{NFNStringValue, _}
import nfn.tools.Networking._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SimulationService extends NFNService {

  override def function(interestName: CCNName, argSeq: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future {
    // /path/to/NBodySimulation [/path/to/config] ['-c' <configSize>] ['-d' <deltaTime>] ['-s' <stepCount>] ['-i' <intermediateInterval>]

    var options = Map(Symbol("configSize") -> 100, Symbol("deltaTime") -> 10, Symbol("stepCount") -> 1000000, Symbol("intermediateInterval") -> 1000)
    var configuration = Array[Byte]()

    var args = argSeq.toList
    while (args.nonEmpty) args match {
      case NFNStringValue("-c") :: NFNIntValue(value) :: tail => options += (Symbol("configSize") -> value); args = args drop 2
      case NFNStringValue("-d") :: NFNIntValue(value) :: tail => options += (Symbol("deltaTime") -> value); args = args drop 2
      case NFNStringValue("-s") :: NFNIntValue(value) :: tail => options += (Symbol("stepCount") -> value); args = args drop 2
      case NFNStringValue("-i") :: NFNIntValue(value) :: tail => options += (Symbol("intermediateInterval") -> value); args = args drop 2
      case NFNContentObjectValue(name, data) :: tail => configuration = data; args = args drop 1
      case _ => args = List()
    }

    val configSize = options(Symbol("configSize"))
    val deltaTime = options(Symbol("deltaTime"))
    val stepCount = options(Symbol("stepCount"))
    val intermediateInterval = options(Symbol("intermediateInterval"))

    println("intermediateInterval: " + intermediateInterval)

    val systemSize = Vector(Body.earth.radius * 500, Body.earth.radius * 500)
    val renderArea = Rect(-systemSize / 2, systemSize)
    val config = if (configuration.length <= 0) Config.random(renderArea, configSize)
      else Config.fromString(configuration.toString)

    val simulation = new Simulation(config, deltaTime)

    var lastTime = System.currentTimeMillis()
    var intermediateIndex = 0


//    intermediateResult(ccnApi, interestName, 0, NFNStringValue(simulation.config.toString))

    simulation.run(stepCount, step => {
      println(s"Step: $step")
      val currentTime = System.currentTimeMillis()
      val elapsed = currentTime - lastTime
      if (elapsed > intermediateInterval && intermediateInterval > 0) {
        intermediateResult(ccnApi, interestName, intermediateIndex, NFNStringValue(simulation.config.toString))
        intermediateIndex += 1
        lastTime = currentTime
      }
//      Thread.sleep(1000)
    })
    NFNStringValue(simulation.config.toString)

//    NFNIntValue(7)

  }
}

