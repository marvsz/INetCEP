package nfn.service

/**
  * Created by Ali on 06.02.18.
  */
import akka.actor.ActorRef

import scala.io.Source

//Added for contentfetch
import java.nio.file.{Files, Paths}

import ccn.packet.CCNName
import config.StaticConfig

import scala.collection.mutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class GetData() extends NFNService {

  override def function(interestName: CCNName, args: Seq[NFNValue], ccnApi: ActorRef): Future[NFNValue]= Future{

    val sacepicnEnv = StaticConfig.systemPath

    def getData(name: NFNStringValue): String = {
      var objectname = s"$sacepicnEnv/nodeData/" + name.str.replace("/", "-");
      var output: String = ""
      if (Files.exists(Paths.get(objectname))) {
        val bufferedSource = Source.fromFile(objectname)
        bufferedSource
          .getLines
          .foreach { line: String =>
            output += "#" + line.toString() + "\n";
          }
        bufferedSource.close
      }

      if (output != "")
        output = output.stripSuffix("\n").stripMargin('#');
      else
        output += "No Results!"

      return output;
    }

    NFNStringValue(
      args match {
        case Seq(name: NFNStringValue) => getData(name)
        case _ =>
          throw new NFNServiceArgumentException(s"$ccnName can only be applied to values of type NFNBinaryDataValue and not $args")
      })
  }
}

