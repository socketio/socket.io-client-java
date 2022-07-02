package io.socket;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.net.URI;

public class Fiddle {

    public static void main(String[] argz) throws Exception {
        IO.Options options = new IO.Options();

        Socket socket = IO.socket(URI.create("http://localhost:3000"), options);

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("connect");
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("connect_error: " + args[0]);
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("disconnect due to: " + args[0]);
            }
        });

        socket.connect();
    }
}
