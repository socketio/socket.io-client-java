package com.github.nkzawa.socketio.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ExecutionTest extends Connection {

    private static final Logger logger = Logger.getLogger(ExecutionTest.class.getName());

    final static int TIMEOUT = 30 * 1000;

    @Test(timeout = TIMEOUT)
    public void execConnection() throws InterruptedException, IOException {
        exec("com.github.nkzawa.socketio.client.executions.Connection");
    }

    @Test(timeout = TIMEOUT)
    public void execConnectionFailure() throws InterruptedException, IOException {
        exec("com.github.nkzawa.socketio.client.executions.ConnectionFailure");
    }

    @Test(timeout = TIMEOUT)
    public void execImmediateClose() throws InterruptedException, IOException {
        exec("com.github.nkzawa.socketio.client.executions.ImmediateClose");
    }

    private void exec(String mainClass) throws InterruptedException, IOException {
        Process process = Runtime.getRuntime().exec(String.format("mvn --quiet exec:java" +
                " -Dexec.mainClass=%s -Dexec.classpathScope=test", mainClass), createEnv());
        BufferedReader input = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = input.readLine()) != null) {
            logger.fine("EXEC OUT: " + line);
        }
        process.waitFor();
        assertThat(process.exitValue(), is(0));
    }
}
