package io.socket.client.executions;

import io.socket.emitter.Emitter;
import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;

import java.net.URISyntaxException;

public class ConnectionFailure {

    public static void main(String[] args) throws URISyntaxException {
        int port = 60_000;
        IO.Options options = new IO.Options();
        options.forceNew = true;
        options.reconnection = false;

        final OkHttpClient client = new OkHttpClient();
        options.webSocketFactory = client;
        options.callFactory = client;

        final Socket socket = IO.socket("http://localhost:" + port, options);
        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("connect error");
                client.dispatcher().executorService().shutdown();
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("disconnect");
            }
        });
        socket.open();
    }
}
