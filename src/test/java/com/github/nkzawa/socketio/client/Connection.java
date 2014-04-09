package com.github.nkzawa.socketio.client;

import org.junit.After;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.concurrent.*;

public abstract class Connection {

    final static int TIMEOUT = 3000;
    final static int PORT = 3000;

    private Process serverProcess;
    private ExecutorService serverService;
    private Future serverOutout;
    private Future serverError;

    @Before
    public void startServer() throws IOException, InterruptedException {
        System.out.println("Starting server ...");

        final CountDownLatch latch = new CountDownLatch(1);
        serverProcess = Runtime.getRuntime().exec(
                String.format("node src/test/resources/index.js %s %s", PORT, nsp()),
                new String[] {"DEBUG=socket.io:*"});
        serverService = Executors.newCachedThreadPool();
        serverOutout = serverService.submit(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serverProcess.getInputStream()));
                String line;
                try {
                    line = reader.readLine();
                    latch.countDown();
                    do {
                        System.out.println("SERVER OUT: " + line);
                    } while ((line = reader.readLine()) != null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        serverError = serverService.submit(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serverProcess.getErrorStream()));
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        System.err.println("SERVER ERR: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        latch.await(3000, TimeUnit.MILLISECONDS);
    }

    @After
    public void stopServer() throws InterruptedException {
        System.out.println("Stopping server ...");
        serverProcess.destroy();
        serverOutout.cancel(false);
        serverError.cancel(false);
        serverService.shutdown();
        serverService.awaitTermination(3000, TimeUnit.MILLISECONDS);
    }

    protected Socket client() throws URISyntaxException {
        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        opts.reconnection = false;
        return IO.socket(uri() + nsp(), opts);
    }

    protected String uri() {
        return "http://localhost:" + PORT;
    }

    protected String nsp() {
        return "/";
    }
}
