package nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static nio.ObjectSerializer.serialize;


public class Client {

    //private static ByteBuffer buffer;
    private static final int BUFFER_SIZE = 1536;
    private static final int META_DATA_REQUEST_CODE = 0;
    private static final int CALCULATING_REQUEST_CODE = 1;
    private static final int CANCELLING_REQUEST_CODE = -1;
    private static final int CLOSE_CLIENT_REQUEST_CODE = -2;

    private static final AtomicInteger resultIdCounter = new AtomicInteger(1);
    private static final AtomicInteger clientIdCounter = new AtomicInteger(1);
    private final int clientId = clientIdCounter.getAndIncrement();
    private final CountDownLatch closeClientCountDown = new CountDownLatch(1);
    private final Map<Integer, Result> resultMap = new ConcurrentHashMap<>();
    private final List<SocketChannel> sendingChannels = new CopyOnWriteArrayList<>();
    private int[] clientsPort;
    private int serverPort;
    private int threadsCountForSend;
    private ExecutorService executor;
    private volatile boolean isClosed = false;

    public Client(int[] clientsPort, int serverPort, int threadsCountForSend) {
        try {
            if (clientsPort == null) {
                throw new NullPointerException();
            }
            if (Arrays.stream(clientsPort).filter(value -> value<=0).findAny().isPresent()) {
                throw new IllegalArgumentException();
            }
            if (serverPort < 0) {
                throw new IllegalArgumentException();
            }
            if (threadsCountForSend < 0) {
                throw new IllegalArgumentException();
            }
            this.clientsPort = clientsPort;
            this.serverPort = serverPort;
            this.threadsCountForSend = threadsCountForSend;
            for (int port : clientsPort) {
                sendingChannels.add(SocketChannel.open(new InetSocketAddress("localhost", port)));
            }
            executor = Executors.newFixedThreadPool(threadsCountForSend);
            Thread resultGettingThread = createResultGettingThread(serverPort);
            resultGettingThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Thread createResultGettingThread(int serverPort) {
        return new Thread(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            try (Selector selector = Selector.open()) {
                ServerSocketChannel receivingChannel = ServerSocketChannel.open();
                receivingChannel.bind(new InetSocketAddress("localhost", serverPort));
                receivingChannel.configureBlocking(false);
                receivingChannel.register(selector, SelectionKey.OP_ACCEPT);
                while (!isClosed) {
                    selector.select();
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = selectionKeys.iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        if (key.isAcceptable()) {
                            register(selector, receivingChannel);
                        }
                        if (key.isReadable()) {
                            getAnswer(buffer, key);
                        }
                        iter.remove();
                    }
                }
                receivingChannel.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    private void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

    private void getAnswer(ByteBuffer buffer, SelectionKey key) throws IOException, ClassNotFoundException {
        SocketChannel receivingClient = (SocketChannel) key.channel();
        receivingClient.read(buffer);
        buffer.flip();
        int responseCode = buffer.getInt();
        if (responseCode == CLOSE_CLIENT_REQUEST_CODE) {
            isClosed = true;
            buffer.flip();
            buffer.clear();
            closeClientCountDown.countDown();
            return;
        }
        int resultId = buffer.getInt();
        if (resultId < 1) {
            throw new RuntimeException("Id cannot be < 1");
        }
        Result result = resultMap.get(resultId);
        result.setState(ClientState.RECEIVING);
        double resultValue = buffer.getDouble();
        result.set(resultValue);
        result.setState(ClientState.DONE);
        synchronized (result) {
            if (result.isWaiting()) {
                result.notify();
            }
        }
        buffer.flip();
        buffer.clear();
    }


    public Result calculate(List<Operand> operands) {
        if (operands == null) {
            throw new NullPointerException();
        }
        if (!operands.get(operands.size()-1).getOperationSecond().equals(OperandType.EQUALS)) {
            throw new IllegalArgumentException();
        }
        int maxThreadsCanUsed = Math.min(operands.size(), threadsCountForSend);
        int resultId = resultIdCounter.getAndIncrement();
        Result calculatingResult = new Result(resultId);
        calculatingResult.setClient(this);
        calculatingResult.setState(ClientState.START);
        resultMap.put(resultId, calculatingResult);
        int oneChannelOperandsNumber = operands.size() / maxThreadsCanUsed;
        int leastChannelOperandsNumber = oneChannelOperandsNumber + operands.size() % maxThreadsCanUsed;
        Runnable runnable1 = () -> {
            List<Operand> list = operands.subList(0, leastChannelOperandsNumber);
            try {
                resultMap.get(resultId).setState(ClientState.SENDING);
                sendMetaDataWithChannel(sendingChannels.get(0), resultId, serverPort, operands.size());
                sendWithChannel(sendingChannels.get(0), resultId, list, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        executor.execute(runnable1);
        for (int i = 1; i < maxThreadsCanUsed; i++) {
            int finalI = i;
            Runnable runnable = () -> {
                int k = leastChannelOperandsNumber + (finalI - 1) * oneChannelOperandsNumber;
                List<Operand> list = operands.subList(k, k + oneChannelOperandsNumber);
                try {
                    sendWithChannel(sendingChannels.get(finalI % clientsPort.length), resultId, list, finalI + 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            executor.execute(runnable);
        }
        resultMap.get(resultId).setState(ClientState.SENT);
        return calculatingResult;
    }

    public void cancelResult(int id) throws IOException {
        if (!resultMap.containsKey(id)) {
            throw new RuntimeException("There is no result with id = " + id);
        }
        Result result = resultMap.get(id);
        if (!result.getState().equals(ClientState.DONE)) {
            result.setState(ClientState.CANCELLING);
            int sendingChannelIndex = (int) (Math.random() * sendingChannels.size());
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            buffer.putInt(CANCELLING_REQUEST_CODE);
            buffer.putInt(id);
            buffer.rewind();
            sendingChannels.get(sendingChannelIndex).write(buffer);
            buffer.clear();
        }
        if (!result.getState().equals(ClientState.DONE)) {
            result.setState(ClientState.CANCEL);
        } else {
            result.setState(ClientState.DONE);
            synchronized (result) {
                if (result.isWaiting()) {
                    result.notify();
                }
            }
        }
    }

    private void sendMetaDataWithChannel(SocketChannel sendingChannel, int resultId, int serverPort, int totalOperands) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.putInt(META_DATA_REQUEST_CODE);
        buffer.putInt(resultId);
        buffer.putInt(clientId);
        buffer.putInt(serverPort);
        buffer.putInt(totalOperands);
        buffer.rewind();
        sendingChannel.write(buffer);
        buffer.clear();
    }

    private void sendCloseRequestWithChannel(SocketChannel sendingChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.putInt(CLOSE_CLIENT_REQUEST_CODE);
        buffer.putInt(clientId);
        buffer.putInt(serverPort);
        buffer.rewind();
        sendingChannel.write(buffer);
        buffer.clear();
    }

    private void sendWithChannel(SocketChannel sendingChannel, int resultId, List<Operand> list, int operationsOrder) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.putInt(CALCULATING_REQUEST_CODE);
        buffer.putInt(resultId);
        buffer.putInt(operationsOrder);
        int operationsSubListNumber = 1;
        buffer.putInt(operationsSubListNumber);
        buffer.mark();
        int operandsInBuffer = 0;
        buffer.putInt(operandsInBuffer);
        for (Operand operand : list) {
            byte[] operandBytes = serialize(operand);
            if (buffer.capacity() > buffer.position() + Integer.BYTES + operandBytes.length) {
                buffer.putInt(operandBytes.length);
                buffer.put(operandBytes);
                operandsInBuffer++;
            } else {
                buffer.reset();
                buffer.putInt(operandsInBuffer);
                buffer.rewind();
                sendingChannel.write(buffer);
                buffer.clear();
                buffer.putInt(CALCULATING_REQUEST_CODE);
                buffer.putInt(resultId);
                buffer.putInt(operationsOrder);
                buffer.putInt(++operationsSubListNumber);
                buffer.mark();
                operandsInBuffer = 1;
                buffer.putInt(operandsInBuffer);
                buffer.putInt(operandBytes.length);
                buffer.put(operandBytes);
            }
        }
        buffer.reset();
        buffer.putInt(operandsInBuffer);
        buffer.rewind();
        sendingChannel.write(buffer);
        buffer.clear();
    }


    public Result getResult(int id) {
        if (!resultMap.containsKey(id)) {
            throw new RuntimeException("There is no result with id = " + id);
        }
        return resultMap.get(id);
    }

    public void close() {
        try {
            synchronized (this) {
                if (!isClosed) {
                    executor.shutdown();
                    sendCloseRequestWithChannel(sendingChannels.get(0));
                    closeClientCountDown.await();
                    resultMap.values().forEach(result -> result.setState(ClientState.CLOSE));
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, Result> getResultMap() {
        return resultMap;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public int getClientId() {
        return clientId;
    }

}
