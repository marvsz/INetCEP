package INetCEP.Operators;

import INetCEP.NFNQueryCreator;

import java.util.Collections;

public class OperatorJoin extends OperatorA {
    public OperatorJoin(String query) {
        super(query);
        this.isOperatorCreatingNode = true;
    }

    @Override
    public Boolean checkParameters() {
        // first parameter
        /*int index = 0;
        if (!isParamFormatNameOnIndex(index)) return false;
        // second parameter
        index = 1;
        if (!isParamFormatNameOnIndex(index)) return false;*/

        // third and fourth parameter
        for (int i = 0; i <= 1; i++) {
            Boolean min = false;
            // "at least one has to be true"
            min |= isParamFilterQueryOnIndex(i);
            min |= isParamWindowQueryOnIndex(i);
            min |= isParamJoinQueryOnIndex(i);
            min |= isParamPredict1QueryOnIndex(i);
            min |= isParamPredict2QueryOnIndex(i);
            min |= isParamHeatmapQueryOnIndex(i);
            if (!min) return false;
        }

        // each parameter is correct
        return true;
    }

    @Override
    public String genNFNQuery(String communicationAppraoch) {
        NFNQueryCreator nfn = null;
        switch (communicationAppraoch.toLowerCase()) {
            case "pra":
                nfn = new NFNQueryCreator("(call " + (this.parameters.length + 3) + " /node/nodeQuery/nfn_service_Join");
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
                nfn = new NFNQueryCreator("(call " + (this.parameters.length + 2) + " /node/nodeQuery/nfn_service_Join");
                nfn.parameters.add("ucl");
                Collections.addAll(nfn.parameters, this.parameters);
                break;
            default:
                break;
        }
        return nfn.getNFNQuery();
    }
}