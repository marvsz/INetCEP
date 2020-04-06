package nfn.service

import java.io.File

import INetCEP.StatesSingleton
import akka.actor.ActorRef
import ccn.packet.CCNName
import myutil.IOHelper
import myutil.systemcomandexecutor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PDFLatex extends NFNService {
  override def function(interestName: CCNName, args: Seq[NFNValue],stateHolder:StatesSingleton, ccnApi: ActorRef): Future[NFNValue] = Future {
    args match {
      case Seq(NFNContentObjectValue(_, doc)) =>

        val dir = new File(s"./temp-service-library/${IOHelper.uniqueFileName("pdflatex")}")
        if(!dir.exists()) dir.mkdirs()
        val cmds = List("pdflatex", s"-output-directory=${dir.getCanonicalPath}")
        SystemCommandExecutor(List(cmds), Some(doc)).executeWithTimeout() match {
          case ExecutionSuccess(_, translatedDoc) =>
            dir.list().find(_.endsWith(".pdf")) match {
              case Some(pdfFile) => NFNDataValue(IOHelper.readByteArrayFromFile(new File(dir.toString + "/" + pdfFile)))
              case None => NFNStringValue(s"Error when executing pdflatex, " +
                "the resulting pdf could not be created, but pdflatex executed without error")
            }
          case ExecutionError(_, errData, _) => NFNStringValue(s"Error when executing pdflatex: ${new String(errData)}")
        }
      case _ => throw new NFNServiceArgumentException(s"$args must be of type [Name, String, String]")
    }
  }
}
