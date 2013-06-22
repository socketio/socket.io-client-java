package com.github.nkzawa.socketio.client;

import java.net.URI;

class Engine extends com.github.nkzawa.engineio.client.Socket {

    Engine(URI uri, Options opts) {
        super(uri, opts);
    }

    @Override
    public void onopen() {}

    @Override
    public void onmessage(String s) {}

    @Override
    public void onclose() {}

    @Override
    public void onerror(Exception err) {}
}
