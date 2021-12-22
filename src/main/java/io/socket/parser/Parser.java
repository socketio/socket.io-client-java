package io.socket.parser;

public interface Parser {

    /**
     * Packet type `connect`.
     */
    int CONNECT = 0;

    /**
     * Packet type `disconnect`.
     */
    int DISCONNECT = 1;

    /**
     * Packet type `event`.
     */
    int EVENT = 2;

    /**
     * Packet type `ack`.
     */
    int ACK = 3;

    /**
     * Packet type `error`.
     */
    int CONNECT_ERROR = 4;

    /**
     * Packet type `binary event`.
     */
    int BINARY_EVENT = 5;

    /**
     * Packet type `binary ack`.
     */
    int BINARY_ACK = 6;

    int protocol = 5;

    /**
     * Packet types.
     */
    String[] types = new String[] {
        "CONNECT",
        "DISCONNECT",
        "EVENT",
        "ACK",
        "ERROR",
        "BINARY_EVENT",
        "BINARY_ACK"
    };

    interface Encoder {

        void encode(Packet obj, Callback callback);

        interface Callback {

            void call(Object[] data);
        }
    }

    interface Decoder {

        void add(String obj);

        void add(byte[] obj);

        void destroy();

        void onDecoded(Callback callback);

        interface Callback {

            void call(Packet packet);
        }
    }
}


