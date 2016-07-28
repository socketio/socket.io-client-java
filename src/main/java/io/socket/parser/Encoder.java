package io.socket.parser;

import io.socket.emitter.Emitter;

/**
 * Abstract Encoder which can be used to create your own Encoder.
 * Encode calls callback when encoding is completed
 */
public abstract class Encoder extends Emitter {
    public abstract void encode(Packet obj, Callback callback);

    public interface Callback {

        void call(Object[] data);
    }
}