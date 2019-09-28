package SACEPICN;

import config.StaticConfig;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Sensor {
    String type;
    Character delimiter;
    String id;
    Set<String> schema;
    ArrayList<String> data;
    String sacepicnEnv = StaticConfig.systemPath();
    int tupleToRead = -1;

    public Sensor(Character _delimiter, String fileName) throws IOException {
        this.type = fileName.replaceAll("[^A-Za-z]+","");
        this.delimiter = _delimiter;
        this.id = fileName.replaceAll("\\D+",""); // Since the Sensor filenames are in the form of sensornameId e.g. victims1 we know that the first part is the type and the second part is the id
        getSchemaAndData(fileName);
    }

    /**
     * Reads the sensor file, gets the schema and the sensor file
     * @param fileName the name of the sensor file
     * @throws IOException
     */
    private void getSchemaAndData(String fileName) throws IOException {
        try(BufferedReader br = new BufferedReader(new FileReader(sacepicnEnv+"/sensors/" + fileName))){
            String line;
            this.schema = new HashSet<String>(Arrays.asList(br.readLine().split(delimiter.toString()))); // Ignore the first line which is the schema
            while ((line = br.readLine()) != null){
                this.data.add(line);
            }
        }
        catch (IOException e){
            System.err.println(e.getMessage());
        }
    }

    /**
     * Reads a tuple from the sensor
     * @return a tuple as a string
     */
    public String read(){
        tupleToRead++;
        return data.get(tupleToRead);
    }

    public Set<String> getSchema(){
        return this.schema;
    }

    public String getType(){
        return this.type;
    }

    public String getId(){
        return this.id;
    }
}
