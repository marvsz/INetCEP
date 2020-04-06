package INetCEP;

import java.util.HashMap;

public class DecentralizedNodeInformationSingleton {
    private static DecentralizedNodeInformationSingleton instance;
    public static final HashMap<String, HashMap<String, DecentralizedNodeInformation>> decentralizedNodeInformationHashMap = new HashMap<String, HashMap<String, DecentralizedNodeInformation>>() {{
        put("nodeA", new HashMap<String, DecentralizedNodeInformation>() {{
                    put("nodeA", new DecentralizedNodeInformation("nodeA", "10.2.1.88", "9001", 0));
                    put("nodeB", new DecentralizedNodeInformation("nodeB", "10.2.1.85", "9002", 1));
                    put("nodeC", new DecentralizedNodeInformation("nodeC", "10.2.1.70", "9003", 2));
                    put("nodeD", new DecentralizedNodeInformation("nodeD", "10.2.1.71", "9004", 2));
                    put("nodeE", new DecentralizedNodeInformation("nodeE", "10.2.1.72", "9005", 2));
                    put("nodeF", new DecentralizedNodeInformation("nodeF", "10.2.1.84", "9006", 3));
                    put("nodeG", new DecentralizedNodeInformation("nodeG", "10.2.1.74", "9007", 3));
                }}
        );
        put("nodeB", new HashMap<String, DecentralizedNodeInformation>() {{
                    put("nodeA", new DecentralizedNodeInformation("nodeA", "10.2.1.88", "9001", 1));
                    put("nodeB", new DecentralizedNodeInformation("nodeB", "10.2.1.85", "9002", 0));
                    put("nodeC", new DecentralizedNodeInformation("nodeC", "10.2.1.70", "9003", 1));
                    put("nodeD", new DecentralizedNodeInformation("nodeD", "10.2.1.71", "9004", 1));
                    put("nodeE", new DecentralizedNodeInformation("nodeE", "10.2.1.72", "9005", 1));
                    put("nodeF", new DecentralizedNodeInformation("nodeF", "10.2.1.84", "9006", 2));
                    put("nodeG", new DecentralizedNodeInformation("nodeG", "10.2.1.74", "9007", 2));
                }}
        );
        put("nodeC", new HashMap<String, DecentralizedNodeInformation>() {{
                    put("nodeA", new DecentralizedNodeInformation("nodeA", "10.2.1.88", "9001", 2));
                    put("nodeB", new DecentralizedNodeInformation("nodeB", "10.2.1.85", "9002", 1));
                    put("nodeC", new DecentralizedNodeInformation("nodeC", "10.2.1.70", "9003", 0));
                    put("nodeD", new DecentralizedNodeInformation("nodeD", "10.2.1.71", "9004", 2));
                    put("nodeE", new DecentralizedNodeInformation("nodeE", "10.2.1.72", "9005", 2));
                    put("nodeF", new DecentralizedNodeInformation("nodeF", "10.2.1.84", "9006", 3));
                    put("nodeG", new DecentralizedNodeInformation("nodeG", "10.2.1.74", "9007", 1));
                }}
        );
        put("nodeD", new HashMap<String, DecentralizedNodeInformation>() {{
                    put("nodeA", new DecentralizedNodeInformation("nodeA", "10.2.1.88", "9001", 2));
                    put("nodeB", new DecentralizedNodeInformation("nodeB", "10.2.1.85", "9002", 1));
                    put("nodeC", new DecentralizedNodeInformation("nodeC", "10.2.1.70", "9003", 2));
                    put("nodeD", new DecentralizedNodeInformation("nodeD", "10.2.1.71", "9004", 0));
                    put("nodeE", new DecentralizedNodeInformation("nodeE", "10.2.1.72", "9005", 2));
                    put("nodeF", new DecentralizedNodeInformation("nodeF", "10.2.1.84", "9006", 3));
                    put("nodeG", new DecentralizedNodeInformation("nodeG", "10.2.1.74", "9007", 3));
                }}
        );
        put("nodeE", new HashMap<String, DecentralizedNodeInformation>() {{
                    put("nodeA", new DecentralizedNodeInformation("nodeA", "10.2.1.88", "9001", 2));
                    put("nodeB", new DecentralizedNodeInformation("nodeB", "10.2.1.85", "9002", 1));
                    put("nodeC", new DecentralizedNodeInformation("nodeC", "10.2.1.70", "9003", 2));
                    put("nodeD", new DecentralizedNodeInformation("nodeD", "10.2.1.71", "9004", 2));
                    put("nodeE", new DecentralizedNodeInformation("nodeE", "10.2.1.72", "9005", 0));
                    put("nodeF", new DecentralizedNodeInformation("nodeF", "10.2.1.84", "9006", 1));
                    put("nodeG", new DecentralizedNodeInformation("nodeG", "10.2.1.74", "9007", 3));
                }}
        );
        put("nodeF", new HashMap<String, DecentralizedNodeInformation>() {{
                    put("nodeA", new DecentralizedNodeInformation("nodeA", "10.2.1.88", "9001", 3));
                    put("nodeB", new DecentralizedNodeInformation("nodeB", "10.2.1.85", "9002", 2));
                    put("nodeC", new DecentralizedNodeInformation("nodeC", "10.2.1.70", "9003", 3));
                    put("nodeD", new DecentralizedNodeInformation("nodeD", "10.2.1.71", "9004", 3));
                    put("nodeE", new DecentralizedNodeInformation("nodeE", "10.2.1.72", "9005", 1));
                    put("nodeF", new DecentralizedNodeInformation("nodeF", "10.2.1.84", "9006", 0));
                    put("nodeG", new DecentralizedNodeInformation("nodeG", "10.2.1.74", "9007", 4));
                }}
        );
        put("nodeG", new HashMap<String, DecentralizedNodeInformation>() {{
                    put("nodeA", new DecentralizedNodeInformation("nodeA", "10.2.1.88", "9001", 3));
                    put("nodeB", new DecentralizedNodeInformation("nodeB", "10.2.1.85", "9002", 2));
                    put("nodeC", new DecentralizedNodeInformation("nodeC", "10.2.1.70", "9003", 1));
                    put("nodeD", new DecentralizedNodeInformation("nodeD", "10.2.1.71", "9004", 3));
                    put("nodeE", new DecentralizedNodeInformation("nodeE", "10.2.1.72", "9005", 3));
                    put("nodeF", new DecentralizedNodeInformation("nodeF", "10.2.1.84", "9006", 4));
                    put("nodeG", new DecentralizedNodeInformation("nodeG", "10.2.1.74", "9007", 0));
                }}
        );
    }};

    private DecentralizedNodeInformationSingleton() {
    }

    public static synchronized DecentralizedNodeInformationSingleton getInstance() {
        if (DecentralizedNodeInformationSingleton.instance == null) {
            DecentralizedNodeInformationSingleton.instance = new DecentralizedNodeInformationSingleton();
        }
        return DecentralizedNodeInformationSingleton.instance;
    }
}
