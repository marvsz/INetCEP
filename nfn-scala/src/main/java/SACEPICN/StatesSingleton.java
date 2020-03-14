package SACEPICN;

import java.util.HashMap;

public class StatesSingleton {
    private static StatesSingleton instantce;
    public HashMap<String, String> states = new HashMap<String, String>();

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
    public String getState(String operatorName){
        return states.get(operatorName);
    }

    /**
     *
     * @param operatorName The name of the operator.
     * @param state The state content that should be updated.
     */
    public void updateState(String operatorName, String state){
        states.put(operatorName,state);
    }
}
