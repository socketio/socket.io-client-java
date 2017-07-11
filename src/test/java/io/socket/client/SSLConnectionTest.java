package io.socket.client;

import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@RunWith(JUnit4.class)
public class SSLConnectionTest extends Connection {

    private static OkHttpClient sOkHttpClient;

    private Socket socket;

    static {
        try {
            prepareOkHttpClient();
        } catch(GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private static void prepareOkHttpClient() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance("JKS");
        File file = new File("src/test/resources/keystore.jks");
        ks.load(new FileInputStream(file), "password".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "password".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext sslContext = SSLContext.getInstance("TLSv1");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        sOkHttpClient = new OkHttpClient.Builder()
                .hostnameVerifier(new HostnameVerifier(){
                    public boolean verify(String hostname, SSLSession sslSession) {
                        return hostname.equals("localhost");
                    }
                })
                .sslSocketFactory(sslContext.getSocketFactory(),
                        (X509TrustManager) tmf.getTrustManagers()[0])
                .build();
    }

    @After
    public void tearDown() {
        IO.setDefaultOkHttpCallFactory(null);
        IO.setDefaultOkHttpWebSocketFactory(null);
    }

    @Test(timeout = TIMEOUT)
    public void connect() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        IO.Options opts = createOptions();
        opts.callFactory = sOkHttpClient;
        opts.webSocketFactory = sOkHttpClient;
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
        IO.setDefaultOkHttpWebSocketFactory(sOkHttpClient);
        IO.setDefaultOkHttpCallFactory(sOkHttpClient);
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
