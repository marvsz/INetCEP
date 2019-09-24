package SACEPICN.Operators;

import SACEPICN.NFNQueryCreator;

import java.sql.Timestamp;

public class OperatorPredict2 extends OperatorA {
    public OperatorPredict2(String query) {
        super(query);
        this.isOperatorCreatingNode = false;
    }

    public Boolean checkParameters() {

        return true;
    }

    @Override
    public String genNFNQuery() {
        NFNQueryCreator nfn = new NFNQueryCreator("(call " + (this.parameters.length+2) + " /node/nodeQuery/nfn_service_Prediction2");
        // add all parameter
        int counter = 1;
        for (int i = 0; i < this.parameters.length; i++)
        {
            if (isParamNestedQuery(i)) {
                nfn.parameters.add("[Q" + counter++ + "]");
            } else {
                nfn.parameters.add(this.parameters[i]);
            }
        }

        return nfn.getNFNQuery();
    }

}