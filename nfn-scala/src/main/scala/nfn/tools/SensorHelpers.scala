package nfn.tools

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

import SACEPICN.{ConnectedSensorsSingleton, SchemaBrokerSingleton, Sensor}
import nfn.tools.Helpers.sacepicnEnv

import scala.io.Source

/**
 * Created by Johannes on 27.9.2019
 */
object SensorHelpers {

  /**
   * Returns a LocalTime from a given string
   *
   * @param datePart  a string representing the date. either in the form of a unix timestamp or in the form of HH:mm:ss.SSS
   * @param delimiter the delimiter that separates the values of the tuple.
   * @return the parsed time in the LocalTime Format
   */
  def parseTime(datePart: String, delimiter: String): LocalTime = {
    val DateTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    var dateString = datePart

    if (delimiter.equals(",")) {
      val calendar = Calendar.getInstance()
      calendar.setTimeInMillis(datePart.toLong * 1000)
      val hour = calendar.get(Calendar.HOUR_OF_DAY).toString
      val minute = calendar.get(Calendar.MINUTE).toString
      val second = calendar.get(Calendar.SECOND).toString

      dateString = ("0" + hour).takeRight(2) + ":" + ("0" + minute).takeRight(2) + ":" + ("0" + second).takeRight(2) + ".000"
    }
    if (delimiter.equals(";")) {
      dateString = datePart.split(" ")(1) + ".000"
    }

    LocalTime.parse(dateString, DateTimeFormat)
  }

  /**
   * Returns the delimiter that separates a tuple of values when given a single line
   *
   * @param line one tuple of values
   * @return the delimiter specifically for this tuple
   */
  def getDelimiterFromLine(line: String): String = {
    var output: String = ""
    if (line.contains("/"))
      output = "/"
    else if (line.contains(","))
      output = ","
    else if (line.contains(";"))
      output = ";"
    output
  }

  /**
   * Returns the position of the date in a tuple when given a delimiter
   *
   * @param delimiter the delmiiter that separates the values of the tuple. based on this we can return the position of the date in the tuple
   * @return the position of the date in the tuple
   */
  def getDatePosition(delimiter: String): Int = {
    var output: Int = 0
    if (delimiter.equals("/"))
      output = 0
    else if (delimiter.equals(","))
      output = 1
    else if (delimiter.equals(";"))
      output = 0
    output
  }

  /**
   * Returns the position of the value in a tuple when given a delimiter
   *
   * @param delimiter the delimiter that separates the values of the tuple. based on this we can return the position of the value in the tuple
   * @return the position of the value in the tuple
   */
  def getValuePosition(delimiter: String): Int = {
    var output: Int = 0
    if (delimiter.equals("/"))
      output = 1
    else if (delimiter.equals(","))
      output = 2
    else if (delimiter.equals(";"))
      output = 2
    output
  }

  /**
   * Returns the delimiter that seperates a tuple of values when given a file path
   *
   * @param path the path to where the file is located.
   * @return the delimiter specifically for the tuples in this file
   */
  def getDelimiterFromPath(path: String): String = {
    val bufferedSource = Source.fromFile(s"$sacepicnEnv/sensors/" + path)
    var output: String = ""
    //val b = bufferedSource.getLines().find(_ => true).toString()
    if (bufferedSource.getLines().find(_ => true).toString().contains("/"))
      output = "/"
    else if (bufferedSource.getLines().find(_ => true).toString().contains(","))
      output = ","
    else if (bufferedSource.getLines().find(_ => true).toString().contains(";"))
      output = ";"
    return output
  }

  /**
   * Returns the input data as a list of strings
   *
   * @param inputSource either name, sensor or data. decides what type of data is used for the input
   * @param sourceValue either a named interest or a path to a file
   * @return The input data read from the sourceValue as a list of strings
   */
  def parseData(inputSource: String, sourceValue: String): List[String] = {
    var output: List[String] = null
    if (inputSource == "sensor") {
      val dataSource = Source.fromFile(s"$sacepicnEnv/sensors/" + sourceValue)
      output = dataSource.getLines.toList
      dataSource.close
    }
    if (inputSource == "data") {
      output = sourceValue.split("\n")
        .toSeq
        .map(_.trim)
        .filter(_ != "").toList
    }
    if (inputSource == "name") {
      output = sourceValue.split("\n")
        .toSeq
        .map(_.trim)
        .filter(_ != "").toList
    }

    output
  }

  /**
   *
   * @param delimiter the delimiter on which we base our decision on where to find the longitude position
   * @return the position on where to find the longitude in a given tuple
   */
  def getLongitudePosition(delimiter: String): Int = {
    var output: Int = 0
    if (delimiter.equals(";"))
      output = 3
    output
  }

  /**
   *
   * @param delimiter the delimiter on which we base our decision on where to find the latitude position
   * @return the position on where to find the latitude in a given tuple
   */
  def getLattitudePosition(delimiter: String): Int = {
    var output: Int = 0
    if (delimiter.equals(";"))
      output = 2
    output
  }

  /**
   * Trims the data and adds a # in front of each line
   *
   * @param input A string of data
   * @return a string with a # as a leading symbol
   */
  def trimData(input: String) = {
    val output = new StringBuilder
    if (input != null && input != "") {
      val bufferedSource = input.split("\n")
        .toSeq
        .map(_.trim)
        .filter(_ != "")

      bufferedSource
        .foreach { line: String => {
          if (!line.contains("redirect")) {
            output.append(line.toString + "\n")
          }
        }
        }
    }
    output.toString()
  }

  /**
   * Returns the column number for a sensor and a given column name
   *
   * @param sensorName the name of the sensor
   * @param columnName the name of the column
   * @return the id of the column
   */
  def getColumnNumber(sensorName: String, columnName: String): Int = {
    val broker = SchemaBrokerSingleton.getInstance()
    broker.getColumnId(sensorName, columnName)
  }

  /**
   * Call to the joinSchema function of the Schema Broker
   *
   * @param leftSensorName  the name of the first Sensor
   * @param rightSensorName the name of the second Sensor
   * @param joinOn          the name of the Column on which to join on
   * @param conditions      the conditions on which to join on
   * @return true if the new sensor schema was created, false otherwise.
   */
  def joinSensors(leftSensorName: String, rightSensorName: String, joinOn: String, conditions: String) = {
    val broker = SchemaBrokerSingleton.getInstance()
    broker.joinSchema(joinOn, conditions, leftSensorName, rightSensorName)
  }

  /**
   * Returns the Column Names of a Schema separated by commas
   *
   * @param sensorName the name of the sensor for which we want the schema for
   * @return the column names of the request schema separated by commas if the sensor exists, ohterwise an error Message
   */
  def getSchema(sensorName: String) = {
    val broker = SchemaBrokerSingleton.getInstance()
    val schema = broker.getSchema(sensorName)
    if (schema != null)
      schema.toArray().mkString(",")
    else
      "Schema does not exist"
  }

  /**
   * Returns the new schema name of two joined data streams
   *
   * @param leftSensorName  the name of the first data stream
   * @param rightSensorName the name of the second data stream
   * @param joinOn          the column on which to join on
   * @param conditions      the conditions
   * @return the new name of the joined schemas
   */
  def getJoinedSchemaName(leftSensorName: String, rightSensorName: String, joinOn: String, conditions: String) =
    "Join(".concat(leftSensorName).concat(",").concat(rightSensorName).concat("|").concat(joinOn).concat(",[").concat(conditions).concat("]").concat(")")

  /**
   * Adds a sensor to the Schema broker and to the List of connected Sensors for this node..
   * @param filename
   * @param delimiter
   */
  def addSensor(filename:String, delimiter:String) : Unit ={
    val connectedSensors = ConnectedSensorsSingleton.getInstance()
    val broker = SchemaBrokerSingleton.getInstance()
    connectedSensors.addSensor(filename,new Sensor(delimiter.charAt(0),filename))
    val schemaInserted = broker.insertSchema(connectedSensors.getSensor(filename).getType,connectedSensors.getSensor(filename).getSchema)
  }

  /**
   * Removes a sensor from the connected Sensor List. Maybe deprecated?
   * @param sensorName
   */
  def removeSensor(sensorName:String) : Unit = {
    val connectedSensors = ConnectedSensorsSingleton.getInstance()
    connectedSensors.removeSensor(sensorName)
  }

  /**
   * Prints the contents of the Schema List. just for debugging purposes
   *
   * @return The List of the Schemas in the form of "SchemaName: Tuple1,Tuple2,Tuple3,..."
   */

  def printSchemaList(): String={
    val broker = SchemaBrokerSingleton.getInstance()
    val sb = new StringBuilder()
    val map = broker.schemes
    var it = map.entrySet().iterator()
    map.entrySet().forEach{
      pair => sb.append(pair.getKey + ":" + pair.getValue.toString + "\n")
    }
    sb.toString()
  }
}
