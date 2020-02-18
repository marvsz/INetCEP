import java.io.{BufferedReader, InputStream, InputStreamReader}

import sbt._

object CommonBuildCode {
  def compileCCNLitePerform(): Unit = {
    val ccnlPath = {
      if(new File("./ccn-lite-nfn/bin").exists()) {
        new File("./ccn-lite-nfn").getCanonicalPath
      } else {
        val p = System.getenv("CCNL_HOME")
        if(p == null || p == "") {
          throw new Exception("nfn-scala ccn-lite submodule was not initialzed (either git clone --recursive or git submodule init && git submodule update) or CCNL_HOME was not set")
        } else p
      }
    }

    val processBuilder = {
      val cmds = List("make", "clean", "all")

      new java.lang.ProcessBuilder(cmds:_*)
    }
    val ccnlPathFile = new File(s"$ccnlPath/src")
    println(s"Building CCN-Lite in directory $ccnlPathFile")
    processBuilder.directory(ccnlPathFile)
    val e = processBuilder.environment()
    e.put("USE_NFN", "1")
    e.put("USE_NACK", "1")
    val process = processBuilder.start()
    val processOutputReaderPrinter = new InputStreamToStdOut(process.getInputStream)
    val t = new Thread(processOutputReaderPrinter).start()
    process.waitFor()
    val resVal = process.exitValue()
    if(resVal == 0)
      println(s"Compiled ccn-lite with return value ${process.exitValue()}")
    else {
      throw new Exception("Error during compilation of ccn-lite")
    }
    process.destroy()
  }
}

class InputStreamToStdOut(is: InputStream) extends Runnable {
  override def run(): Unit = {
    val reader = new BufferedReader(new InputStreamReader(is))
    var line = reader.readLine
    while(line != null) {
      line = reader.readLine
    }
  }
}
