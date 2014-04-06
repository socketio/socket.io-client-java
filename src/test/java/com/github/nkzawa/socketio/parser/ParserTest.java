package com.github.nkzawa.socketio.parser;

import com.github.nkzawa.emitter.Emitter;
import org.json.JSONTokener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ParserTest {

    private static Parser.Encoder encoder = new Parser.Encoder();


    @Test
    public void connect() {
        Packet packet = new Packet(Parser.CONNECT);
        packet.nsp = "/woot";
        test(packet);
    }

    @Test
    public void disconnect() {
        Packet packet = new Packet(Parser.DISCONNECT);
        packet.nsp = "/woot";
        test(packet);
    }

    @Test
    public void event() {
        Packet packet1 = new Packet(Parser.EVENT);
        packet1.data = new JSONTokener("[\"a\", 1, {}]").nextValue();
        packet1.nsp = "/";
        test(packet1);

        Packet packet2 = new Packet(Parser.EVENT);
        packet2.data = new JSONTokener("[\"a\", 1, {}]").nextValue();
        packet2.nsp = "/test";
        test(packet2);
    }

    @Test
    public void ack() {
        Packet packet = new Packet(Parser.ACK);
        packet.data = new JSONTokener("[\"a\", 1, {}]").nextValue();
        packet.id = 123;
        packet.nsp = "/";
        test(packet);
    }

    private void test(final Packet obj) {
        encoder.encode(obj, new Parser.Encoder.Callback() {
            @Override
            public void call(String[] encodedPackets) {
                Parser.Decoder decoder = new Parser.Decoder();
                decoder.on(Parser.Decoder.EVENT_DECODED, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Packet packet = (Packet)args[0];
                        assertThat(packet.type, is(obj.type));
                        assertThat(packet.id, is(obj.id));
                        if (packet.data == null) {
                            assertThat(packet.data, is(obj.data));
                        } else {
                            assertThat(packet.data.toString(), is(obj.data.toString()));
                        }
                        assertThat(packet.nsp, is(obj.nsp));
                        assertThat(packet.attachments, is(obj.attachments));
                    }
                });
                decoder.add(encodedPackets[0]);
            }
        });
    }
}
