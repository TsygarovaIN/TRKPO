import nio.Client;
import nio.ClientState;
import nio.Operand;
import nio.OperandType;
import nio.Result;
import nio.Server;
import nio.ServerState;
import org.junit.*;
import org.junit.rules.Timeout;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static nio.Server.ClientInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SystemTests {

    static int clientPortsCounter = 8001;
    static int serverPortsCounter = 9191;
    private static final AtomicInteger resultIdCounter = new AtomicInteger(1);
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
    public Timeout globalTimeout = Timeout.seconds(200);

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

    //1S
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

    //2S
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

    //3S
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

    //4S
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

    //5S
    @Test(expected = NullPointerException.class)
    public void nullPorts() {
        Client client = new Client(null, 1000, 4);
    }


    //6S
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

    //7S
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

    //8S
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

    //9S
    @Test
    public void closeTest() {
        Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 3);
        Result result = client.calculate(operands1);
        client.close();
        assertEquals(ClientState.CLOSE, result.getState());
        ClientInfo clientInfo = server.getClientInfo(client.getClientId());
        assertTrue(clientInfo.isClosed());
    }

    //10S
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

    //11S
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

//    @Test
//    public void volume_testing() {
//        try {
//            double time = 0;
//            for (int i = 0; i < 100; i++) {
//                time += getCalculationTime(new int[]{clientPortsCounter++, clientPortsCounter++}, serverPortsCounter++,
//                        10, 10);
//            }
//            System.out.println(time);
//        } catch (Exception e) {
//            assertNull(e);
//        }
//    }
//
//    @Test
//    public void load_test() {
//        try {
//            int[] ports = new int[50];
//            for (int i = 0; i < 50; i++) {
//                ports[i] = clientPortsCounter++;
//            }
//            Server server = new Server(ports, 2);
//            Runnable serverRunnable = server::start;
//            serverRunnable.run();
//            Thread.sleep(1000);
//            Client client1 = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 4);
//            Client client2 = new Client(new int[]{clientPortsCounter - 1, clientPortsCounter - 2, clientPortsCounter - 3}, serverPortsCounter++, 4);
//            Client client3 = new Client(new int[]{clientPortsCounter - 2, clientPortsCounter - 4, clientPortsCounter - 5}, serverPortsCounter++, 4);
//            Client client4 = new Client(new int[]{clientPortsCounter - 8, clientPortsCounter - 9, clientPortsCounter - 10}, serverPortsCounter++, 4);
//            Client client5 = new Client(new int[]{clientPortsCounter - 11, clientPortsCounter - 12} , serverPortsCounter++, 4);
//            Client client6 = new Client(new int[]{clientPortsCounter - 13, clientPortsCounter - 14, clientPortsCounter - 15}, serverPortsCounter++, 4);
//            Client client7 = new Client(new int[]{clientPortsCounter - 16}, serverPortsCounter++, 4);
//            Client client8 = new Client(new int[]{clientPortsCounter- 17, clientPortsCounter - 18, clientPortsCounter - 19}, serverPortsCounter++, 4);
//            Client client9 = new Client(new int[]{clientPortsCounter - 20, clientPortsCounter - 21, clientPortsCounter - 22}, serverPortsCounter++, 4);
//            Client client10 = new Client(new int[]{clientPortsCounter - 23, clientPortsCounter - 24, clientPortsCounter - 25}, serverPortsCounter++, 4);
//            Runnable r1 = () -> {
//                try {
//                    assertEquals(51, client1.calculate(operands1).get(), 0.000000001);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//            };
//            Runnable r2 = () -> {
//                try {
//                    assertEquals(51, client1.calculate(operands1).get(), 0.000000001);
//                    assertEquals(51, client1.calculate(operands1).get(), 0.000000001);
//                    assertEquals(3.2625158429879466, client2.calculate(operands2).get(), 0.000000001);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//            };
//            Runnable r3 = () -> {
//                try {
//                    assertEquals(149, client1.calculate(operands3).get(), 0.000000001);
//                    assertEquals(51, client3.calculate(operands1).get(), 0.000000001);
//                    assertEquals(149, client3.calculate(operands3).get(), 0.000000001);
//                    assertEquals(3.2625158429879466, client2.calculate(operands2).get(), 0.000000001);
//                    assertEquals(51, client2.calculate(operands1).get(), 0.000000001);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            };
//            Runnable r4 = () -> {
//                try {
//                    assertEquals(149, client3.calculate(operands3).get(), 0.000000001);
//                    assertEquals(51, client4.calculate(operands1).get(), 0.000000001);
//                    assertEquals(3.2625158429879466, client2.calculate(operands2).get(), 0.000000001);
//                    assertEquals(51, client4.calculate(operands1).get(), 0.000000001);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            };
//            Runnable r5 = () -> {
//                try {
//                    assertEquals(149, client5.calculate(operands3).get(), 0.000000001);
//                    assertEquals(51, client4.calculate(operands1).get(), 0.000000001);
//                    assertEquals(149, client4.calculate(operands3).get(), 0.000000001);
//                    assertEquals(3.2625158429879466, client5.calculate(operands2).get(), 0.000000001);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            };
//            Runnable r6 = () -> {
//                try {
//                    assertEquals(149, client6.calculate(operands3).get(), 0.000000001);
//                    assertEquals(51, client6.calculate(operands1).get(), 0.000000001);
//                    assertEquals(149, client6.calculate(operands3).get(), 0.000000001);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            };
//            Runnable r7 = () -> {
//                try {
//                    assertEquals(149, client7.calculate(operands3).get(), 0.000000001);
//                    assertEquals(51, client3.calculate(operands1).get(), 0.000000001);
//                    assertEquals(149, client3.calculate(operands3).get(), 0.000000001);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            };
//            Runnable r8 = () -> {
//                try {
//                    assertEquals(149, client8.calculate(operands3).get(), 0.000000001);
//                    assertEquals(51, client8.calculate(operands1).get(), 0.000000001);
//                    assertEquals(149, client8.calculate(operands3).get(), 0.000000001);
//                    assertEquals(3.2625158429879466, client5.calculate(operands2).get(), 0.000000001);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            };
//            Runnable r9 = () -> {
//                try {
//                    assertEquals(149, client9.calculate(operands3).get(), 0.000000001);
//                    assertEquals(51, client2.calculate(operands1).get(), 0.000000001);
//                    assertEquals(149, client2.calculate(operands3).get(), 0.000000001);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            };
//            Runnable r10 = () -> {
//                try {
//                    assertEquals(149, client10.calculate(operands3).get(), 0.000000001);
//                    assertEquals(51, client9.calculate(operands1).get(), 0.000000001);
//                    assertEquals(149, client8.calculate(operands3).get(), 0.000000001);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            };
//            r1.run();
//            r2.run();
//            r3.run();
//            r4.run();
//            r5.run();
//            r6.run();
//            r7.run();
//            r8.run();
//            r9.run();
//            r10.run();
//            Set<Integer> clientIds = Set.of(
//                    client1.getClientId(),
//                    client2.getClientId(),
//                    client3.getClientId(),
//                    client4.getClientId(),
//                    client5.getClientId(),
//                    client6.getClientId(),
//                    client7.getClientId(),
//                    client8.getClientId(),
//                    client9.getClientId(),
//                    client10.getClientId()
//            );
//            assertEquals(clientIds, server.getClients());
//        } catch (Exception e) {
//            assertNull(e);
//        }
//    }

    //12S
    @Test
    public void calculateWithDeadlineTrue() {
        try {
            Client client = new Client(new int[]{ports[0]}, serverPortsCounter++, 1);
            client.calculateWithDeadline(operands2, 3000);
            assertEquals(3.26251584, client.getResult(resultIdCounter.getAndIncrement()).get(), 0.001);
        } catch (Exception e) {
            assertNull(e);
        }
    }

    //13S
    @Test(expected = RuntimeException.class)
    public void calculateWithDeadlineFalse() {
        try {
            Client client = new Client(new int[]{ports[0]}, serverPortsCounter++, 1);
            client.calculateWithDeadline(operands2, 1);
            assertEquals(3.26251584, client.getResult(resultIdCounter.getAndIncrement()).get(), 0.001);
        } catch (InterruptedException e){
            assertNull(e);
        }
    }

    //14S
    @Test(expected = IllegalArgumentException.class)
    public void negativeSPorts() {
        Client client = new Client(new int[]{clientPortsCounter - 1}, -1, 4);
    }

    //15S
    @Test(expected = IllegalArgumentException.class)
    public void negativeThreadsCount() {
        Client client = new Client(new int[]{clientPortsCounter - 1}, 4, -4);
    }

    //16S
    @Test
    public void cancelResultAndGetItAgain() {
        try {
            Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 1);
            Result result = client.calculate(operands1);
            client.cancelResult(result.getId());
            Assert.assertEquals(ClientState.CANCEL, result.getState());
            Assert.assertEquals(Double.NaN, result.get(), EPS);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //17S
    @Test
    public void checkResultStatusSent() {
        try {
            Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 1);
            Result result = client.calculate(operands1);
            Assert.assertEquals(ClientState.SENT, result.getState());
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    //18S
    @Test(expected = RuntimeException.class)
    public void getWrongIdResult() {
        Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 1);
        client.getResult(0);
    }

    //19S
    @Test(expected = RuntimeException.class)
    public void calculateNothing() {
        Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 1);
        client.calculate(null);
    }

    //20S
    @Test(expected = IllegalArgumentException.class)
    public void calculateNothing2() {
        Client client = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 1);
        List<Operand> operands = new ArrayList<>();
        operands.add(op1);
        client.calculate(operands);
    }

//    private double getCalculationTime(int[] ports, int serverPort, int serverThreads, int clientThreads) {
//        try {
//            Server server = new Server(ports, serverThreads);
//            Runnable serverRunnable = server::start;
//            serverRunnable.run();
//            Thread.sleep(1000);
//            Client client = new Client(ports, serverPort, clientThreads);
//            long m = System.currentTimeMillis();
//            CountDownLatch countDownLatch = new CountDownLatch(10);
//
//            for (int i = 0; i < 10; i++) {
//                client.calculate(operands2);
//            }
//            for (int i = 0; i < 10; i++) {
//                Runnable runnable = () -> {
//                    try {
//                        assertEquals(3.26251584, client.getResult(resultIdCounter.getAndIncrement()).get(), 0.001);
//                        countDownLatch.countDown();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                };
//                runnable.run();
//            }
//            countDownLatch.await();
//            return System.currentTimeMillis() - m;
//        } catch (Exception e) {
//            return 0.0;
//        }
//    }
}