package INetCEP.Operators;

import INetCEP.NFNQueryCreator;

import java.util.Collections;

public class OperatorWindow extends OperatorA {
    public OperatorWindow(String query) {
        super(query);
        this.isOperatorCreatingNode = true;
    }

    @Override
    public Boolean checkParameters() {
        // first param
        /*Integer index = 0;
        if (!isParamFormatNameOnIndex(index));*/

        return true;
    }

    @Override
    public String genNFNQuery(String communicationAppraoch) {
        NFNQueryCreator nfn = prepareNfn(communicationAppraoch);
        return nfn.getNFNQuery();
    }

    @Override
    public String genFlatNFNQuery(String communicationAppraoch) {
        NFNQueryCreator nfn = prepareNfn(communicationAppraoch);
        return nfn.getNFNQueryWindow();
    }

    private NFNQueryCreator prepareNfn(String communicationAppraoch) {
        NFNQueryCreator nfn = null;
        switch (communicationAppraoch.toLowerCase()) {
            case "pra":
                nfn = new NFNQueryCreator("(call " + (this.parameters.length + 2) + " /node/nodeQuery/nfn_service_Window");
                nfn.parameters.add("pra");
                int counter = 1;
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
                nfn = new NFNQueryCreator("(call " + (this.parameters.length + 1) + " /node/nodeQuery/nfn_service_Window");
                nfn.parameters.add("ucl");
                Collections.addAll(nfn.parameters, this.parameters);
                break;
            default:
                break;
        }
        return nfn;
    }
}