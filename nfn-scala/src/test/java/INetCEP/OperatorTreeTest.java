package INetCEP;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class OperatorTreeTest {


    OperatorTree opTree;

    @BeforeEach
    public void setup() {
        this.opTree = new OperatorTree();
    }

    @AfterEach
    public void tearDown() {
        this.opTree = null;
    }

    @Test
    public void testCreateOperatorTree() throws Exception {

        Map result = opTree.createOperatorTree("WINDOW(node/nodeA/sensor/victims/1,4,S,scala)","ucl");
        assertNotNull(result);
        assertEquals(1, result._stackSize);

        result = opTree.createOperatorTree("WINDOW(node/nodeA/sensor/victims/1,4,S,builtin)","ucl");
        assertNotNull(result);
        assertEquals(1, result._stackSize);

        result = opTree.createOperatorTree("WINDOW(node/nodeA/sensor/victims/1,4,S,scala,500,MS)","pra");
        assertNotNull(result);
        assertEquals(1, result._stackSize);

        result = opTree.createOperatorTree("FILTER(WINDOW(node/nodeA/sensor/victims/1,4,S,scala),Gender=M&Age<15)","ucl");
        assertNotNull(result);
        assertEquals(2, result._stackSize);

        result = opTree.createOperatorTree("FILTER(WINDOW(node/nodeA/sensor/victims/1,4,S,builtin),Gender=M&Age<15)","ucl");
        assertNotNull(result);
        assertEquals(2, result._stackSize);

        result = opTree.createOperatorTree("FILTER(WINDOW(node/nodeA/sensor/victims/1,4,S,scala,500,MS),Gender=M&Age<15)","pra");
        assertNotNull(result);
        assertEquals(2, result._stackSize);

        result = opTree.createOperatorTree("JOIN(FILTER(WINDOW(node/nodeA/sensor/victims/1,4,S,scala),Gender=M&Age<15),FILTER(WINDOW(node/nodeA/sensor/victims/2,4,S,scala),Gender=F&Age>30),time,none,inner)","ucl");
        assertNotNull(result);
        assertEquals(5, result._stackSize);

        result = opTree.createOperatorTree("JOIN(FILTER(WINDOW(node/nodeA/sensor/victims/1,4,S,builtin),Gender=M&Age<15),FILTER(WINDOW(node/nodeA/sensor/victims/2,4,S,builtin),Gender=F&Age>30),time,none,inner)","ucl");
        assertNotNull(result);
        assertEquals(5, result._stackSize);

        result = opTree.createOperatorTree("JOIN(FILTER(WINDOW(node/nodeA/sensor/victims/1,4,S,scala,500,MS),Gender=M&Age<15),FILTER(WINDOW(node/nodeA/sensor/victims/2,4,S,scala,500,MS),Gender=F&Age>30),time,none,inner)","pra");
        assertNotNull(result);
        assertEquals(5, result._stackSize);

        result = opTree.createOperatorTree("HEATMAP(0.0015,8.7262659072876,8.8215389251709,51.7832946777344,51.8207664489746,JOIN(WINDOW(node/nodeA/sensor/gps/1,5,S,scala),WINDOW(node/nodeA/sensor/gps/2,5,S,scala),date,none,innerjoin))","ucl");
        assertNotNull(result);
        assertEquals(4, result._stackSize);

        result = opTree.createOperatorTree("HEATMAP(0.0015,8.7262659072876,8.8215389251709,51.7832946777344,51.8207664489746,JOIN(WINDOW(node/nodeA/sensor/gps/1,5,S,builtin),WINDOW(node/nodeA/sensor/gps/2,5,S,builtin),date,none,innerjoin))","ucl");
        assertNotNull(result);
        assertEquals(4, result._stackSize);

        result = opTree.createOperatorTree("HEATMAP(0.0015,8.7262659072876,8.8215389251709,51.7832946777344,51.8207664489746,JOIN(WINDOW(node/nodeA/sensor/gps/1,5,S,scala,500,MS),WINDOW(node/nodeA/sensor/gps/2,5,S,scala,500,MS),date,none,innerjoin))","pra");
        assertNotNull(result);
        assertEquals(4, result._stackSize);

        result = opTree.createOperatorTree("FILTER(JOIN(PREDICT2(30s,WINDOW(node/nodeA/sensor/plug/1,5,S,scala)),PREDICT2(30s,WINDOW(node/nodeA/sensor/plug/1,5,S,scala)),date,fullouter,none),Value>50)","ucl");
        assertNotNull(result);
        assertEquals(6, result._stackSize);

        result = opTree.createOperatorTree("FILTER(JOIN(PREDICT2(30s,WINDOW(node/nodeA/sensor/plug/1,5,S,builtin)),PREDICT2(30s,WINDOW(node/nodeA/sensor/plug/1,5,S,builtin)),date,fullouter,none),Value>50)","ucl");
        assertNotNull(result);
        assertEquals(6, result._stackSize);

        result = opTree.createOperatorTree("FILTER(JOIN(PREDICT2(30s,WINDOW(node/nodeA/sensor/plug/1,5,S,scala,500,MS)),PREDICT2(30s,WINDOW(node/nodeA/sensor/plug/1,5,S,scala,500,MS)),date,fullouter,none),Value>50)","pra");
        assertNotNull(result);
        assertEquals(6, result._stackSize);

        // here a wrong query testing, for missing closing parantheis/brackets/braces
        // result = opTree.createOperatorTree("JOIN([name],[FILTER(name,FILTER(name,WINDOW(victims,22:18:36.800,22:18:44.001),3=M&4>30,name),3=M&4>30,name)],[FILTER(name,WINDOW(survivors,22:18:35.800,22:18:41.001),3=F&4>20,name)],[NULL])");
        // assertNotNull(result);
        // assertEquals(result._stackSize, 2);
    }




    // @Test
    // public void parseNode() throws Exception {
    // }

    // @Test
    // public void getOperator() throws Exception {
    // }

    

    // @Test
    // public void getNestedQueries() throws Exception {
    // }

    // @Test
    // public void isOperatorEvaluatingParams() throws Exception {
    // }
}