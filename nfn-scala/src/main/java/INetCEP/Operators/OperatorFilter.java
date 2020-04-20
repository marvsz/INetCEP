package INetCEP.Operators;

import INetCEP.NFNQueryCreator;

import java.util.Collections;
import java.util.HashMap;

public class OperatorFilter extends OperatorA {
    String query;

    public OperatorFilter(String query) {
        super(query);
        this.isOperatorCreatingNode = true;
        this.query = query;
    }

    /**
     * @Overriden
     */
    public Boolean checkParameters() {

        // first parameter
        //int index = 0;
        //if (!isParamFormatNameOnIndex(index)) return false;
        // third parameter
        int index = 0;
        // "at least one has to be true"
        Boolean min = false;
        min = isParamFilterQueryOnIndex(index);
        min |= isParamWindowQueryOnIndex(index);
        min |= isParamJoinQueryOnIndex(index);
        min |= isParamPredict1QueryOnIndex(index);
        min |= isParamPredict2QueryOnIndex(index);
        min |= isParamHeatmapQueryOnIndex(index);
        if (!min) return false;

        index = 1;
        if (!isParamBoolExp(index) || this.parameters[index].contains("NULL")) return false;
        // mapping for expressiveness
        //this.parameters[index] = performExpressivenessHandling(this.parameters[index]);

        /*index = 3;
        if (!isParamFormatNameOnIndex(index)) return false;*/

        // each parameter is correct
        return true;
    }

    public String genNFNQuery(String communicationAppraoch) {
        NFNQueryCreator nfn = null;
        switch (communicationAppraoch.toLowerCase()) {
            case "pra":
                nfn = new NFNQueryCreator("(call " + (this.parameters.length + 3) + " /node/nodeQuery/nfn_service_Filter");
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
                nfn = new NFNQueryCreator("(call " + (this.parameters.length + 2) + " /node/nodeQuery/nfn_service_Filter");
                nfn.parameters.add("ucl");
                Collections.addAll(nfn.parameters, this.parameters);
                break;
            default:
                break;
        }

        // add all parameter


        return nfn.getNFNQuery();
    }


    private String performExpressivenessHandling(String boolExp) {
        HashMap<String, String> mapping = new HashMap<String, String>();
        mapping.put("time", "0");
        mapping.put("id", "1");
        mapping.put("food", "1");
        mapping.put("gender", "2");
        mapping.put("shelter", "3");
        mapping.put("age", "3");
        mapping.put("medicaltreatmeant", "4");
        mapping.put("basichygiene", "5");

        String theExp = boolExp.toLowerCase();

        for (String k : mapping.keySet()) {
            theExp = theExp.replaceAll(k, mapping.get(k));
        }

        return theExp;
    }

}