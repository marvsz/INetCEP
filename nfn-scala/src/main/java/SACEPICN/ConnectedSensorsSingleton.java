package SACEPICN;

import java.util.HashMap;

public class ConnectedSensorsSingleton {
    private static ConnectedSensorsSingleton instance;
    HashMap<String, Sensor> sensors;

    ConnectedSensorsSingleton() {

    }

    public static synchronized ConnectedSensorsSingleton getInstance() {
        if (ConnectedSensorsSingleton.instance == null)
            ConnectedSensorsSingleton.instance = new ConnectedSensorsSingleton();
        return ConnectedSensorsSingleton.instance;
    }

    /**
     * Adds a Sensor to the sensor List
     * @param sensorName the name of the sensor
     * @param sensor the sensor object
     */
    public void addSensor(String sensorName, Sensor sensor) {
        sensors.put(sensorName, sensor);
    }

    /**
     * Removes a Sensor from the sensor List identified by its name
     * This enables us to dynamically remove Sensors from a node.
     * @param sensorName the name of the sensor
     */
    public void removeSensor(String sensorName) {
        sensors.remove("sensorName");
    }
}
