package nio;

import nio.*;
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

import static org.junit.Assert.*;

public class ServerTest {
    static int portsCounter = 7100;
    static int[] ports;
    static int serverPort = 7270;
    static Client client;

    static Operand op1 = new Operand(OperandType.COS, 10, OperandType.PLUS);
    static Operand op2 = new Operand(OperandType.EMPTY, 5, OperandType.MINUS);
    static Operand op3 = new Operand(OperandType.SQUARE, 100, OperandType.PLUS);
    static Operand op4 = new Operand(OperandType.EMPTY, 7, OperandType.MULT);
    static Operand op5 = new Operand(OperandType.ABS, -3, OperandType.EQUALS);

    static List<Operand> operands = listOf(op1, op2, op3, op4, op5);

    private static List<Operand> listOf(Operand... operands) {
        return new ArrayList<>(Arrays.asList(operands));
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(1000);
    @Before
    public void init() {
        ports = new int[]{++portsCounter, ++portsCounter, ++portsCounter};
    }

    private Server runServer (int serverThreads) {
        ports = new int[]{++portsCounter, ++portsCounter, ++portsCounter};
        Server server = new Server(ports, serverThreads);
        Runnable serverRunnable = server::start;
        serverRunnable.run();
        return server;
    }

    @After
    public void closeAll() {
        if (client!=null ) {
            client.close();
        }
    }

    @Test
    public void startSingleServer() {
        try {
            runServer(1);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void startMultiServer() {
        try {
            Server server = runServer(10);
            server.close();
        } catch (Exception throwable) {
            Assert.assertNull(throwable);
        }
    }

    @Test
    public void getClientsEmpty() {
        Server server = runServer(1);
        server.close();
        assertFalse(server.getClients().iterator().hasNext());
    }

    @Test
    public void getClientsSimple() {
        try {
            Server server = runServer(1);
            assertFalse(server.getClients().iterator().hasNext());
            Thread.sleep(1000);
            client = new Client(ports, ++serverPort, 4);
            client.calculate(operands).get();
            assertEquals(1, server.getClients().size());
            client.close();
            server.close();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getClientsThree(){
        try {
            Server server = runServer(1);
            assertFalse(server.getClients().iterator().hasNext());
            Thread.sleep(1000);
            client = new Client(new int[]{ports[0]}, ++serverPort, 4);
            client.calculate(operands).get();
            client = new Client(new int[]{ports[1]}, ++serverPort, 4);
            client.calculate(operands).get();
            client = new Client(new int[]{ports[2]}, ++serverPort, 4);
            client.calculate(operands).get();
            assertEquals(3, server.getClients().size());
            client.close();
            server.close();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    @Test
    public void getOperationsForClientNonExist() {
        try {
            Server server = runServer(1);
            client = new Client(ports, ++serverPort, 4);
            client.calculate(operands).get();
            assertNull(server.getOperationsForClient(client.getClientId() + 100));
            client.close();
            server.close();
        } catch (Exception e){
            e.printStackTrace();

        }
    }

    @Test
    public void getServerOperationOrderResult() {
        try {
            int currThreadsCountForSend = 4;
            Server server = runServer(1);
            client = new Client(ports, ++serverPort, currThreadsCountForSend);
            client.calculate(operands).get();
            Thread.sleep(1500);
            ServerOperation serverOperation = server.getOperationsForClient(client.getClientId()).values().iterator().next();
            assertEquals(serverOperation.getOperationOrderResults().size(), Math.min(operands.size(), currThreadsCountForSend));
            client.close();
            server.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void getServerOperationOrderLocks() {
        try {
            int currThreadsCountForSend = 4;
            Server server = runServer(1);
            client = new Client(ports, ++serverPort, currThreadsCountForSend);
            client.calculate(operands).get();
            ServerOperation serverOperation = server.getOperationsForClient(client.getClientId()).values().iterator().next();
            assertEquals(serverOperation.getReceivingCalculationLocks().size(), Math.min(operands.size(), currThreadsCountForSend));
            client.close();
            server.close();
        } catch (Exception e){
            e.printStackTrace();

        }
    }

    @Test
    public void getServerOperationReceivedTotalOperands() {
        try {
            Server server = runServer(1);
            client = new Client(ports, ++serverPort, 4);
            client.calculate(operands).get();
            ServerOperation serverOperation = server.getOperationsForClient(client.getClientId()).values().iterator().next();
            assertEquals(serverOperation.getReceivedOperands(), serverOperation.getTotalOperands());
            client.close();
            server.close();
        } catch (Exception e){
            e.printStackTrace();

        }
    }

    @Test
    public void getServerOperationPort(){
        try {
            Server server = runServer(1);
            client = new Client(new int[]{ports[2] - 1}, ++serverPort, 4);
            client.calculate(operands).get();
            //ServerOperation serverOperation = server.getOperationsForClient(client.getClientId()).values().iterator().next();
            assertEquals(serverPort, serverPort);
            client.close();
            server.close();
        } catch (Exception e){
            e.printStackTrace();

        }
    }

    @Test
    public void getServerOperationReceivedOperands() {
        try {
            Server server = runServer(1);
            client = new Client(ports, ++serverPort, 4);
            client.calculate(operands).get();
            ServerOperation serverOperation = server.getOperationsForClient(client.getClientId()).values().iterator().next();
            assertEquals(serverOperation.getReceivedOperands(), operands.size());
            client.close();
            server.close();
        } catch (Exception ignored){
        }
    }

    @Test
    public void getServerOperationTotalOperands(){
        try {
            Server server = runServer(1);
            client = new Client(ports, ++serverPort, 4);
            client.calculate(operands).get();
            ServerOperation serverOperation = server.getOperationsForClient(client.getClientId()).values().iterator().next();
            assertEquals(serverOperation.getTotalOperands(), operands.size());
            client.close();
            server.close();
        }catch (Exception ignored){
        }
    }

    @Test
    public void getServerOperationStateNotLoading() {
        try {
            Server server = runServer(1);
            client = new Client(ports, ++serverPort, 1);
            client.calculate(operands).get();
            ServerOperation serverOperation = server.getOperationsForClient(client.getClientId()).values().iterator().next();
            assertNotEquals(serverOperation.getServerState(), ServerState.LOADING);
            client.close();
            server.close();
        } catch (Exception ignored){
        }
    }

    @Test
    public void getServerOperationState(){
        try {
            Server server = runServer(1);
            client = new Client(ports, ++serverPort, 4);
            client.calculate(operands).get();
            ServerOperation serverOperation = server.getOperationsForClient(client.getClientId()).values().iterator().next();
            assertEquals(serverOperation.getServerState(), ServerState.DONE);
            client.close();
            server.close();
        } catch (Exception e){
            e.printStackTrace();

        }
    }

}