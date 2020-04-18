package INetCEP;

import java.util.ArrayList;
import java.sql.Timestamp;

public class NFNQueryCreator {

    private String call;
    public ArrayList<String> parameters;
    private Timestamp _timestamp = new Timestamp(System.currentTimeMillis());

    public NFNQueryCreator(String call) {
        this.call = call;
        this.parameters = new ArrayList<>();
    }

    public String appendNFNParameter(String f)
    {
        String call = f;
        // add each parameter as string
        if(this.parameters.contains("builtin")){
            call = "\"window";
            for(String p : this.parameters)
            {
                if(!(p.equals("ucl") || p.equals("builtin")))
                    if(p.equals("S"))
                        call += " 1";
                    else
                        if(p.contains("node"))
                            call += " /" + p;
                        else
                            call += " " + p;
            }
            // close
            call += "\"";
        }
        else {
            for (String p : this.parameters) {
                if(!(p.equals("scala")||p.equals("ucl")))
                    call += " '" + p + "'";
            }
            // close
            call += ")";
        }


        return call;
    }

    public String getNFNQuery() {
        return appendNFNParameter(call);
    }

    // SPECIAL FOR WINDOW OPERATOR

    public String appendNFNWindowParameter(String f)
    {
        String call = f;
        if(this.parameters.contains("builtin")){
            call = "\"window";
            for(String p : this.parameters)
            {
                if(p!="builtin")
                    call += " " + p;
            }
            call += "\"";
        }
        else{
            // add each parameter as string
            call += " {" + _timestamp.getTime() + "}";
            for (String p : this.parameters)
            {
                if(p!="scala")
                    call += " {" + p + "}";
            }

            // close
            call += ")";
        }


        return call;
    }

    public String getNFNQueryWindow() {
        return appendNFNWindowParameter(call);
    }

    public String getFlatNFNQueryWindow() {
        return appendNFNParameter(call);
    }


}