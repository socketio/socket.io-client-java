package com.github.nkzawa.socketio.parser;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Parser {

    private static final Logger logger = Logger.getLogger(Parser.class.getName());

    private static final Gson gson = new Gson();
    private static final JsonParser parser = new JsonParser();

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

    public static int protocol = 1;

    /**
     * Packet types.
     */
    public static List<String > types = new ArrayList<String>() {{
        add("CONNECT");
        add("DISCONNECT");
        add("EVENT");
        add("ACK");
        add("ERROR");
    }};

    private Parser() {}

    public static String encode(Packet obj) {
        StringBuilder str = new StringBuilder();
        boolean nsp = false;
        str.append(obj.type);

        if (obj.nsp != null && !obj.nsp.isEmpty() && !"/".equals(obj.nsp)) {
            nsp = true;
            str.append(obj.nsp);
        }

        if (obj.id >= 0) {
            if (nsp) {
                str.append(",");
                nsp = false;
            }
            str.append(obj.id);
        }

        if (obj.data != null) {
            if (nsp) str.append(",");
            str.append(gson.toJson(obj.data));
        }

        logger.fine(String.format("encoded %s as %s", obj, str));
        return str.toString();
    }

    public static Packet decode(String str) {
        Packet p = new Packet();
        int i = 0;

        try {
            p.type = Character.getNumericValue(str.charAt(0));
            types.get(p.type);
        } catch (IndexOutOfBoundsException e) {
            return error();
        }

        char next = Character.UNASSIGNED;
        try {
            next = str.charAt(i + 1);
        } catch (IndexOutOfBoundsException e) {
            // do nothing
        }
        if (next == '/') {
            StringBuilder nsp = new StringBuilder();
            while (true) {
                ++i;
                char c = str.charAt(i);
                if (c == ',') break;
                nsp.append(c);
                if (i + 1 == str.length()) break;
            }
            p.nsp = nsp.toString();
        } else {
            p.nsp = "/";
        }

        next = Character.UNASSIGNED;
        try {
            next = str.charAt(i + 1);
        } catch (IndexOutOfBoundsException e) {
            // do nothing
        }
        if (Character.getNumericValue(next) != -1) {
            StringBuilder id = new StringBuilder();
            while (true) {
                ++i;
                Character c = str.charAt(i);
                if (c == null || Character.getNumericValue(c) == -1) {
                    --i;
                    break;
                }
                id.append(c);
                if (i + 1 == str.length()) break;
            }
            p.id = Integer.parseInt(id.toString());
        }

        try {
            str.charAt(++i);
            p.data = parser.parse(str.substring(i));
        } catch (IndexOutOfBoundsException e) {
            // do nothing
        } catch (JsonParseException e) {
            return error();
        }

        logger.fine(String.format("decoded %s as %s", str, p));
        return p;
    }

    private static Packet error() {
        return new Packet(ERROR, new JsonPrimitive("parser error"));
    }
}
