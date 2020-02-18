package nfn.service

import config.StaticConfig
import nfn.tools.{Helpers, SensorHelpers}
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions._
class TestJoin {
  val sacepicnEnv = StaticConfig.systemPath
  val testData1 = "TestData1Join2"
  val testData2 = "TestData2Join2"

  /*
  @Test
  def equalLinesequalDelimiter(){
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Gender"
    val conditions = ""
    val joinType = "innerJoin"
    System.out.println("New Schema:")
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    System.out.println(SensorHelpers.getSchema(joinedSchemaName))
    System.out.println("Query Result: ")
    val res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    if(res == "")
      System.out.println("No Results!")
    else
      System.out.println(res)
  }*/

  @Test
  def innerJoinVictimsSurvivorsOnDateNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Date"
    val conditions = ""
    val joinType = "innerJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:00.000/87401/F/55/87406/M/96\n00:00:01.000/87402/M/80/87407/M/34\n00:00:02.000/87403/F/16/87408/F/68\n00:00:03.000/87404/F/98/87409/F/5\n00:00:04.000/87405/F/56/87410/M/47"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def innerJoinVictimsSurvivorsOnSequenceNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "SequenceNumber"
    val conditions = ""
    val joinType = "innerJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "No Results!"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def innerJoinVictimsSurvivorsOnGenderNoConditions() ={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Gender"
    val conditions = ""
    val joinType = "innerJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    System.out.println("Query Result: ")
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:00.000/87401/F/55/00:00:02.000/87408/68\n00:00:00.000/87401/F/55/00:00:03.000/87409/5\n00:00:01.000/87402/M/80/00:00:00.000/87406/96\n00:00:01.000/87402/M/80/00:00:01.000/87407/34\n00:00:01.000/87402/M/80/00:00:04.000/87410/47\n00:00:02.000/87403/F/16/00:00:02.000/87408/68\n00:00:02.000/87403/F/16/00:00:03.000/87409/5\n00:00:03.000/87404/F/98/00:00:02.000/87408/68\n00:00:03.000/87404/F/98/00:00:03.000/87409/5\n00:00:04.000/87405/F/56/00:00:02.000/87408/68\n00:00:04.000/87405/F/56/00:00:03.000/87409/5"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def innerJoinVictimsSurvivorsOnAgeNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Age"
    val conditions = ""
    val joinType = "innerJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "No Results!"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def leftJoinVictimsSurvivorsOnDateNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Date"
    val conditions = ""
    val joinType = "leftOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:00.000/87401/F/55/87406/M/96\n00:00:01.000/87402/M/80/87407/M/34\n00:00:02.000/87403/F/16/87408/F/68\n00:00:03.000/87404/F/98/87409/F/5\n00:00:04.000/87405/F/56/87410/M/47"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def leftJoinVictimsSurvivorsOnSequenceNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "SequenceNumber"
    val conditions = ""
    val joinType = "leftOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:00.000/87401/F/55/Null/Null/Null\n00:00:01.000/87402/M/80/Null/Null/Null\n00:00:02.000/87403/F/16/Null/Null/Null\n00:00:03.000/87404/F/98/Null/Null/Null\n00:00:04.000/87405/F/56/Null/Null/Null"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def leftJoinVictimsSurvivorsOnGenderNoConditions() ={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Gender"
    val conditions = ""
    val joinType = "leftOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:00.000/87401/F/55/00:00:02.000/87408/68\n00:00:00.000/87401/F/55/00:00:03.000/87409/5\n00:00:01.000/87402/M/80/00:00:00.000/87406/96\n00:00:01.000/87402/M/80/00:00:01.000/87407/34\n00:00:01.000/87402/M/80/00:00:04.000/87410/47\n00:00:02.000/87403/F/16/00:00:02.000/87408/68\n00:00:02.000/87403/F/16/00:00:03.000/87409/5\n00:00:03.000/87404/F/98/00:00:02.000/87408/68\n00:00:03.000/87404/F/98/00:00:03.000/87409/5\n00:00:04.000/87405/F/56/00:00:02.000/87408/68\n00:00:04.000/87405/F/56/00:00:03.000/87409/5"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def leftJoinVictimsSurvivorsOnAgeNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Age"
    val conditions = ""
    val joinType = "leftOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:00.000/87401/F/55/Null/Null/Null\n00:00:01.000/87402/M/80/Null/Null/Null\n00:00:02.000/87403/F/16/Null/Null/Null\n00:00:03.000/87404/F/98/Null/Null/Null\n00:00:04.000/87405/F/56/Null/Null/Null"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def rightJoinVictimsSurvivorsOnDateNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Date"
    val conditions = ""
    val joinType = "rightOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:00.000/87401/F/55/87406/M/96\n00:00:01.000/87402/M/80/87407/M/34\n00:00:02.000/87403/F/16/87408/F/68\n00:00:03.000/87404/F/98/87409/F/5\n00:00:04.000/87405/F/56/87410/M/47"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def rightJoinVictimsSurvivorsOnSequenceNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "SequenceNumber"
    val conditions = ""
    val joinType = "rightOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "Null/Null/Null/00:00:00.000/87406/M/96\nNull/Null/Null/00:00:01.000/87407/M/34\nNull/Null/Null/00:00:02.000/87408/F/68\nNull/Null/Null/00:00:03.000/87409/F/5\nNull/Null/Null/00:00:04.000/87410/M/47"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def rightJoinVictimsSurvivorsOnGenderNoConditions() ={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Gender"
    val conditions = ""
    val joinType = "rightOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:01.000/87402/M/80/00:00:00.000/87406/96\n00:00:01.000/87402/M/80/00:00:01.000/87407/34\n00:00:00.000/87401/F/55/00:00:02.000/87408/68\n00:00:02.000/87403/F/16/00:00:02.000/87408/68\n00:00:03.000/87404/F/98/00:00:02.000/87408/68\n00:00:04.000/87405/F/56/00:00:02.000/87408/68\n00:00:00.000/87401/F/55/00:00:03.000/87409/5\n00:00:02.000/87403/F/16/00:00:03.000/87409/5\n00:00:03.000/87404/F/98/00:00:03.000/87409/5\n00:00:04.000/87405/F/56/00:00:03.000/87409/5\n00:00:01.000/87402/M/80/00:00:04.000/87410/47"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def rightJoinVictimsSurvivorsOnAgeNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Age"
    val conditions = ""
    val joinType = "rightOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "Null/Null/Null/00:00:00.000/87406/M/96\nNull/Null/Null/00:00:01.000/87407/M/34\nNull/Null/Null/00:00:02.000/87408/F/68\nNull/Null/Null/00:00:03.000/87409/F/5\nNull/Null/Null/00:00:04.000/87410/M/47"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def fullOuterJoinVictimsSurvivorsOnDateNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Date"
    val conditions = ""
    val joinType = "fullOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:00.000/87401/F/55/87406/M/96\n00:00:01.000/87402/M/80/87407/M/34\n00:00:02.000/87403/F/16/87408/F/68\n00:00:03.000/87404/F/98/87409/F/5\n00:00:04.000/87405/F/56/87410/M/47"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def fullOuterJoinVictimsSurvivorsOnSequenceNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "SequenceNumber"
    val conditions = ""
    val joinType = "fullOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:00.000/87401/F/55/Null/Null/Null\n00:00:01.000/87402/M/80/Null/Null/Null\n00:00:02.000/87403/F/16/Null/Null/Null\n00:00:03.000/87404/F/98/Null/Null/Null\n00:00:04.000/87405/F/56/Null/Null/Null\nNull/Null/Null/00:00:00.000/87406/M/96\nNull/Null/Null/00:00:01.000/87407/M/34\nNull/Null/Null/00:00:02.000/87408/F/68\nNull/Null/Null/00:00:03.000/87409/F/5\nNull/Null/Null/00:00:04.000/87410/M/47"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def fullOuterJoinVictimsSurvivorsOnGenderNoConditions() ={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Gender"
    val conditions = ""
    val joinType = "fullOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:00.000/87401/F/55/00:00:02.000/87408/68\n00:00:00.000/87401/F/55/00:00:03.000/87409/5\n00:00:01.000/87402/M/80/00:00:00.000/87406/96\n00:00:01.000/87402/M/80/00:00:01.000/87407/34\n00:00:01.000/87402/M/80/00:00:04.000/87410/47\n00:00:02.000/87403/F/16/00:00:02.000/87408/68\n00:00:02.000/87403/F/16/00:00:03.000/87409/5\n00:00:03.000/87404/F/98/00:00:02.000/87408/68\n00:00:03.000/87404/F/98/00:00:03.000/87409/5\n00:00:04.000/87405/F/56/00:00:02.000/87408/68\n00:00:04.000/87405/F/56/00:00:03.000/87409/5"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

  @Test
  def fullOuterJoinVictimsSurvivorsOnAgeNoConditions()={
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Age"
    val conditions = ""
    val joinType = "fullOuterJoin"
    val joiningWorked = SensorHelpers.joinSensors(sensor1Name,sensor2Name,joinOn,conditions)
    val joinedSchemaName = SensorHelpers.getJoinedSchemaName(sensor1Name,sensor2Name,joinOn,conditions)
    var res = join.joinStreamsOn(win1,sensor1Name,win2,sensor2Name,joinOn,conditions,joinType)
    val expectedOutcome = "00:00:00.000/87401/F/55/Null/Null/Null\n00:00:01.000/87402/M/80/Null/Null/Null\n00:00:02.000/87403/F/16/Null/Null/Null\n00:00:03.000/87404/F/98/Null/Null/Null\n00:00:04.000/87405/F/56/Null/Null/Null\nNull/Null/Null/00:00:00.000/87406/M/96\nNull/Null/Null/00:00:01.000/87407/M/34\nNull/Null/Null/00:00:02.000/87408/F/68\nNull/Null/Null/00:00:03.000/87409/F/5\nNull/Null/Null/00:00:04.000/87410/M/47"
    if(res == "")
      res = "No Results!"
    assertEquals(expectedOutcome,res)
  }

}
