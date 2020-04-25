package INetCEP.Operators;

import INetCEP.NFNQueryCreator;

import java.util.Collections;

public class OperatorSequence extends OperatorA {
    String query;

    public OperatorSequence(String query) {
        super(query);
        this.isOperatorCreatingNode = true;
        this.query = query;
    }

    /**
     * @Overriden
     */
    public Boolean checkParameters() {
        return true;
    }

    /**
     * @Overriden
     */
    public String genNFNQuery(String communicationAppraoch) {
        NFNQueryCreator nfn = null;
        switch (communicationAppraoch.toLowerCase()) {
            case "pra":
                nfn = new NFNQueryCreator("(call " + (this.parameters.length + 3) + " /node/nodeQuery/nfn_service_Sequence");
                int counter = 1;
                nfn.parameters.add("pra");
                // add all parameters except for nested queries (maximum 1)
                for (int i = 0; i < this.parameters.length; i++) {
                    if (isParamNestedQuery(i)) {
                        nfn.parameters.add("[Q" + counter++ + "]");
                    } else {
                        nfn.parameters.add(this.parameters[i]);
                    }
                }
                break;
            case "ucl":
                // add all parameters
                nfn = new NFNQueryCreator("(call " + (this.parameters.length + 2) + " /node/nodeQuery/nfn_service_Sequence");
                nfn.parameters.add("ucl");
                Collections.addAll(nfn.parameters, this.parameters);
                break;
            default:
                break;
        }
        return nfn.getNFNQuery();
    }

}