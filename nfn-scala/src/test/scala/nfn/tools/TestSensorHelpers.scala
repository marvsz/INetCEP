package nfn.tools

import org.scalatest.{FlatSpec, Matchers}

class TestSensorHelpers extends FlatSpec with Matchers {

  "printSchemaList" should "Print the schema" in {
    SensorHelpers.printSchemaList should be ("survivors:[date, sequencenumber, gender, age]\nplug:[sequencenumber, date, value, property, plug_id, household_id, house_id]\ngps:[date, identifier, latitude, longitude, altitude, accuracy, distance, speed]\nvictims:[date, sequencenumber, gender, age]\n")
  }

  "addSensor" should "Add a new Schema to the schema list" in {
    SensorHelpers.addSensor("survivors","/")
    val expectedList = "survivors:[date, sequencenumber, gender, age]\nplug:[sequencenumber, date, value, property, plug_id, household_id, house_id]\ngps:[date, identifier, latitude, longitude, altitude, accuracy, distance, speed]\nvictims:[date, sequencenumber, gender, age]\n"
    val actualList = SensorHelpers.printSchemaList()
    expectedList should be (actualList)
  }

  "removeSensor" should "Remove a Sensor Schema from the Schema list" in {
    SensorHelpers.removeSensor("victims")
    val expectedList = "survivors:[date, sequencenumber, gender, age]\nplug:[sequencenumber, date, value, property, plug_id, household_id, house_id]\ngps:[date, identifier, latitude, longitude, altitude, accuracy, distance, speed]\n"
    val actualList = SensorHelpers.printSchemaList()
    expectedList should be (actualList)
  }

}
