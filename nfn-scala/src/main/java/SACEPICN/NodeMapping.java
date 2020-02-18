package SACEPICN;

/**
 * Created by Ali on 06.02.18.
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

//We can also make this class final (essentially static)
public class NodeMapping {

    public HashMap<String, String> nodeMapPortName = null;
    public HashMap<String, String> nodeMapNamePort = null;

    public HashMap<String, String> nodeMapPortIP = null;
    public HashMap<String, String> nodeMapNameIP = null;

    public NodeMapping() {
        //String systemEnv = System.getenv("HOME") + "/INetCEP";//"/manisha/gitlab/ws18_aoc2_lab"; // "/INetCEP";
        //String systemEnv = System.getenv("HOME") + "/manisha/gitlab/ws18_aoc2_lab"; // "/INetCEP";

        if (nodeMapPortName == null && nodeMapNamePort == null) {
            NodeInformationSingleton nis = NodeInformationSingleton.getInstance();
            nodeMapPortName = new HashMap<String, String>();
            nodeMapNamePort = new HashMap<String, String>();

            nodeMapPortIP = new HashMap<String, String>();
            nodeMapNameIP = new HashMap<String, String>();
            for (NodeInformation nodeinfo : nis.nodeInfoList){
                nodeMapNamePort.put(nodeinfo._nodeName,nodeinfo._port);
                nodeMapPortName.put(nodeinfo._port,nodeinfo._nodeName);
                nodeMapPortIP.put(nodeinfo._port, nodeinfo._iPAddress);
                nodeMapNameIP.put(nodeinfo._nodeName, nodeinfo._iPAddress);
            }
        }
    }

    //Get Port by Node Name
    public String getPort(String name) {
        if (nodeMapNamePort != null) return nodeMapNamePort.get(name);
        else {
            NodeMapping obj = new NodeMapping();
            return obj.nodeMapNamePort.get(name);
        }
    }

    //Get Name by Node Port
    public String getName(String port) {
        if (nodeMapPortName != null) return nodeMapPortName.get(port);
        else {
            NodeMapping obj = new NodeMapping();
            return obj.nodeMapPortName.get(port);
        }
    }

    //Get IP by Node Name
    public String getIPbyName(String name) {
        if (nodeMapNameIP != null) return nodeMapNameIP.get(name);
        else {
            NodeMapping obj = new NodeMapping();
            return obj.nodeMapNameIP.get(name);
        }
    }
    //Get IP by Node Port
    public String getIPbyPort(String port) {
        if (nodeMapPortIP != null) return nodeMapPortIP.get(port);
        else {
            NodeMapping obj = new NodeMapping();
            return obj.nodeMapPortIP.get(port);
        }
    }
}