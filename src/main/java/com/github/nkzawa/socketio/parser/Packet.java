package com.github.nkzawa.socketio.parser;

import com.google.gson.JsonElement;

public class Packet {

    public int type = -1;
    public int id = -1;
    public String nsp;
    public JsonElement data;

    public Packet() {}

    public Packet(int type) {
        this.type = type;
    }

    public Packet(int type, JsonElement data) {
        this.type = type;
        this.data = data;
    }
}
