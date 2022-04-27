
import nio.Client;
import nio.Operand;
import nio.OperandType;
import nio.Server;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class ClientServerTest {

    static int clientPortsCounter = 2001;
    static int serverPortsCounter = 4101;
    private static final AtomicInteger resultIdCounter = new AtomicInteger(1);

    static Operand op1 = new Operand(OperandType.COS, 10, OperandType.PLUS);
    static Operand op2 = new Operand(OperandType.EMPTY, 5, OperandType.MINUS);

    static Operand op4 = new Operand(OperandType.SQUARE, 100, OperandType.PLUS);
    static Operand op5 = new Operand(OperandType.EMPTY, 7, OperandType.MULT);
    static Operand op6 = new Operand(OperandType.ABS, -3, OperandType.EQUALS);
    static Operand op7 = new Operand(OperandType.EMPTY, 100, OperandType.PLUS);
    static Operand op8 = new Operand(OperandType.EMPTY, 50, OperandType.MINUS);
    static Operand op9 = new Operand(OperandType.LN, Math.E, OperandType.EQUALS);

    static List<Operand> partOfOperands = new ArrayList<>();
    static List<Operand> operands2 = new ArrayList<>();
    static List<Operand> operands1 = listOf(op4, op5, op6);
    static List<Operand> operands3 = listOf(op7, op8, op9);

    static {
        partOfOperands.add(op1);
        partOfOperands.add(op2);
        partOfOperands.add(op4);
        partOfOperands.add(op5);
        for (int i = 0; i < 1000; i++) {
            operands2.addAll(partOfOperands);
        }
        operands2.add(op6);
    }

    private static List<Operand> listOf(Operand... operands) {
        return new ArrayList<>(Arrays.asList(operands));
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(90);

    @Test
    public void single_server_single_client_single_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++}, serverPortsCounter++, 1, 1));
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void multi_server_single_client_single_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++}, serverPortsCounter++, 5, 1));
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void single_server_multi_client_single_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++}, serverPortsCounter++, 1, 5));
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void multi_server_multi_client_single_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++}, serverPortsCounter++, 5, 5));
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void single_server_single_client_multi_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++},
                    serverPortsCounter++, 1, 1));
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void single_server_multi_client_multi_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++, clientPortsCounter++},
                    serverPortsCounter++, 1, 5));
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void multi_server_single_client_multi_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++,
                            clientPortsCounter++, clientPortsCounter++},
                    serverPortsCounter++, 5, 1));
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void multi_server_multi_client_multi_ports() {
        try {
            System.out.println(getCalculationTime(new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++,
                            clientPortsCounter++, clientPortsCounter++},
                    serverPortsCounter++, 5, 5));
        } catch (Exception e) {
            assertNull(e);
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
                        assertEquals(3.26251584, client.getResult(resultIdCounter.getAndIncrement()).get(), 0.001);
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
            return 0.0;
        }
    }
}