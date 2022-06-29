package io.socket;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;

public class Fiddle {

    public static void main(String[] argz) throws Exception {
        IO.Options options = new IO.Options();

        Socket socket = IO.socket(URI.create("http://localhost:3000"), options);

        socket.on(Socket.EVENT_CONNECT, (__) -> {
            System.out.println("connect");
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, (args) -> {
            System.out.println("connect_error: " + args[0]);
        });

        socket.on(Socket.EVENT_DISCONNECT, (args) -> {
            System.out.println("disconnect due to: " + args[0]);
        });

        socket.connect();
    }
}
