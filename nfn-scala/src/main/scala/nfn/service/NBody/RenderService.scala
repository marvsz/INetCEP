package nfn.service.NBody

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

import akka.actor.ActorRef
import ccn.packet.CCNName
import javax.imageio.ImageIO
import nfn.service.{NFNStringValue, _}


class RenderService extends NFNService {
  override def function(interestName: CCNName, argSeq: Seq[NFNValue], ccnApi: ActorRef): NFNValue = {


    var options = Map(Symbol("xres") -> 500, Symbol("yres") -> 500)
    var configuration = Array[Byte]()

    var args = argSeq.toList
    while (args.nonEmpty) args match {
      case NFNStringValue("-w") :: NFNIntValue(value) :: tail => options += (Symbol("xres") -> value); args = args drop 2
      case NFNStringValue("-h") :: NFNIntValue(value) :: tail => options += (Symbol("yres") -> value); args = args drop 2
      case NFNContentObjectValue(name, data) :: tail => configuration = data; args = args drop 1
      case _ => args = List()
    }

    val dataString = new String(configuration)

    val deltaTime = 60
    val systemSize = Vector(Body.earth.radius * 500, Body.earth.radius * 500)
    val renderArea = Rect(-systemSize / 2, systemSize)
//    val config = Config.random(renderArea, 500)
    val config = Config.fromString(dataString)

    val simulation = new Simulation(config, deltaTime)

//    val resolution = Vector(options('xres), options('yres))
    val canvas = new BufferedImage(options(Symbol("xres")), options(Symbol("yres")), BufferedImage.TYPE_INT_RGB)
    simulation.render(renderArea, canvas)

    val baos = new ByteArrayOutputStream()
    ImageIO.write(canvas, "png", baos)
    val byteArray = baos.toByteArray

    NFNDataValue(byteArray)

  }
}

