package runnables.ExampleRunner;

//import runnables.javaservice.JavaWordCount;
import runnables.servicegenerator.ServiceGenerator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExampleRunner {

    public static void main(String[] args) {
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        /*logger.setLevel(Level.ALL);

        ServiceGenerator sg = new ServiceGenerator();
        JavaWordCount jvcService = new JavaWordCount();
        byte[] bc = sg.generateService(jvcService);

        List<String> cmpsList = (List<String>) jvcService.ccnName().cmpsList();
        StringBuilder sb = new StringBuilder();


        for (int i = 0; i < cmpsList.size(); i++) {
            if(i != 0) { sb.append("_"); }
            sb.append(cmpsList.get(i));
        }
        sb.append(".jar");

        String filename = "./service-library/" + sb.toString();
        File f = new File(filename);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(bc);
            bos.flush();
        } catch (IOException e) {
            logger.severe(e.toString());
        } finally {
            if(fos != null) try {
                fos.close();
            } catch (IOException e) {
                logger.severe(e.toString());
            }
        }*/
        //System.out.print("wrote bc size=" + bc.length + " to " + filename);
    }
}
