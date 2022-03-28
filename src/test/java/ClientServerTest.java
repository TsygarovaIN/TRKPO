
import nio.Client;
import nio.Operand;
import nio.OperandType;
import nio.Server;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


public class ClientServerTest {

    static int clientPortsCounter = 8001;
    static int serverPortsCounter = 8081;
    private static final AtomicInteger resultIdCounter = new AtomicInteger(1);

    static Operand op1 = new Operand(OperandType.COS, 10, OperandType.PLUS);
    static Operand op2 = new Operand(OperandType.EMPTY, 5, OperandType.MINUS);

    static Operand op4 = new Operand(OperandType.SQUARE, 100, OperandType.PLUS);
    static Operand op5 = new Operand(OperandType.EMPTY, 7, OperandType.MULT);
    static Operand op6 = new Operand(OperandType.ABS, -3, OperandType.EQUALS);

    static List<Operand> partOfOperands = List.of(op1, op2, op4, op5);
    static List<Operand> operands2 = new ArrayList<>();

    static {
        for (int i = 0; i < 1000; i++) {
            operands2.addAll(partOfOperands);
        }
        operands2.add(op6);
    }

    @Test
    public void single_server_single_client_single_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++}, serverPortsCounter++, 1, 1));
        } catch (Exception e) {
            Assert.assertNull(e);
        }
    }

    @Test
    public void multi_server_single_client_single_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++}, serverPortsCounter++, 5, 1));
        } catch (Exception e) {
            Assert.assertNull(e);
        }
    }

    @Test
    public void single_server_multi_client_single_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++}, serverPortsCounter++, 1, 5));
        } catch (Exception e) {
            Assert.assertNull(e);
        }
    }

    @Test
    public void multi_server_multi_client_single_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++}, serverPortsCounter++, 5, 5));
        } catch (Exception e) {
            Assert.assertNull(e);
        }
    }

    @Test
    public void single_server_single_client_multi_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++},
                    serverPortsCounter++, 1, 1));
        } catch (Exception e) {
            Assert.assertNull(e);
        }
    }

    @Test
    public void single_server_multi_client_multi_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++},
                    serverPortsCounter++, 1, 5));
        } catch (Exception e) {
            Assert.assertNull(e);
        }
    }

    @Test
    public void multi_server_single_client_multi_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++},
                    serverPortsCounter++, 5, 1));
        } catch (Exception e) {
            Assert.assertNull(e);
        }
    }

    @Test
    public void multi_server_multi_client_multi_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++},
                    serverPortsCounter++, 5, 5));
        } catch (Exception e) {
            Assert.assertNull(e);
        }
    }

    private double getCalculationTime(int[] ports, int serverPort, int serverThreads, int clientThreads) {
        try {
            Server server = new Server(ports, serverThreads);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
            Client client = new Client(ports, serverPort, clientThreads);
            long m = System.currentTimeMillis();
            CountDownLatch countDownLatch = new CountDownLatch(10);
            for (int i = 0; i < 10; i++) {
                client.calculate(operands2);
            }
            for (int i = 0; i < 10; i++) {
                Runnable runnable = () -> {
                    try {
                        Assert.assertEquals(3.26251584, client.getResult(resultIdCounter.getAndIncrement()).get(), 0.001);
                        countDownLatch.countDown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                };
                runnable.run();
            }
            countDownLatch.await();
            return System.currentTimeMillis() - m;
        } catch (Exception e) {
            Assert.assertNull(e);
            return 0.0;
        }
    }
}