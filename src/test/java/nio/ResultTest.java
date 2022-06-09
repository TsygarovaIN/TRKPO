package nio;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ResultTest {
    static Operand op1 = new Operand(OperandType.COS, 10, OperandType.PLUS);
    static Operand op2 = new Operand(OperandType.EMPTY, 5, OperandType.MINUS);
    static Operand op3 = new Operand(OperandType.SQUARE, 100, OperandType.PLUS);
    static Operand op4 = new Operand(OperandType.EMPTY, 7, OperandType.MULT);
    static Operand op5 = new Operand(OperandType.ABS, -3, OperandType.EQUALS);

    static List<Operand> operands = listOf(op1, op2, op3, op4, op5);

    private static int idCounter = 1;


    private static List<Operand> listOf(Operand... operands) {
        return new ArrayList<>(Arrays.asList(operands));
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(25);

    @Test
    public void getId() {
        Result result = new Result(idCounter++);
        assertEquals(idCounter - 1, result.getId());
        Result result2 = new Result(idCounter++);
        assertNotEquals(result.getId(), result2.getId());
    }

    @Test
    public void get() {
        try {
            Server server = new Server(new int[]{6000}, 5);
            server.start();
            Thread.sleep(1000);
            Client client = new Client(new int[]{6000}, 6100, 1);
            Result result = client.calculate(operands);
            assertNotEquals(Double.NaN, result.get());
            client.close();
        } catch (InterruptedException e) {
            assertNull(e);
        }
    }

    @Test
    public void getNonBlocking() {
        Result result = new Result(idCounter++);
        assertNull(result.getNonBlocking());
    }

    @Test
    public void getStateDone() {
        try {
            Server server = new Server(new int[]{6001}, 5);
            server.start();
            Thread.sleep(1000);
            Client client = new Client(new int[]{6001}, 6101, 1);
            Result result = client.calculate(operands);
            result.get();
            assertEquals(ClientState.DONE, result.getState());
            client.close();
        } catch (InterruptedException e) {
            assertNull(e);
        }
    }

    @Test
    public void getStateCancel() {
        try {
            Server server = new Server(new int[]{6002}, 5);
            server.start();
            Thread.sleep(1000);
            Client client = new Client(new int[]{6002}, 6102, 1);
            Result result = client.calculate(operands);
            client.cancelResult(result.getId());
            Thread.sleep(2000);
            assertTrue(ClientState.CANCEL == result.getState() || ClientState.SENDING == result.getState());
        } catch (InterruptedException | IOException e) {
            assertNull(e);
            e.printStackTrace();
        }
    }
/*
    @Test
    public void getStateClose() {
    
        try {
            Server server = new Server(new int[]{6003}, 1);
            server.start();
            Thread.sleep(1000);
            Client client = new Client(new int[]{6003}, 6103, 1);
            Result result = client.calculate(operands);
            client.close();
            Thread.sleep(1000);
            assertEquals(ClientState.CLOSE, result.getState());
            client.close();
        } catch (InterruptedException e) {
            assertNull(e);
        }
    }
    */

    @Test
    public void cancel() {
        try {
            Server server = new Server(new int[]{6004}, 1);
            server.start();
            Thread.sleep(1000);
            Client client = new Client(new int[]{6004}, 6104, 1);
            Result result = client.calculate(operands);
            result.cancel();
            Thread.sleep(2000);
            assertTrue( result.getState() == ClientState.CANCEL || result.getState() == ClientState.SENDING);
        } catch (InterruptedException e) {
            assertNull(e);
        }
    }

    @Test
    public void isWaiting() {
        try {
            Server server = new Server(new int[]{6005}, 1);
            server.start();
            Thread.sleep(1000);
            Client client = new Client(new int[]{6005}, 6105, 1);
            Result result = client.calculate(operands);
            result.get();
            assertFalse(result.isWaiting());
        } catch (InterruptedException e) {
            assertNull(e);
        }
    }
}
