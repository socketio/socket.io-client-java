package com.github.nkzawa.socketio.parser;


public class Packet<T> {

    public int type = -1;
    public int id = -1;
    public String nsp;
    public T data;
    public int attachments;

    public Packet() {}

    public Packet(int type) {
        this.type = type;
    }

    public Packet(int type, T data) {
        this.type = type;
        this.data = data;
    }
}
