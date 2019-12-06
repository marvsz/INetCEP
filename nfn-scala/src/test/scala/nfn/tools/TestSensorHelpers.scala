package nfn.tools

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestSensorHelpers {
  @Test
  def testPrintSchemaList = {
    System.out.println(SensorHelpers.printSchemaList)
    assertTrue(true)
  }

  @Test
  def testAddNewSensor={
    SensorHelpers.addSensor("survivors","/")
    val expectedList = "survivors:[date, gender, sequencenumber, age]\nplug:[sequencenumber, date, value, property, plug_id, household_id, house_id]\ngps:[date, identifier, latitude, longitude, altitude, accuracy, distance, speed]\nvictims:[date, sequencenumber, gender, age]\n"
    assertTrue(expectedList==SensorHelpers.printSchemaList)
  }

}
