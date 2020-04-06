package INetCEP;

import java.util.HashMap;

public class StatesSingleton {
    private static StatesSingleton instance;
    private HashMap<String, String> windowStates = new HashMap<String, String>();
    private HashMap<String, double[]> prediction2States = new HashMap<String, double[]>();

    /**
     * A synchronized singleton that holds state for an operator.
     * @return the Instance of the States Singleton.
     */
    public static synchronized StatesSingleton getInstance() {
        if(StatesSingleton.instance == null){
            StatesSingleton.instance = new StatesSingleton();
        }
        return StatesSingleton.instance;
    }

    /**
     *
     * @param operatorName The name of the operator.
     * @return The state that is stored for this operator.
     */
    public String getWindowState(String operatorName){
        return windowStates.get(operatorName);
    }

    /**
     *
     * @param operatorName
     * @return
     */
    public double[] getPredictionState(String operatorName){
        return prediction2States.get(operatorName);
    }

    /**
     *
     * @param operatorName The name of the operator.
     * @param state The state content that should be updated.
     */
    public void updateWindowState(String operatorName, String state){
        windowStates.put(operatorName,state);
    }

    public void updatePrediction2State(String operatorName, double[] state){
        prediction2States.put(operatorName,state);
    }
}
