package com.github.nkzawa.socketio.parser;

import com.github.nkzawa.emitter.Emitter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.skyscreamer.jsonassert.JSONAssert;

import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ParserTest {

    private static Parser.Encoder encoder = new Parser.Encoder();


    @Test
    public void encodeConnection() {
        Packet packet = new Packet(Parser.CONNECT);
        packet.nsp = "/woot";
        test(packet);
    }

    @Test
    public void encodeDisconnect() {
        Packet packet = new Packet(Parser.DISCONNECT);
        packet.nsp = "/woot";
        test(packet);
    }

    @Test
    public void encodeEvent() {
        Packet<JSONArray> packet1 = new Packet<JSONArray>(Parser.EVENT);
        packet1.data = new JSONArray("[\"a\", 1, {}]");
        packet1.nsp = "/";
        test(packet1);

        Packet<JSONArray> packet2 = new Packet<JSONArray>(Parser.EVENT);
        packet2.data = new JSONArray("[\"a\", 1, {}]");
        packet2.nsp = "/test";
        test(packet2);
    }

    @Test
    public void encodeAck() {
        Packet<JSONArray> packet = new Packet<JSONArray>(Parser.ACK);
        packet.data = new JSONArray("[\"a\", 1, {}]");
        packet.id = 123;
        packet.nsp = "/";
        test(packet);
    }

    @Test
    public void encodeByteArray() {
        Packet<byte[]> packet = new Packet<byte[]>(Parser.BINARY_EVENT);
        packet.data = "abc".getBytes(Charset.forName("UTF-8"));
        packet.id = 23;
        packet.nsp = "/cool";
        testBin(packet);
    }

    @Test
    public void encodeByteArray2() {
        Packet<byte[]> packet = new Packet<byte[]>(Parser.BINARY_EVENT);
        packet.data = new byte[2];
        packet.id = 0;
        packet.nsp = "/";
        testBin(packet);
    }

    @Test
    public void encodeByteArrayDeep() {
        JSONObject data = new JSONObject("{a: \"hi\", b: {}, c: {a: \"bye\", b: {}}}");
        data.getJSONObject("b").put("why", new byte[3]);
        data.getJSONObject("c").getJSONObject("b").put("a", new byte[6]);

        Packet<JSONObject> packet = new Packet<JSONObject>(Parser.BINARY_EVENT);
        packet.data = data;
        packet.id = 999;
        packet.nsp = "/deep";
        testBin(packet);
    }

    @Test
    public void cleanItselfUpOnClose() {
        JSONArray data = new JSONArray();
        data.put(new byte[2]);
        data.put(new byte[3]);

        Packet<JSONArray> packet = new Packet<JSONArray>(Parser.BINARY_EVENT);
        packet.data = data;
        packet.id = 0;
        packet.nsp = "/";

        encoder.encode(packet, new Parser.Encoder.Callback() {
            @Override
            public void call(final Object[] encodedPackets) {
                final Parser.Decoder decoder = new Parser.Decoder();
                decoder.on(Parser.Decoder.EVENT_DECODED, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        throw new RuntimeException("received a packet when not all binary data was sent.");
                    }
                });

                decoder.add((String)encodedPackets[0]);
                decoder.add((byte[]) encodedPackets[1]);
                decoder.destroy();
                assertThat(decoder.reconstructor.buffers.size(), is(0));
            }
        });
    }

    private void test(final Packet obj) {
        encoder.encode(obj, new Parser.Encoder.Callback() {
            @Override
            public void call(Object[] encodedPackets) {
                Parser.Decoder decoder = new Parser.Decoder();
                decoder.on(Parser.Decoder.EVENT_DECODED, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Packet packet = (Packet)args[0];
                        assertPacket(packet, obj);
                    }
                });
                decoder.add((String)encodedPackets[0]);
            }
        });
    }

    private void testBin(final Packet obj) {
        final Object originalData = obj.data;
        encoder.encode(obj, new Parser.Encoder.Callback() {
            @Override
            public void call(Object[] encodedPackets) {
                Parser.Decoder decoder = new Parser.Decoder();
                decoder.on(Parser.Decoder.EVENT_DECODED, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Packet packet = (Packet)args[0];
                        obj.data = originalData;
                        obj.attachments = -1;
                        assertPacket(packet, obj);
                    }
                });

                for (Object packet : encodedPackets) {
                    if (packet instanceof String) {
                        decoder.add((String)packet);
                    } else if (packet instanceof byte[]) {
                        decoder.add((byte[])packet);
                    }
                }
            }
        });
    }

    private void assertPacket(Packet expected, Packet actual) {
        assertThat(actual.type, is(expected.type));
        assertThat(actual.id, is(expected.id));
        assertThat(actual.nsp, is(expected.nsp));
        assertThat(actual.attachments, is(expected.attachments));

        if (expected.data instanceof JSONArray) {
            JSONAssert.assertEquals((JSONArray)expected.data, (JSONArray)actual.data, true);
        } else if (expected.data instanceof JSONObject) {
            JSONAssert.assertEquals((JSONObject)expected.data, (JSONObject)actual.data, true);
        } else {
            assertThat(actual.data, is(expected.data));
        }
    }
}
