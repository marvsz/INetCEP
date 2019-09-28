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

    public void addSensor(String sensorName, Sensor sensor) {
        sensors.put(sensorName, sensor);
    }

    public void removeSensor(String sensorName) {
        sensors.remove("sensorName");
    }
}
