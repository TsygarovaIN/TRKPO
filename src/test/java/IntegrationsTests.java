import nio.Client;
import nio.ClientState;
import nio.Operand;
import nio.OperandType;
import nio.Result;
import nio.Server;
import nio.ServerOperation;
import nio.ServerState;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static nio.Server.ClientInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class IntegrationsTests {

    static int clientPortsCounter = 8101;
    static int serverPortsCounter = 8181;

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
    public Timeout globalTimeout = Timeout.seconds(20);

    @Test
    public void simpleTest() {
        try {
            Server server = new Server(new int[]{clientPortsCounter}, 4);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
            Client client = new Client(new int[]{clientPortsCounter++}, serverPortsCounter++, 4);
            assertEquals(51, client.calculate(operands1).get(), 0.000000001);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void multiClientsTest() {
        try {
            int[] ports = new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++};
            Server server = new Server(ports, 4);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
            Client client1 = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 4);
            Client client2 = new Client(new int[]{clientPortsCounter - 2}, serverPortsCounter++, 4);
            Client client3 = new Client(new int[]{clientPortsCounter - 3}, serverPortsCounter++, 4);
            assertEquals(51, client1.calculate(operands1).get(), 0.000000001);
            assertEquals(3.2625158429879466, client2.calculate(operands2).get(), 0.000000001);
            assertEquals(149, client3.calculate(operands3).get(), 0.000000001);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void threeClientsTestMultiThreading() {
        try {
            Server server = new Server(new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++}, 4);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
            Client client1 = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 4);
            Client client2 = new Client(new int[]{clientPortsCounter - 1, clientPortsCounter - 2, clientPortsCounter - 3}, serverPortsCounter++, 4);
            Client client3 = new Client(new int[]{clientPortsCounter - 2, clientPortsCounter - 4, clientPortsCounter - 5}, serverPortsCounter++, 4);
            Runnable r1 = () -> {
                try {
                    assertEquals(51, client1.calculate(operands1).get(), 0.000000001);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            };
            Runnable r2 = () -> {
                try {
                    assertEquals(51, client1.calculate(operands1).get(), 0.000000001);
                    assertEquals(51, client1.calculate(operands1).get(), 0.000000001);
                    assertEquals(3.2625158429879466, client2.calculate(operands2).get(), 0.000000001);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            };
            Runnable r3 = () -> {
                try {
                    assertEquals(149, client1.calculate(operands3).get(), 0.000000001);
                    assertEquals(51, client3.calculate(operands1).get(), 0.000000001);
                    assertEquals(149, client3.calculate(operands3).get(), 0.000000001);
                    assertEquals(3.2625158429879466, client2.calculate(operands2).get(), 0.000000001);
                    assertEquals(51, client2.calculate(operands1).get(), 0.000000001);
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
            clientIds.add(client3.getClientId());
            assertEquals(clientIds, server.getClients());
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void oneClientManyOpsTest() {
        try {
            int[] ports = new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++};
            Server server = new Server(ports, 4);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
            Client client = new Client(new int[]{clientPortsCounter - 1, clientPortsCounter - 2, clientPortsCounter - 3}, serverPortsCounter++, 3);
            Result result1 = client.calculate(operands1);
            Result result2 = client.calculate(operands2);
            Result result3 = client.calculate(operands3);
            assertEquals(51, result1.get(), 0.000000001);
            assertEquals(3.2625158429879466, result2.get(), 0.000000001);
            assertEquals(149, result3.get(), 0.000000001);
            ClientInfo clientInfo = server.getClientInfo(client.getClientId());
            Set<Integer> resultIds = new HashSet<>();
            resultIds.add(result1.getId());
            resultIds.add(result2.getId());
            resultIds.add(result3.getId());
            assertEquals(resultIds.size(), clientInfo.getResultIds().size());
            assertEquals(resultIds, clientInfo.getResultIds());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void threeClientManyOpsTest() {
        try {
            int[] ports = new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++};
            Server server = new Server(ports, 4);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
            Client client1 = new Client(new int[]{ports[0]}, serverPortsCounter++, 3);
            Client client2 = new Client(new int[]{ports[1]}, serverPortsCounter++, 3);
            Client client3 = new Client(new int[]{ports[2]}, serverPortsCounter++, 3);
            Result result1 = client1.calculate(operands1);
            Result result2 = client1.calculate(operands2);
            Result result3 = client1.calculate(operands3);
            Result result21 = client2.calculate(operands1);
            Result result22 = client2.calculate(operands2);
            Result result31 = client3.calculate(operands1);
            result21.get();
            result22.get();
            client2.close();
            result31.get();
            assertEquals(51, result1.get(), 0.000000001);
            assertEquals(3.2625158429879466, result2.get(), 0.000000001);
            assertEquals(149, result3.get(), 0.000000001);
            assertEquals(6, server.getResultsMap().size());
            assertEquals(operands1.size(), server.getResultsMap().get(result1.getId()).getTotalOperands());
            assertEquals(operands1.size(), server.getResultsMap().get(result1.getId()).getReceivedOperands());
            assertEquals(operands1.size(), server.getResultsMap().get(result21.getId()).getTotalOperands());
            assertEquals(operands1.size(), server.getResultsMap().get(result21.getId()).getReceivedOperands());
            ClientInfo clientInfo = server.getClientInfo(client1.getClientId());
            Set<Integer> resultIds = new HashSet<>();
            resultIds.add(result1.getId());
            resultIds.add(result2.getId());
            resultIds.add(result3.getId());
            assertEquals(resultIds.size(), clientInfo.getResultIds().size());
            assertEquals(resultIds, clientInfo.getResultIds());
            ClientInfo clientInfo2 = server.getClientInfo(client2.getClientId());
            assertTrue(clientInfo2.isClosed());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void closeTest() {
        try {
            int[] ports = new int[]{clientPortsCounter++};
            Server server = new Server(ports, 4);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
            Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 3);
            Result result = client.calculate(operands1);
            client.close();
            assertEquals(ClientState.CLOSE, result.getState());
            ClientInfo clientInfo = server.getClientInfo(client.getClientId());
            assertTrue(clientInfo.isClosed());
        } catch (InterruptedException e) {
            assertNull(e);
        }
    }

    @Test
    public void cancelResult() {
        try {
            int[] ports = new int[]{clientPortsCounter++};
            Server server = new Server(ports, 4);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
            Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 1);
            Result result = client.calculate(operands1);
            client.cancelResult(result.getId());
            assertEquals(ClientState.CANCEL, result.getState());
            assertTrue(server.getOperationsForClient(client.getClientId()).isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void cancelDoneResult() {
        try {
            int[] ports = new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++};
            Server server = new Server(ports, 4);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
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
            assertEquals(ClientState.DONE, result1.getState());
            assertEquals(ClientState.DONE, result2.getState());
            assertEquals(ClientState.DONE, result3.getState());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void serverOperationsTest() {
        try {
            int[] ports = new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++};
            Server server = new Server(ports, 4);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
            Client client = new Client(new int[]{ports[0], ports[1], ports[2]}, serverPortsCounter++, 3);
            Result result1 = client.calculate(operands1);
            Result result2 = client.calculate(operands2);
            Result result3 = client.calculate(operands3);
            Result result4 = client.calculate(operands1);
            Result result5 = client.calculate(operands2);
            Result result6 = client.calculate(operands3);
            List<Result> results = new ArrayList<>();
            results.add(result1);
            results.add(result2);
            results.add(result3);
            results.add(result4);
            results.add(result5);
            results.add(result6);
            Map<Integer, ServerOperation> serverOperationMap = server.getResultsMap();
            for (Result result : results) {
                result.get();
                ServerState serverState = serverOperationMap.get(result.getId()).getServerState();
                assertTrue(serverState == ServerState.WAITING_TO_SEND || serverState == ServerState.DONE);
                assertFalse(result.isWaiting());
            }
            assertEquals(serverPortsCounter - 1, serverOperationMap.get(result1.getId()).getAnswerPort());
            assertEquals(serverPortsCounter - 1, serverOperationMap.get(result2.getId()).getAnswerPort());
            assertEquals(serverPortsCounter - 1, serverOperationMap.get(result4.getId()).getAnswerPort());
            assertEquals(operands1.size(), serverOperationMap.get(result1.getId()).getTotalOperands());
            assertEquals(operands1.size(), serverOperationMap.get(result4.getId()).getTotalOperands());
            assertEquals(operands2.size(), serverOperationMap.get(result2.getId()).getTotalOperands());
            assertEquals(operands2.size(), serverOperationMap.get(result5.getId()).getTotalOperands());
            assertEquals(operands3.size(), serverOperationMap.get(result3.getId()).getTotalOperands());
            assertEquals(operands3.size(), serverOperationMap.get(result6.getId()).getTotalOperands());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(expected = NullPointerException.class)
    public void nullPorts() {
        new Client(null, 1000, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negServerPort() {
        new Client(new int[]{1000}, -1000, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negThreadsCount() {
        new Client(new int[]{1000}, 1000, -4);
    }
}