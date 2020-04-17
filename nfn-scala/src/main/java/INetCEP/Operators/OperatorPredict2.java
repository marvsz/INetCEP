package INetCEP.Operators;

import INetCEP.NFNQueryCreator;

public class OperatorPredict2 extends OperatorA {
    public OperatorPredict2(String query) {
        super(query);
        this.isOperatorCreatingNode = true;
    }

    public Boolean checkParameters() {

        return true;
    }

    @Override
    public String genNFNQuery() {
        NFNQueryCreator nfn = new NFNQueryCreator("(call " + (this.parameters.length+1) + " /node/nodeQuery/nfn_service_Prediction2");
        // add all parameter
        int counter = 1;
        for (int i = 0; i < this.parameters.length; i++)
        {
            if (isParamNestedQuery(i)) {
                //nfn.parameters.add("[Q" + counter++ + "]");
                nfn.parameters.add(this.parameters[i]);
            } else {
                nfn.parameters.add(this.parameters[i]);
            }
        }

        return nfn.getNFNQuery();
    }

}