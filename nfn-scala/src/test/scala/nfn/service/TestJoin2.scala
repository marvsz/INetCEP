package nfn.service

import config.StaticConfig
import org.junit.Test
class TestJoin2 {
  val sacepicnEnv = StaticConfig.systemPath
  val testData1 = "TestData1Join2"
  val testData2 = "TestData2Join2"

  @Test
  def equalLinesequalDelimiter(){
    val join = new Join2()
    val window1 = new Window()
    val window2 = new Window()
    System.out.println("Query Result: ")
    val res = join.joinStreams(window1.readRelativeTimedSensor(testData1,5,"S","debugTest"), window2.readRelativeTimedSensor(testData2,5,"S","debugTest"))
    if(res == "")
      System.out.println("No Results!")
    else
      System.out.println(res)
  }

}
