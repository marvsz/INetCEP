package SACEPICN;

import scala.Array;
import scala.Double;

import java.util.HashMap;

public class StatesSingleton {
    private static StatesSingleton instantce;
    private HashMap<String, String> windowStates = new HashMap<String, String>();
    private HashMap<String, Double[]> prediction2States = new HashMap<String, Double[]>();

    /**
     * A synchronized singleton that holds state for an operator.
     * @return the Instance of the States Singleton.
     */
    public static synchronized StatesSingleton getInstance() {
        if(StatesSingleton.instantce == null){
            StatesSingleton.instantce = new StatesSingleton();
        }
        return StatesSingleton.instantce;
    }

    /**
     *
     * @param operatorName The name of the operator.
     * @return The state that is stored for this operator.
     */
    public String getWindowState(String operatorName){
        return windowStates.get(operatorName);
    }

    public Double[] getPrediction2State(String operatorName){
        return prediction2States.get(operatorName);
    }

    /**
     *
     * @param operatorName The name of the operator.
     * @param state The state content that should be updated.
     */
    public void updateState(String operatorName, String state){
        windowStates.put(operatorName,state);
    }
}
