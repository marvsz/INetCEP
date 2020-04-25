package INetCEP.Operators;

import INetCEP.NFNQueryCreator;

import java.util.Collections;

public class OperatorHeatmap extends OperatorA {
    public OperatorHeatmap(String query) {
        super(query);
        this.isOperatorCreatingNode = true;
    }

    public Boolean checkParameters() {
        return true;
    }

    public String genNFNQuery(String communicationAppraoch) {
        NFNQueryCreator nfn = null;
        switch (communicationAppraoch.toLowerCase()) {
            case "pra":
                nfn = new NFNQueryCreator("(call " + (this.parameters.length + 3) + " /node/nodeQuery/nfn_service_Heatmap");
                int counter = 1;
                nfn.parameters.add("pra");
                for (int i = 0; i < this.parameters.length; i++) {
                    if (isParamNestedQuery(i)) {
                        nfn.parameters.add("[Q" + counter++ + "]");
                    } else {
                        nfn.parameters.add(this.parameters[i]);
                    }
                }
                break;
            case "ucl":
                nfn = new NFNQueryCreator("(call " + (this.parameters.length + 2) + " /node/nodeQuery/nfn_service_Heatmap");
                nfn.parameters.add("ucl");
                Collections.addAll(nfn.parameters, this.parameters);
                break;
            default:
                break;
        }

        // add all parameter


        return nfn.getNFNQuery();
    }
}
