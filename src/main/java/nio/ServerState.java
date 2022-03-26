package nio;


public enum ServerState {
    LOADING,
    WAITING_CALCULATE,
    CALCULATING,
    WAITING_TO_SEND,
    SENDING,
    DONE,
    CANCEL,
    CLOSE
}
