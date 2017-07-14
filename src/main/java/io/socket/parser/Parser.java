package io.socket.parser;

public interface Parser {

    /**
     * Packet type `connect`.
     */
    public static final int CONNECT = 0;

    /**
     * Packet type `disconnect`.
     */
    public static final int DISCONNECT = 1;

    /**
     * Packet type `event`.
     */
    public static final int EVENT = 2;

    /**
     * Packet type `ack`.
     */
    public static final int ACK = 3;

    /**
     * Packet type `error`.
     */
    public static final int ERROR = 4;

    /**
     * Packet type `binary event`.
     */
    public static final int BINARY_EVENT = 5;

    /**
     * Packet type `binary ack`.
     */
    public static final int BINARY_ACK = 6;

    public static int protocol = 4;

    /**
     * Packet types.
     */
    public static String[] types = new String[] {
        "CONNECT",
        "DISCONNECT",
        "EVENT",
        "ACK",
        "ERROR",
        "BINARY_EVENT",
        "BINARY_ACK"
    };

    public static interface Encoder {

        public void encode(Packet obj, Callback callback);

        public interface Callback {

            public void call(Object[] data);
        }
    }

    public static interface Decoder {

        public void add(String obj);

        public void add(byte[] obj);

        public void destroy();

        public void onDecoded(Callback callback);

        public interface Callback {

            public void call(Packet packet);
        }
    }
}


