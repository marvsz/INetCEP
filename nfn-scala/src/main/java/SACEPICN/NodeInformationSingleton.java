package SACEPICN;

import java.util.ArrayList;
import java.util.Arrays;

public class NodeInformationSingleton {
    private static NodeInformationSingleton instance;

    public static final ArrayList<NodeInformation> nodeInfoList = new ArrayList<NodeInformation>(Arrays.asList(
            new NodeInformation("nodeA", "10.2.1.118", "9001"),
            new NodeInformation("nodeB", "10.2.1.85", "9002"),
            new NodeInformation("nodeC", "10.2.1.70", "9003"),
            new NodeInformation("nodeD", "10.2.1.71", "9004"),
            new NodeInformation("nodeE", "10.2.1.72", "9005"),
            new NodeInformation("nodeF", "10.2.1.84", "9006"),
            new NodeInformation("nodeG", "10.2.1.74", "9007")));

    private NodeInformationSingleton() {
    }

    public static synchronized NodeInformationSingleton getInstance() {
        if (NodeInformationSingleton.instance == null) {
            NodeInformationSingleton.instance = new NodeInformationSingleton();
        }
        return NodeInformationSingleton.instance;
    }


}
