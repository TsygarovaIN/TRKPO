import nio.Client;
import nio.ClientState;
import nio.Operand;
import nio.OperandType;
import nio.Result;
import nio.Server;
import nio.ServerState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nio.Server.ClientInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SystemTests {

    static int clientPortsCounter = 9001;
    static int serverPortsCounter = 9191;
    static int[] ports;
    static Server server;

    static final double EPS = 0.00000000001;

    static Operand op1 = new Operand(OperandType.COS, 10, OperandType.PLUS);
    static Operand op2 = new Operand(OperandType.EMPTY, 5, OperandType.MINUS);

    static Operand op4 = new Operand(OperandType.SQUARE, 100, OperandType.PLUS);
    static Operand op5 = new Operand(OperandType.EMPTY, 7, OperandType.MULT);
    static Operand op6 = new Operand(OperandType.ABS, -3, OperandType.EQUALS);

    static Operand op7 = new Operand(OperandType.EMPTY, 100, OperandType.PLUS);
    static Operand op8 = new Operand(OperandType.EMPTY, 50, OperandType.MINUS);
    static Operand op9 = new Operand(OperandType.LN, Math.E, OperandType.EQUALS);

    static List<Operand> partOfOperands = listOf(op1, op2, op4, op5);
    static List<Operand> operands1 = listOf(op4, op5, op6);
    static List<Operand> operands2 = new ArrayList<>();
    static List<Operand> operands3 = listOf(op7, op8, op9);

    static {
        for (int i = 0; i < 1000; i++) {
            operands2.addAll(partOfOperands);
        }
        operands2.add(op6);

    }

    private static List<Operand> listOf(Operand... operands) {
        return new ArrayList<>(Arrays.asList(operands));
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(1000);
    @Before
    public void startServer() {
        ports = new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++};
        server = new Server(ports, 4);
        Runnable serverRunnable = server::start;
        serverRunnable.run();
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    public void closeServer() {
        server.close();
    }

    @Test
    public void multiThreadingClientCalculate() {
        Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 4);
        for (int i = 0; i < 10; i++) {
            Runnable runnable = () -> {
                try {
                    assertEquals(51, client.calculate(operands1).get(), EPS);
                    assertEquals(3.2625158429879466, client.calculate(operands2).get(), EPS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
            runnable.run();
        }
    }

    @Test
    public void threeClientsCalculate() {
        try {
            Client client1 = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 4);
            Client client2 = new Client(new int[]{clientPortsCounter - 2}, serverPortsCounter++, 4);
            Client client3 = new Client(new int[]{clientPortsCounter - 3}, serverPortsCounter++, 4);
            assertEquals(51, client1.calculate(operands1).get(), EPS);
            assertEquals(3.2625158429879466, client2.calculate(operands2).get(), EPS);
            assertEquals(149, client3.calculate(operands3).get(), EPS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void cancelResult() {
        try {
            Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 1);
            Result result = client.calculate(operands1);
            client.cancelResult(result.getId());
            Assert.assertEquals(ClientState.CANCEL, result.getState());
            assertNull(server.getOperationsForClient(client.getClientId()));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test(expected = RuntimeException.class)
    public void cancelResultDontExist() {
        Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 1);
        client.calculate(operands1);
        try {
            client.cancelResult(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = NullPointerException.class)
    public void nullPorts() {
        Client client = new Client(null, 1000, 4);
    }

    @Test
    public void clientManyOperations() {
        try {
            Client client = new Client(new int[]{clientPortsCounter - 1, clientPortsCounter - 2, clientPortsCounter - 3}, serverPortsCounter++, 3);
            Result result1 = client.calculate(operands1);
            Result result2 = client.calculate(operands2);
            Result result3 = client.calculate(operands3);
            assertEquals(51, result1.get(), EPS);
            assertEquals(3.2625158429879466,result2.get(), EPS);
            assertEquals(149, result3.get(), EPS);
            ClientInfo clientInfo = server.getClientInfo(client.getClientId());
            Set<Integer> resultIds = new HashSet<>();
            resultIds.add(result1.getId());
            resultIds.add(result2.getId());
            resultIds.add(result3.getId());
            assertEquals(resultIds.size(), clientInfo.getResultIds().size());
            assertEquals(resultIds, clientInfo.getResultIds());
            ServerState serverState = server.getResultsMap().get(result2.getId()).getServerState();
            assertTrue(serverState == ServerState.DONE || serverState == ServerState.WAITING_TO_SEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void calculateAndCancelResult() {
        try {
            Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 1);
            Result result2 = client.calculate(operands2);
            assertEquals(3.2625158429879466,result2.get(), EPS);
            Result result = client.calculate(operands1);
            client.cancelResult(result.getId());
            Assert.assertTrue( result.getState() == ClientState.CANCEL || result.getState() == ClientState.SENDING);
            assertEquals(server.getResultsMap().get(result2.getId()).getServerState(), ServerState.DONE);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void twoClientsTestMultiThreading() {
        try {
            Client client1 = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 4);
            Client client2 = new Client(new int[]{clientPortsCounter - 1, clientPortsCounter - 2, clientPortsCounter - 3}, serverPortsCounter++, 4);
            Runnable r1 = () -> {
                try {
                    assertEquals(51, client1.calculate(operands1).get(), EPS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            };
            Runnable r2 = () -> {
                try {
                    assertEquals(51, client1.calculate(operands1).get(), EPS);
                    assertEquals(51, client1.calculate(operands1).get(), EPS);
                    assertEquals(3.2625158429879466, client2.calculate(operands2).get(), EPS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            };
            Runnable r3 = () -> {
                try {
                    assertEquals(149, client1.calculate(operands3).get(), EPS);
                    assertEquals(3.2625158429879466, client2.calculate(operands2).get(), EPS);
                    assertEquals(51, client2.calculate(operands1).get(), EPS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
            r1.run();
            r2.run();
            r3.run();
            Set<Integer> clientIds = new HashSet<>();
            clientIds.add(client1.getClientId());
            clientIds.add(client2.getClientId());
            assertEquals(clientIds, server.getClients());
        } catch (Exception e) {
            Assert.assertNull(e);
        }
    }

    @Test
    public void closeTest() {
        Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 3);
        Result result = client.calculate(operands1);
        client.close();
        assertEquals(ClientState.CLOSE, result.getState());
        ClientInfo clientInfo = server.getClientInfo(client.getClientId());
        assertTrue(clientInfo.isClosed());
    }

    @Test
    public void closeReopenTest() {
        try {
            Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 1);
            assertEquals(51, client.calculate(operands1).get(), EPS);
            client.close();
            client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 1);
            assertEquals(51, client.calculate(operands1).get(), EPS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void cancelAfterDone(){
        try {
            Client client = new Client(new int[]{ports[0], ports[1], ports[2]}, serverPortsCounter++, 3);
            Result result1 = client.calculate(operands1);
            Result result2 = client.calculate(operands2);
            Result result3 = client.calculate(operands3);
            result1.get();
            result2.get();
            result3.get();
            client.cancelResult(result1.getId());
            client.cancelResult(result2.getId());
            client.cancelResult(result3.getId());
            Assert.assertEquals(ClientState.DONE, result1.getState());
            Assert.assertEquals(ClientState.DONE, result2.getState());
            Assert.assertEquals(ClientState.DONE, result3.getState());
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }
}