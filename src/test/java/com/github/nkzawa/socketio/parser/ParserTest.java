package com.github.nkzawa.socketio.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ParserTest {

    private static Parser.Encoder encoder = new Parser.Encoder();

    @Test
    public void encodeConnection() {
        Packet packet = new Packet(Parser.CONNECT);
        packet.nsp = "/woot";
        Helpers.test(packet);
    }

    @Test
    public void encodeDisconnection() {
        Packet packet = new Packet(Parser.DISCONNECT);
        packet.nsp = "/woot";
        Helpers.test(packet);
    }

    @Test
    public void encodeEvent() throws JSONException {
        Packet<JSONArray> packet1 = new Packet<JSONArray>(Parser.EVENT);
        packet1.data = new JSONArray("[\"a\", 1, {}]");
        packet1.nsp = "/";
        Helpers.test(packet1);

        Packet<JSONArray> packet2 = new Packet<JSONArray>(Parser.EVENT);
        packet2.data = new JSONArray("[\"a\", 1, {}]");
        packet2.nsp = "/test";
        Helpers.test(packet2);
    }

    @Test
    public void encodeAck() throws JSONException {
        Packet<JSONArray> packet = new Packet<JSONArray>(Parser.ACK);
        packet.data = new JSONArray("[\"a\", 1, {}]");
        packet.id = 123;
        packet.nsp = "/";
        Helpers.test(packet);
    }

    @Test
    public void decodeInError() throws JSONException {
        // Random string
        Helpers.testDecodeError("asdf");
        // Unknown type
        Helpers.testDecodeError(Parser.types.length + "asdf");
        // Binary event with no `-`
        Helpers.testDecodeError(Parser.BINARY_EVENT + "asdf");
        // Binary ack with no `-`
        Helpers.testDecodeError(Parser.BINARY_ACK + "asdf");
        // Binary event with no attachment
        Helpers.testDecodeError(String.valueOf(Parser.BINARY_EVENT));
        // event non numeric id
        Helpers.testDecodeError(Parser.EVENT + "2sd");
        // event with invalid json data
        Helpers.testDecodeError(Parser.EVENT + "2[\"a\",1,{asdf}]");
    }
}
