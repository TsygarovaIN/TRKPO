package nio;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientTest {
    static int portsCounter = 7000;
    static int[] ports;
    static int serverPort = 7070;
    static int serverThreads = 5;
    static Client client;
    static Server server;

    static Operand op1 = new Operand(OperandType.COS, 10, OperandType.PLUS);
    static Operand op2 = new Operand(OperandType.EMPTY, 5, OperandType.MINUS);
    static Operand op3 = new Operand(OperandType.SQUARE, 100, OperandType.PLUS);
    static Operand op4 = new Operand(OperandType.EMPTY, 7, OperandType.MULT);
    static Operand op5 = new Operand(OperandType.ABS, -3, OperandType.EQUALS);

    static List<Operand> operands = listOf(op1, op2, op3, op4, op5);
    static List<Operand> partOfOperands = listOf(op1, op2, op3, op4);
    static List<Operand> operands2 = new ArrayList<>();

    static {
        for (int i = 0; i < 1000; i++) {
            operands2.addAll(partOfOperands);
        }
        operands2.add(op5);
    }

    private static List<Operand> listOf(Operand... operands) {
        return new ArrayList<>(Arrays.asList(operands));
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(20);

    @Before
    public void init() {
        ports = new int[]{++portsCounter, ++portsCounter, ++portsCounter};
        server = new Server(ports, serverThreads);
        Runnable serverRunnable = server::start;
        serverRunnable.run();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            e.printStackTrace();

        }
        client = new Client(ports, ++serverPort, 4);
    }

    @After
    public void closeAll() {
        client.close();
        server.close();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void singleClientCalculate() {
        client.close();
        client = new Client(new int[]{portsCounter - 1}, serverPort, 1);
        try {
            Assert.assertEquals(3.48278, client.calculate(operands).get(), 0.001);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void multiClientCalculate() {
        try {
            Assert.assertEquals(3.48278, client.calculate(operands).get(), 0.001);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void singleOperandCalculate() {
        try {
            Assert.assertEquals(3, client.calculate(listOf(op5)).get(), 0.001);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidOperandCalculate() {
        Client client = new Client(new int[]{ports[1]}, ++serverPort, 1);
        try {
            Assert.assertEquals(Double.NaN, client.calculate(listOf(op1, op2, op3)).get(), 0.001);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void multiClientCalculateHugeOperands() {
        try {
            Assert.assertEquals(3.26251584, client.calculate(operands2).get(), 0.001);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void multiThreadingClientCalculate() {
        for (int i = 0; i < 10; i++) {
            Runnable runnable = () -> {
                try {
                    Assert.assertEquals(3.48278, client.calculate(operands).get(), 0.001);
                    Assert.assertEquals(3.26251584, client.calculate(operands2).get(), 0.001);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
            runnable.run();
        }
    }

    @Test
    public void nonNullCalculate() {
        Assert.assertNotNull(client.calculate(operands));
    }

    @Test
    public void twoClientCalculate() {
        Client client2 = new Client(ports, ++serverPort, 2);
        try {
            Assert.assertEquals(3.48278, client.calculate(operands).get(), 0.001);
            Assert.assertEquals(3.26251584, client2.calculate(operands2).get(), 0.001);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void cancelResult() {
        try {
            for (int i = 0; i < 3 * serverThreads; i++) {
                client.calculate(operands);
            }
            Result result = client.calculate(operands);
            client.cancelResult(result.getId());
            Assert.assertEquals(ClientState.CANCEL, result.getState());
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Test
    public void cancelDoneResult(){
        try {
            Result result = client.calculate(operands);
            result.get();
            client.cancelResult(result.getId());
            Assert.assertEquals(ClientState.DONE, result.getState());
        } catch (IOException | InterruptedException e){
            e.printStackTrace();

        }
    }

    @Test
    public void cancelResultDontExist() {
        client.calculate(operands);
        try {
            client.cancelResult(-1);
        } catch (Exception throwable) {
            Assert.assertTrue(throwable instanceof RuntimeException);
            Assert.assertEquals("There is no result with id = " + "-1", throwable.getMessage());
        }
    }

    @Test
    public void getResult() {
        Result result = client.calculate(operands);
        try {
            Assert.assertEquals(3.48278, client.getResult(result.getId()).get(), 0.001);
        } catch (Exception throwable) {
            Assert.assertNull(throwable);
        }
    }

    @Test
    public void getResultMultithreading() {
        List<Result> results = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 10; i++) {
            Runnable runnable = () -> {
                results.add(client.calculate(operands));
            };
            runnable.run();
        }
        try {
            for (int i = 0; i < 10; i++) {
                Assert.assertEquals(3.48278, client.getResult(results.get(i).getId()).get(), 0.001);
            }
        } catch (Exception throwable) {
            Assert.assertNull(throwable);
        }
    }


    @Test
    public void getResultDontExist() {
        client.calculate(operands);
        try {
            client.getResult(-1);
        } catch (Exception throwable) {
            Assert.assertEquals("There is no result with id = " + "-1", throwable.getMessage());
        }
    }

    @Test
    public void closeReopenTest() {
        try {
            client.close();
            client = new Client(ports, serverPort, 4);
            Assert.assertEquals(3.48278, client.calculate(operands).get(), 0.001);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void closeTest() {
        client.close();
        Assert.assertTrue(client.isClosed());
    }

    @Test(expected = NullPointerException.class)
    public void nullOperands() {
        client.calculate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nEqualsOperands() {
        List<Operand> operandss = new ArrayList<>();
        operandss.add(op1);
        client.calculate(operandss);
    }

    @Test(expected = NullPointerException.class)
    public void nullPorts() {
        Client client = new Client(null, 1000, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negPorts() {
        Client client = new Client(new int[]{-1, -2}, 1000, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negServerPort() {
       Client client = new Client(new int[]{1000}, -1000, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negThreadsCount() {
        Client client = new Client(new int[]{1000}, 1000, -4);
    }

    @Test()
    public void resultMapCheck() {
        Result result = client.calculate(operands);
        Assert.assertTrue(client.getResultMap().containsKey(result.getId()));
    }
}