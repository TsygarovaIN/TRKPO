package nio;


public enum ClientState {
    START,
    SENDING,
    SENT,
    RECEIVING,
    DONE,
    CANCELLING,
    CANCEL,
    CLOSE
}
