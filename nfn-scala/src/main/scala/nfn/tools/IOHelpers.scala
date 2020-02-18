package nfn.tools

import java.io.{File, FileOutputStream, PrintWriter}
import java.util.Base64

import myutil.FormattedOutput
import nfn.tools.Helpers.sacepicnEnv

import scala.io.Source

object IOHelpers {
  /**
   * Convert a Array[Byte] to a String (base64 encoding)
   * Inverse function of stringToByte(...)
   *
   * NOTE: Output String has more characters than length of data:Byte[Array] because of base64 encoding.
   *
   * @param   data Data to convert
   * @return Same data as String (base64 encoding)
   */
  def byteToString(data: Array[Byte]): String =
    Base64.getEncoder.encodeToString(data) // note: this is new in java 8


  /**
   * Convert a String (base64 encoding) to a Array[Byte].
   * Inverse function of byteToString(...)
   *
   * NOTE: data:String has more characters than length of returned Byte[Array] because of base64 encoding.
   *
   * @param   data Data to convert (base64 encoding)
   * @return Same data as Array[Byte]
   */
  def stringToByte(data: String): Array[Byte] =
    Base64.getDecoder.decode(data) // note: this is new in java 8

  /**
   * Writes two strings into a single file placementUtilityFunction
   *
   * @param Energy   the Energy data
   * @param Overhead the overhead used
   * @return void
   */
  def writeMetricsToStore(Energy: String, Overhead: String): Any = {
    val weights = s"$sacepicnEnv/nodeData/placementUtilityFunction"
    val file1 = new File(weights)
    file1.getParentFile.mkdirs()
    file1.createNewFile()

    val pw1 = new PrintWriter(new FileOutputStream(file1, true))

    var writeText = s"$Energy\\|$Overhead"
    pw1.println(writeText)
    pw1.close()
  }

  /**
   * Writes a string given as the first arguemtn in the queryOutput and a string given as the second argument in the queryWeightVariance
   *
   * @param runAnalysis a string with all analysis data form the run
   * @param queryResult a string containing the weight variance
   * @return void
   */
  def writeOutputFiles(runAnalysis: String, queryResult: String): Any = {

    var queryOutput = s"$sacepicnEnv/nodeData/queryOutput"
    var queryResultFile = s"$sacepicnEnv/nodeData/queryResult"
    val file1 = new File(queryOutput)
    val file4 = new File(queryResultFile)
    file1.getParentFile.mkdirs()
    file1.createNewFile()
    file4.getParentFile.mkdirs()
    file4.createNewFile()

    val pw1 = new PrintWriter(new FileOutputStream(file1, true))

    val pw4 = new PrintWriter(new FileOutputStream(file4, true))
    pw1.println(runAnalysis)
    pw1.close()
    pw4.println("Query Result:")
    pw4.println(queryResult)
    pw4.close()
  }

  /**
   * Writes a string given as the first arguemtn in the queryOutput and a string given as the second argument in the queryWeightVariance
   *
   * @param runAnalysis    a string with all analysis data form the run
   * @param weightVariance a string containing the weight variance
   * @return void
   */
  def writeOutputFiles(runAnalysis: String, weightVariance: String, queryResult: String): Any = {

    var queryOutput = s"$sacepicnEnv/nodeData/queryOutput"
    var queryWeightVariance = s"$sacepicnEnv/nodeData/queryWeightVariance"
    var queryResultFile = s"$sacepicnEnv/nodeData/queryResult"
    val file1 = new File(queryOutput)
    val file2 = new File(queryWeightVariance)
    val file4 = new File(queryResultFile)
    file1.getParentFile.mkdirs()
    file1.createNewFile()
    file2.getParentFile.mkdirs()
    file2.createNewFile()
    file4.getParentFile.mkdirs()
    file4.createNewFile()

    val pw1 = new PrintWriter(new FileOutputStream(file1, true))
    val pw2 = new PrintWriter(new FileOutputStream(file2, true))

    val pw4 = new PrintWriter(new FileOutputStream(file4, true))
    pw1.println(runAnalysis)
    pw1.close()
    pw2.println(weightVariance)
    pw2.close()
    pw4.println("Query Result:")
    pw4.println(queryResult)
    pw4.close()
  }

  def writeOutputFiles(runAnalysis: String, weightVariance: String, accuracyOutput: String, queryResult: String): Any = {

    var queryOutput = s"$sacepicnEnv/nodeData/queryOutput"
    var queryWeightVariance = s"$sacepicnEnv/nodeData/queryWeightVariance"
    var accuracyOutputFile = s"$sacepicnEnv/nodeData/queryAccuracy"
    var queryResultFile = s"$sacepicnEnv/nodeData/queryResult"
    val file1 = new File(queryOutput)
    val file2 = new File(queryWeightVariance)
    val file3 = new File(accuracyOutputFile)
    val file4 = new File(queryResultFile)
    file1.getParentFile.mkdirs()
    file1.createNewFile()
    file2.getParentFile.mkdirs()
    file2.createNewFile()
    file3.getParentFile.mkdirs()
    file3.createNewFile()
    file4.getParentFile.mkdirs()
    file4.createNewFile()
    val pw1 = new PrintWriter(new FileOutputStream(file1, true))
    val pw2 = new PrintWriter(new FileOutputStream(file2, true))
    val pw3 = new PrintWriter(new FileOutputStream(file3, true))
    val pw4 = new PrintWriter(new FileOutputStream(file4, true))
    pw1.println(runAnalysis)
    pw1.close()
    pw2.println(weightVariance)
    pw2.close()
    pw3.println(accuracyOutput)
    pw3.close()
    pw4.println("Query Result:")
    pw4.println(queryResult)
    pw4.close()
  }

  def getMultiObjectiveFunctionMetrics: Array[Double] = {
    val bufferedSource = Source.fromFile(s"$sacepicnEnv/nodeData/placementUtilityFunction")
    var returnData = new Array[Double](2); //0.8|0.2 (ENERGY|BANDWIDTH.DELAY.PRODUCT)
    //To ensure that we always have utility function. Else, we get it from the file
    returnData(0) = 0.5
    returnData(1) = 0.5

    //To ensure that we get a E|BDP value from the file, we iterate over all lines and check the last line in the config file.
    bufferedSource
      .getLines
      .foreach { line: String =>
        var lineSplit = line.split("\\|")
        if (lineSplit.length > 1) {
          //Load this information in the Array:
          returnData(0) = FormattedOutput.parseDouble(lineSplit(0))
          returnData(1) = FormattedOutput.parseDouble(lineSplit(1))
        }
      }

    bufferedSource.close

    returnData
  }
}
