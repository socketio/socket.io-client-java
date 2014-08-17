package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@RunWith(JUnit4.class)
public class SSLConnectionTest extends Connection {

    static {
        // for test on localhost
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier(){
                    public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                        return hostname.equals("localhost");
                    }
                });
    }

    private Socket socket;

    @Override
    String uri() {
        return "https://localhost:" + PORT;
    }

    @Override
    IO.Options createOptions() {
        IO.Options opts = super.createOptions();
        opts.secure = true;
        return opts;
    }

    @Override
    String[] createEnv() {
        return new String[] {"DEBUG=socket.io:*", "PORT=" + PORT, "SSL=1"};
    }

    SSLContext createSSLContext() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("JKS");
        File file = new File("src/test/resources/keystore.jks");
        ks.load(new FileInputStream(file), "password".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "password".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }

    @After
    public void tearDown() {
        IO.setDefaultSSLContext(null);
    }

    @Test(timeout = TIMEOUT)
    public void connect() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        IO.Options opts = createOptions();
        opts.sslContext = createSSLContext();
        socket = client(opts);
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("echo");
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer("done");
                    }
                });
            }
        });
        socket.connect();
        values.take();
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void defaultSSLContext() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        IO.setDefaultSSLContext(createSSLContext());
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("echo");
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer("done");
                    }
                });
            }
        });
        socket.connect();
        values.take();
        socket.close();
    }
}
