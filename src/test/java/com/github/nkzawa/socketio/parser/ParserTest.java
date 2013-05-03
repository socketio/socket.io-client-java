package com.github.nkzawa.socketio.parser;

import com.google.gson.JsonParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.github.nkzawa.socketio.parser.Parser.decode;
import static com.github.nkzawa.socketio.parser.Parser.encode;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ParserTest {

    @Test
    public void connect() {
        Packet packet = new Packet(Parser.CONNECT);
        packet.nsp = "/woot";
        test(packet);
    }

    public void disconnect() {
        Packet packet = new Packet(Parser.DISCONNECT);
        packet.nsp = "/woot";
        test(packet);
    }

    public void event() {
        Packet packet1 = new Packet(Parser.EVENT);
        packet1.data = new JsonParser().parse("[\"a\", 1, {}]");
        packet1.nsp = "/";
        test(packet1);

        Packet packet2 = new Packet(Parser.EVENT);
        packet2.data = new JsonParser().parse("[\"a\", 1, {}]");
        packet2.nsp = "/test";
        test(packet2);
    }

    public void ack() {
        Packet packet = new Packet(Parser.ACK);
        packet.data = new JsonParser().parse("[\"a\", 1, {}]");
        packet.id = 123;
        packet.nsp = "/";
        test(packet);
    }

    private void test(Packet packet) {
        Packet _packet = decode(encode(packet));
        assertThat(_packet.type, is(packet.type));
        assertThat(_packet.data, is(packet.data));
        assertThat(_packet.nsp, is(packet.nsp));
    }
}
