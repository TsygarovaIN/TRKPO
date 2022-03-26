package nio;

import java.io.IOException;
import java.io.Serializable;

public class Result implements Serializable {

    private final int id;
    private volatile double result;
    private volatile boolean isWaiting = false;
    private volatile Client client;
    private volatile ClientState state = ClientState.START;


    public Result(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }


    public synchronized double get() throws InterruptedException {
        if (state.equals(ClientState.CANCEL)) {
            return Double.NaN;
        }
        if (!state.equals(ClientState.DONE)) {
            isWaiting = true;
            wait();
            isWaiting = false;
        }
        return result;
    }

    public void set(double result) {
        this.result = result;
    }

    public Double getNonBlocking() {
        return state.equals(ClientState.DONE) ? result : null;
    }

     public ClientState getState() {
        return state;
    }

    public void cancel() {
        try {
            client.cancelResult(id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isWaiting() {
        return isWaiting;
    }

    public void setState(ClientState state) {
        this.state = state;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
