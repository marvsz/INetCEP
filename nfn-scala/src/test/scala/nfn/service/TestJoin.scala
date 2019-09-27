package nfn.service

import config.StaticConfig
import nfn.tools.{Helpers, SensorHelpers}
import org.junit.Test
class TestJoin {
  val sacepicnEnv = StaticConfig.systemPath
  val testData1 = "TestData1Join2"
  val testData2 = "TestData2Join2"

  @Test
  def equalLinesequalDelimiter(){
    val join = new Join()
    val window1 = new Window()
    val window2 = new Window()
    val win1 = window1.readRelativeTimedSensor(testData1,5,"S","debugTest")
    val sensor1Name = "Victims"
    val win2 = window2.readRelativeTimedSensor(testData2,5,"S","debugTest")
    val sensor2Name = "Survivors"
    val joinOn = "Date"
    val conditions = ""
    val joinType = "innerjoin"
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
  }

}
