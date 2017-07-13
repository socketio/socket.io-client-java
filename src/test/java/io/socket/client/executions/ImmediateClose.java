package io.socket.client.executions;

import io.socket.emitter.Emitter;
import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;

import java.net.URISyntaxException;

public class ImmediateClose {

    public static void main(String[] args) throws URISyntaxException {
        IO.Options options = new IO.Options();
        options.forceNew = true;

        final OkHttpClient client = new OkHttpClient();
        options.webSocketFactory = client;
        options.callFactory = client;

        final Socket socket = IO.socket("http://localhost:" + System.getenv("PORT"), options);
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("connect");
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("disconnect");
            }
        });
        socket.io().on(io.socket.engineio.client.Socket.EVENT_CLOSE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("engine close");
                client.dispatcher().executorService().shutdown();
            }
        });
        socket.connect();
        socket.disconnect();
    }
}
