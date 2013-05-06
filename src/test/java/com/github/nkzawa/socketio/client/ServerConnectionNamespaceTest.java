package com.github.nkzawa.socketio.client;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ServerConnectionNamespaceTest extends ServerConnectionTest {

    protected String nsp() {
        return "/foo";
    }
}
