package io.socket.client;

import org.junit.After;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Logger;

public abstract class Connection {

    private static final Logger logger = Logger.getLogger(Connection.class.getName());

    final static int TIMEOUT = 7_000;
    final static int PORT = 3000;
    final static int NO_RECOVERY_PORT = 3001;

    private Process serverProcess;
    private Process noRecoveryServerProcess;
    private ExecutorService serverService;
    private Future<?> serverOutput;
    private Future<?> serverError;
    private Future<?> noRecoveryServerOutput;
    private Future<?> noRecoveryServerError;

    @Before
    public void startServer() throws IOException, InterruptedException {
        logger.fine("Starting servers...");

        // Start main server
        final CountDownLatch latch = new CountDownLatch(1);
        serverProcess = startServerProcess("node src/test/resources/server.js %s", PORT);
        serverService = Executors.newCachedThreadPool();
        serverOutput = startServerOutput(serverProcess, "MAIN", latch);
        serverError = startServerError(serverProcess, "MAIN");

        // Start no-recovery server
        final CountDownLatch noRecoveryLatch = new CountDownLatch(1);
        noRecoveryServerProcess = startServerProcess("node src/test/resources/server_no_recovery.js %s", NO_RECOVERY_PORT);
        noRecoveryServerOutput = startServerOutput(noRecoveryServerProcess, "NO_RECOVERY", noRecoveryLatch);
        noRecoveryServerError = startServerError(noRecoveryServerProcess, "NO_RECOVERY");

        // Wait for both servers to start
        latch.await(3000, TimeUnit.MILLISECONDS);
        noRecoveryLatch.await(3000, TimeUnit.MILLISECONDS);
    }

    private Process startServerProcess(String script, int port) throws IOException {
        return Runtime.getRuntime().exec(String.format(script, nsp()), createEnv(port));
    }

    private Future<?> startServerOutput(final Process process, final String serverName, final CountDownLatch latch) {
        return serverService.submit(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                try {
                    line = reader.readLine();
                    latch.countDown();
                    do {
                        logger.fine(serverName + " SERVER OUT: " + line);
                    } while ((line = reader.readLine()) != null);
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        });
    }

    private Future<?> startServerError(final Process process, final String serverName) {
        return serverService.submit(new Runnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()));
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        logger.fine(serverName + " SERVER ERR: " + line);
                    }
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        });
    }

    @After
    public void stopServer() throws InterruptedException {
        logger.fine("Stopping servers...");

        // Stop main server
        serverProcess.destroy();
        serverOutput.cancel(false);
        serverError.cancel(false);

        // Stop no-recovery server
        noRecoveryServerProcess.destroy();
        noRecoveryServerOutput.cancel(false);
        noRecoveryServerError.cancel(false);

        serverService.shutdown();
        serverService.awaitTermination(3000, TimeUnit.MILLISECONDS);
    }

    Socket client() {
        return client(createOptions());
    }

    Socket client(String path) {
        return client(path, createOptions());
    }

    Socket client(IO.Options opts) {
        return client(nsp(), opts);
    }

    Socket client(String path, IO.Options opts) {
        int port = opts.port != -1 ? opts.port : PORT;
        return IO.socket(URI.create(uri(port) + path), opts);
    }

    URI uri() {
        return uri(PORT);
    }

    URI uri(int port) {
        return URI.create("http://localhost:" + port);
    }

    String nsp() {
        return "/";
    }

    IO.Options createOptions() {
        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        return opts;
    }

    String[] createEnv() {
        return createEnv(PORT);
    }

    String[] createEnv(int port) {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("DEBUG", "socket.io:*");
        env.put("PORT", String.valueOf(port));
        String[] _env = new String[env.size()];
        int i = 0;
        for (String key : env.keySet()) {
            _env[i] = key + "=" + env.get(key);
            i++;
        }
        return _env;
    }
}
