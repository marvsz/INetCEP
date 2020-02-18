package nfn.tools

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Johannes on 27.9.2019
 */
object FilterHelpers {
  def applyConditions(sensorName: String, stream: Array[String], conditions: String, delimiter: String): Array[String] = {
    val sb = new StringBuilder
    val filterArguments = parseFilterArguments(conditions)
    val aNDs = filterArguments(0)
    val oRS = filterArguments(1)
    stream.foreach(line => {
      var lineAdded = false
      var andConditionisValid = true
      aNDs.foreach(
        and =>
          if (andConditionisValid && !conditionHandler(sensorName, and, line, delimiter)) {
            andConditionisValid = false
          })
      if (andConditionisValid && !lineAdded) {
        sb.append(line.toString + "\n")
        lineAdded = true
      }
      if (!lineAdded) {
        oRS.foreach(
          or => {
            if (!lineAdded && conditionHandler(sensorName, or, line, delimiter)) {
              sb.append(line.toString + "\n")
              lineAdded = true
            }
          }
        )
      }
    }
    )
    sb.toString().split("\n")
  }

  def conditionHandler(sensor: String, filter: String, line: String, delimiter: String): Boolean = {
    val operatorPattern = "[>,<,=,<>]+".r // regex for all operators
    val operator = operatorPattern.findFirstIn(filter).map(_.toString).getOrElse("")
    val value = filter.toString.split("[>,<,=,<>]+").map(_.trim)

    var queryVariable = ""
    var queryColumn = 0

    if (operator != "")
      queryColumn = SensorHelpers.getColumnNumber(sensor, value(0).stripPrefix("(").stripSuffix(")").trim)
    else
      return false

    queryVariable = value(1).stripPrefix("(").stripSuffix(")").trim
    val returnValue = matchCondition(operator, queryColumn, queryVariable, line, delimiter)
    returnValue
  }

  def matchCondition(operator: String, queryColumn: Int, queryVariable: String, line: String, delimiter: String): Boolean = {
    //First, split the link into schema:
    var schema = line.split(delimiter);

    var index = queryColumn
    //val schamaValue = schema(index).toDouble
    //val quaryVariableValue = queryVariable.toString.toDouble
    if (schema.length > index) {
      try {
        operator match {
          case ">" => if (schema(index).toDouble > queryVariable.toString.toDouble) {
            true
          } else false
          case "<" => if (schema(index).toDouble < queryVariable.toString.toDouble) {
            true
          } else false
          case "=" => if (schema(index).toLowerCase() == queryVariable.toLowerCase()) {
            true
          } else false
          case "<>" => if (schema(index).toLowerCase() != queryVariable.toLowerCase()) {
            true
          } else false
          case "NULL" => false
          case _ => false
        }
      }
      catch {
        case e: Exception => false
      }
    }
    else {
      false
    }
  }

  def parseFilterArguments(filter: String) = {
    var retVal = new Array[ArrayBuffer[String]](2)
    var allANDs = ArrayBuffer[String]()
    var allORs = ArrayBuffer[String]()

    val orFilters = filter.split('|').map(_.trim)
    if (orFilters.length > 0 && filter.contains("|")) {
      for (oF <- orFilters) {
        //This will give us a filter that contains ||
        if (oF.contains("&")) {
          //Further splits needed:
          //Split the AND first:
          val childAndOP = oF.split("&").map(_.trim)
          for (childOP <- childAndOP) {
            //Handle each condition and put it in a list of all AND conditions
            allANDs += childOP
          }
        }
        else {
          //This part of the OR Split does not contain and ANDs. Push all to OR list:
          allORs += oF
        }
      }
    }
    else {
      //Filter doesnt contain any ORs - so filter it with ANDs
      val andFilters = filter.split("&").map(_.trim)
      if (andFilters.length > 0 && filter.contains("&")) {
        for (aF <- andFilters) {
          //This will give us a filter that contains &&
          allANDs += aF
        }
      }
      else {
        //This is a single filter (e.g. 2>1001, so add it to andFilters and pass to handler)
        allANDs += filter
      }

    }

    retVal(0) = allANDs
    retVal(1) = allORs
    retVal
  }
}
