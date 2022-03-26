import nio.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class TestCases2 {

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

    static List<Operand> partOfOperands = List.of(op1, op2, op4, op5);
    static List<Operand> operands1 = List.of(op4, op5, op6);
    static List<Operand> operands2 = new ArrayList<>();
    static List<Operand> operands3 = List.of(op7, op8, op9);

    static {
        for (int i = 0; i < 1000; i++) {
            operands2.addAll(partOfOperands);
        }
        operands2.add(op6);
    }


    @Test
    public void simpleTest() {
        try {
            Server server = new Server(new int[]{clientPortsCounter}, 4);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
            Client client = new Client(new int[]{clientPortsCounter++}, serverPortsCounter++, 4);
            Assert.assertEquals(51, client.calculate(operands1).get(), 0.000000001);
        } catch (Exception e) {
            Assert.assertNull(e);
        }
    }

    @Test
    public void threeClientsTest() {
        try {
            int[] ports = new int[]{clientPortsCounter++, clientPortsCounter++, clientPortsCounter++};
            Server server = new Server(ports, 4);
            Runnable serverRunnable = server::start;
            serverRunnable.run();
            Thread.sleep(1000);
            Client client1 = new Client(new int[]{clientPortsCounter - 1}, serverPortsCounter++, 4);
            Client client2 = new Client(new int[]{clientPortsCounter - 2}, serverPortsCounter++, 4);
            Client client3 = new Client(new int[]{clientPortsCounter - 3}, serverPortsCounter++, 4);
            Assert.assertEquals(51, client1.calculate(operands1).get(), 0.000000001);
            Assert.assertEquals(3.2625158429879466, client2.calculate(operands2).get(), 0.000000001);
            Assert.assertEquals(149, client3.calculate(operands3).get(), 0.000000001);
        } catch (Exception e) {
            Assert.assertNull(e);
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
                    Assert.assertEquals(51, client1.calculate(operands1).get(), 0.000000001);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            };
            Runnable r2 = () -> {
                try {
                    Assert.assertEquals(51, client1.calculate(operands1).get(), 0.000000001);
                    Assert.assertEquals(51, client1.calculate(operands1).get(), 0.000000001);
                    Assert.assertEquals(3.2625158429879466, client2.calculate(operands2).get(), 0.000000001);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            };
            Runnable r3 = () -> {
                try {
                    Assert.assertEquals(149, client1.calculate(operands3).get(), 0.000000001);
                    Assert.assertEquals(51, client3.calculate(operands1).get(), 0.000000001);
                    Assert.assertEquals(149, client3.calculate(operands3).get(), 0.000000001);
                    Assert.assertEquals(3.2625158429879466, client2.calculate(operands2).get(), 0.000000001);
                    Assert.assertEquals(51, client2.calculate(operands1).get(), 0.000000001);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
            r1.run();
            r2.run();
            r3.run();
        } catch (Exception e) {
            Assert.assertNull(e);
        }
    }
}