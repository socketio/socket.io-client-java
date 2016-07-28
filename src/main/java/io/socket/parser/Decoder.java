package io.socket.parser;

import io.socket.emitter.Emitter;

/**
 * Abstract decoder which can be used to implement your own decoder.
 */
public abstract class Decoder extends Emitter {

    public static String EVENT_DECODED = "decoded";

    public abstract void add(String obj);
    public abstract void add(byte[] obj);
    public abstract void destroy();
}