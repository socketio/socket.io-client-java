package io.socket.parser;

import io.socket.emitter.Emitter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class Helpers {

    private static Parser.DefaultEncoder defaultEncoder = new Parser.DefaultEncoder();
    private static Packet<String> errorPacket = new Packet<String>(Parser.ERROR, "parser error");

    public static void test(final Packet obj) {
        defaultEncoder.encode(obj, new Encoder.Callback() {
            @Override
            public void call(Object[] encodedPackets) {
                Parser.DefaultDecoder defaultDecoder = new Parser.DefaultDecoder();
                defaultDecoder.on(Decoder.EVENT_DECODED, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Packet packet = (Packet)args[0];
                        assertPacket(packet, obj);
                    }
                });
                defaultDecoder.add((String)encodedPackets[0]);
            }
        });
    }

    public static void testDecodeError(final String errorMessage) {
        Parser.DefaultDecoder defaultDecoder = new Parser.DefaultDecoder();
        defaultDecoder.on(Decoder.EVENT_DECODED, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Packet packet = (Packet)args[0];
                assertPacket(errorPacket, packet);
            }
        });
        defaultDecoder.add(errorMessage);
    }

    @SuppressWarnings("unchecked")
    public static void testBin(final Packet obj) {
        final Object originalData = obj.data;
        defaultEncoder.encode(obj, new Encoder.Callback() {
            @Override
            public void call(Object[] encodedPackets) {
                Parser.DefaultDecoder defaultDecoder = new Parser.DefaultDecoder();
                defaultDecoder.on(Decoder.EVENT_DECODED, new Emitter.Listener() {
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
                        defaultDecoder.add((String)packet);
                    } else if (packet instanceof byte[]) {
                        defaultDecoder.add((byte[])packet);
                    }
                }
            }
        });
    }

    public static void assertPacket(Packet expected, Packet actual) {
        assertThat(actual.type, is(expected.type));
        assertThat(actual.id, is(expected.id));
        assertThat(actual.nsp, is(expected.nsp));
        assertThat(actual.attachments, is(expected.attachments));

        if (expected.data instanceof JSONArray) {
            try {
                JSONAssert.assertEquals((JSONArray)expected.data, (JSONArray)actual.data, true);
            } catch (JSONException e) {
                throw new AssertionError(e);
            }
        } else if (expected.data instanceof JSONObject) {
            try {
                JSONAssert.assertEquals((JSONObject)expected.data, (JSONObject)actual.data, true);
            } catch (JSONException e) {
                throw new AssertionError(e);
            }
        } else {
            assertThat(actual.data, is(expected.data));
        }
    }
}
