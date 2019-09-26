package SACEPICN;

import lambdacalculus.parser.ast.Str;
import scala.util.parsing.combinator.token.StdTokens;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
Created by Johannes on 26.9.2019
 */

public class SchemaBrokerSingleton {
    private static SchemaBrokerSingleton instance;
    public HashMap<String, Set<String>> schemes = new HashMap<String,Set<String>>();

    public SchemaBrokerSingleton(){
        Set<String> survivorSet = new HashSet<>();
        survivorSet.add("Date");
        survivorSet.add("SequenceNumber");
        survivorSet.add("Gender");
        survivorSet.add("Age");
        schemes.put("Survivors",survivorSet);

        Set<String> victimSet = new HashSet<>();
        victimSet.add("Date");
        victimSet.add("SequenceNumber");
        victimSet.add("Gender");
        victimSet.add("Age");
        schemes.put("Victims",victimSet);

        Set<String> gpsSet = new HashSet<>();
        gpsSet.add("Date");
        gpsSet.add("Identifier");
        gpsSet.add("Latitude");
        gpsSet.add("Longitude");
        gpsSet.add("Altitude");
        gpsSet.add("Accuracy");
        gpsSet.add("Distance");
        gpsSet.add("Speed");
        schemes.put("GPS",gpsSet);

        Set<String> plugSet = new HashSet<>();
        plugSet.add("SequenceNumber");
        plugSet.add("Date");
        plugSet.add("Value");
        plugSet.add("Property");
        plugSet.add("Plug_ID");
        plugSet.add("Household_ID");
        plugSet.add("House_ID");
        schemes.put("plug",plugSet);
    }

    /**
     * returns the SchemaBrokerSingleton
     * @return the Instance of the Schema Broker.
     */
    public static synchronized SchemaBrokerSingleton getInstance(){
        if(SchemaBrokerSingleton.instance == null){
            SchemaBrokerSingleton.instance = new SchemaBrokerSingleton();
        }
        return SchemaBrokerSingleton.instance;
    }

    /**
     * Inserts a new schema into the list of existing schemas if it does not already exist.
     * @param schemaName the name of the new schema
     * @param columnNames a set of strings representing the new column names of the schema
     * @return true iff the new schema did not already exist, false otherwise
     */
    public boolean insertSchema(String schemaName,Set<String> columnNames){
        if(schemes.get(schemaName) == null){
            schemes.put(schemaName,columnNames);
            return true;
        }
        else
            return false;
    }

    /**
     * Returns the schema for a given name
     * @param schemaName the Name for which we want the schema
     * @return a set of column names representing the schema, null if it does not exist.
     */
    public Set<String> getSchema(String schemaName){
        return schemes.get(schemaName);
    }

    /**
     * Removes the schema from the list of schemas.
     * If the schema did not exist it returns false, if it did exist it returns true. Might be somewhat interesting for
     * the caller to know.
     * @param schemaName the schema to remove
     * @return true if the schema was originally present in the schema list, false if it was not present
     */
    public boolean removeSchema(String schemaName){
        if(schemes.remove(schemaName)!=null)
            return true;
        return false;
    }

    /**
     * Joins the schemas of two data streams.
     * When a join is called on two data streams, we have to also join the schemas in order to be able to still carry out filter operations.
     * The New Entry is in the form of Join(schema1,schema2|joinOn) since  it provides us with a new schema for the joined streams.
     * Both schemas must already exist in the schema lilst, the column name must be present in both schemas and he new entry must not already exist.
     * If any of these requirements is violated, we cannot perform the operation and return false.
     *
     * TODO: Maybe throw meaningful exceptions in order to distinguish between what exactly went wrong.
     *
     * @param joinOn the column name on which to join on
     * @param schema1 the name of the first sensor that provides a data stream
     * @param schema2 the name of the second sensor that provides a data stream
     * @return true iff joining was successfull and a new schema was inserted into the schema list, flase otherwise
     */
    public boolean joinSchema(String joinOn, String schema1, String schema2){
        Set<String> columnNames1 = getSchema(schema1);
        Set<String> columnNames2 = getSchema(schema2);
        String newSchemaName="Join(".concat(schema1).concat(",").concat(schema2).concat("|").concat(joinOn).concat(")");
        if(columnNames1 != null && columnNames2 != null){
            if(columnNames1.contains(joinOn)&&columnNames2.contains(joinOn)){
                Set<String> newColumnNames = new HashSet<>();
                for(String cName : columnNames1){
                    if(cName.equals(joinOn))
                        newColumnNames.add(cName);
                    else
                        newColumnNames.add(schema1.concat("_").concat(cName));
                }
                for(String cName: columnNames2){
                    if(!cName.equals(joinOn))
                        newColumnNames.add(schema2.concat("_").concat(cName));
                }
                return insertSchema(newSchemaName, newColumnNames);
            }
        }
        return false;
    }
}
