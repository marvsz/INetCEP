package INetCEP;

import org.junit.jupiter.api.Test;


public class TestOperatorTree {
    @Test
    public void testOpTreePrediction1(){
        String query = "PREDICT1({name},{name},{2m},{JOIN([name],[name],[WINDOW(plug1,08:00:00.000,08:05:00.000)],[WINDOW(plug2,08:00:00.000,08:05:00.000)],[NULL])})";
        OperatorTree opt = new OperatorTree();
        //Map expectedOperatorTree = new Map();
        //Map operatorTree = opt.createOperatorTree(query);
    }
    @Test
    public void testGetPredictionParameters(){
        String query="PREDICT1({name},{name},{2m},{JOIN([name],[name],[WINDOW(plug1,08:00:00.000,08:05:00.000)],[WINDOW(plug2,08:00:00.000,08:05:00.000)],[NULL])})";
        OperatorTree opt = new OperatorTree();
        String[] expectedParams = new String[4];
        expectedParams[0] = "name";
        expectedParams[1] = "name";
        expectedParams[2] = "2m";
        expectedParams[3] = "JOIN([name],[name],[WINDOW(plug1,08:00:00.000,08:05:00.000)],[WINDOW(plug2,08:00:00.000,08:05:00.000)],[NULL])";
        //String[] predictionParameters = opt.getPredictionParameters(query);
        //Assert.assertArrayEquals(expectedParams,predictionParameters);
        //assertEquals(expectedParams,predictionParameters);
    }
    @Test
    public void testGetJoinParameters(){
        String query="JOIN([name],[name],[WINDOW(plug1,08:00:00.000,08:05:00.000)],[WINDOW(plug2,08:00:00.000,08:05:00.000)],[NULL])";
        OperatorTree opt = new OperatorTree();
        String[] expectedParams = new String[5];
        expectedParams[0] = "name";
        expectedParams[1] = "name";
        expectedParams[2] = "WINDOW(plug1,08:00:00.000,08:05:00.000)";
        expectedParams[3] = "WINDOW(plug2,08:00:00.000,08:05:00.000)";
        expectedParams[4] = "NULL";
        //String[] joinParameters = opt.getJoinParameters(query);
        //Assert.assertArrayEquals(expectedParams,joinParameters);
    }
    @Test
    public void testGetWindowParameters(){
        String query = "WINDOW(plug1,08:00:00.000,08:05:00.000)";
        OperatorTree opt = new OperatorTree();
        String[] expectedParams = new String[3];
        expectedParams[0] = "plug1";
        expectedParams[1] = "08:00:00.000";
        expectedParams[2] = "08:05:00.000";
    }
    @Test
    public void testOpTreeHeatmap(){
        String query = "HEATMAP({name},{name},{0.0015},{8.8215389251709},{8.7262659072876},{51.7832946777344},{51.8207664489746},{JOIN([name],[name],[WINDOW(gps1,15:23:00.000,15:24:27.000)],[WINDOW(gps2,15:23:00.000,15:24:27.000)],[NULL])})";
        OperatorTree opt = new OperatorTree();
        //Map expectedOperatorTree = new Map();
        //Map operatorTree = opt.createOperatorTree(query);
    }
    @Test
    public void testGetHeatmapParameters(){
        String query = "HEATMAP({name},{name},{0.0015},{8.8215389251709},{8.7262659072876},{51.7832946777344},{51.8207664489746},{JOIN([name],[name],[WINDOW(gps1,15:23:00.000,15:24:27.000)],[WINDOW(gps2,15:23:00.000,15:24:27.000)],[NULL])})";
        OperatorTree opt = new OperatorTree();
        String[] expectedParams = new String[8];
        expectedParams[0] = "name";
        expectedParams[1] = "name";
        expectedParams[2] = "0.0015";
        expectedParams[3] = "8.8215389251709";
        expectedParams[4] = "8.7262659072876";
        expectedParams[5] = "51.7832946777344";
        expectedParams[6] = "51.8207664489746";
        expectedParams[7] = "JOIN([name],[name],[WINDOW(gps1,15:23:00.000,15:24:27.000)],[WINDOW(gps2,15:23:00.000,15:24:27.000)],[NULL])";
        //String[] heatmapParams = opt.getHeatmapParameters(query);
        //Assert.assertArrayEquals(expectedParams,heatmapParams);
    }
}
