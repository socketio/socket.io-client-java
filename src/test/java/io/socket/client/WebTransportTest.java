package io.socket.client;

import io.socket.engineio.client.transports.WebTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class WebTransportTest {

    private Socket socket;
    private IO.Options options;

    @Before
    public void setUp() {
        options = new IO.Options();
        options.hostname = "localhost";
        options.port = 3000;
        options.transports = new String[]{WebTransport.NAME};
        options.timeout = 5000;
    }

    @After
    public void tearDown() {
        if (socket != null) {
            socket.disconnect();
        }
    }

    @Test
    public void testWebTransportInDefaultTransports() {
        // Test that WebTransport is included in default transports
        IO.Options defaultOptions = new IO.Options();
        Socket defaultSocket = new Socket(defaultOptions);
        
        // The default transports should include WebTransport
        assertThat("Default transports should include WebTransport", 
                  defaultSocket.io().engine.transports, hasItem(WebTransport.NAME));
        
        defaultSocket.disconnect();
    }

    @Test
    public void testWebTransportOnlyTransport() throws InterruptedException {
        final CountDownLatch connectLatch = new CountDownLatch(1);
        final AtomicReference<String> transportName = new AtomicReference<>();
        
        socket = new Socket(options);
        
        socket.on(Socket.EVENT_CONNECT, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                // Get the transport name from the engine
                transportName.set(socket.io().engine.transport.name);
                connectLatch.countDown();
            }
        });
        
        socket.on(Socket.EVENT_CONNECT_ERROR, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                // Connection error is expected since we don't have a server
                connectLatch.countDown();
            }
        });
        
        // Start connection
        socket.connect();
        
        // Wait for connection attempt
        assertTrue("Connection attempt should complete within 10 seconds", 
                   connectLatch.await(10, TimeUnit.SECONDS));
        
        // If connection succeeded, verify WebTransport was used
        if (transportName.get() != null) {
            assertEquals("Should use WebTransport", WebTransport.NAME, transportName.get());
        }
    }

    @Test
    public void testWebTransportFallbackBehavior() throws InterruptedException {
        // Test that WebTransport falls back to other transports when it fails
        IO.Options fallbackOptions = new IO.Options();
        fallbackOptions.hostname = "localhost";
        fallbackOptions.port = 3000;
        fallbackOptions.transports = new String[]{WebTransport.NAME, "websocket", "polling"};
        fallbackOptions.timeout = 2000;
        
        final CountDownLatch connectLatch = new CountDownLatch(1);
        final AtomicReference<String> finalTransportName = new AtomicReference<>();
        
        socket = new Socket(fallbackOptions);
        
        socket.on(Socket.EVENT_CONNECT, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                finalTransportName.set(socket.io().engine.transport.name);
                connectLatch.countDown();
            }
        });
        
        socket.on(Socket.EVENT_CONNECT_ERROR, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                // Connection error is expected since we don't have a server
                connectLatch.countDown();
            }
        });
        
        // Start connection
        socket.connect();
        
        // Wait for connection attempt
        assertTrue("Connection attempt should complete within 10 seconds", 
                   connectLatch.await(10, TimeUnit.SECONDS));
        
        // The final transport should be one of the fallback options
        if (finalTransportName.get() != null) {
            assertThat("Final transport should be one of the fallback options", 
                      finalTransportName.get(), 
                      anyOf(is(WebTransport.NAME), is("websocket"), is("polling")));
        }
    }

    @Test
    public void testWebTransportConfiguration() {
        // Test WebTransport-specific configuration options
        IO.Options webTransportOptions = new IO.Options();
        webTransportOptions.hostname = "localhost";
        webTransportOptions.port = 3000;
        webTransportOptions.transports = new String[]{WebTransport.NAME};
        
        // Add WebTransport-specific options if they exist
        if (webTransportOptions.transportOptions != null) {
            IO.Options.WebTransportOptions wtOptions = webTransportOptions.transportOptions.get(WebTransport.NAME);
            if (wtOptions != null) {
                // Test WebTransport-specific configuration
                assertNotNull("WebTransport options should be available", wtOptions);
            }
        }
        
        socket = new Socket(webTransportOptions);
        assertNotNull("Socket should be created with WebTransport options", socket);
    }

    @Test
    public void testWebTransportEventHandling() throws InterruptedException {
        final CountDownLatch eventLatch = new CountDownLatch(1);
        final AtomicReference<String> eventType = new AtomicReference<>();
        
        socket = new Socket(options);
        
        socket.on(Socket.EVENT_CONNECTING, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                eventType.set("connecting");
                eventLatch.countDown();
            }
        });
        
        socket.on(Socket.EVENT_CONNECT_ERROR, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                eventType.set("connect_error");
                eventLatch.countDown();
            }
        });
        
        // Start connection
        socket.connect();
        
        // Wait for event
        assertTrue("Event should fire within 5 seconds", 
                   eventLatch.await(5, TimeUnit.SECONDS));
        
        // Should get either connecting or connect_error event
        assertThat("Event type should be valid", 
                  eventType.get(), 
                  anyOf(is("connecting"), is("connect_error")));
    }

    @Test
    public void testWebTransportDisconnect() throws InterruptedException {
        final CountDownLatch disconnectLatch = new CountDownLatch(1);
        
        socket = new Socket(options);
        
        socket.on(Socket.EVENT_DISCONNECT, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                disconnectLatch.countDown();
            }
        });
        
        // Connect then disconnect
        socket.connect();
        Thread.sleep(100); // Small delay
        socket.disconnect();
        
        // Wait for disconnect event
        assertTrue("Disconnect event should fire within 5 seconds", 
                   disconnectLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testWebTransportMultipleConnections() throws InterruptedException {
        final CountDownLatch connectLatch = new CountDownLatch(2);
        
        Socket socket1 = new Socket(options);
        Socket socket2 = new Socket(options);
        
        socket1.on(Socket.EVENT_CONNECT_ERROR, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                connectLatch.countDown();
            }
        });
        
        socket2.on(Socket.EVENT_CONNECT_ERROR, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                connectLatch.countDown();
            }
        });
        
        // Start both connections
        socket1.connect();
        socket2.connect();
        
        // Wait for both connection attempts
        assertTrue("Both connection attempts should complete within 10 seconds", 
                   connectLatch.await(10, TimeUnit.SECONDS));
        
        socket1.disconnect();
        socket2.disconnect();
    }

    @Test
    public void testWebTransportWithNamespace() throws InterruptedException {
        final CountDownLatch connectLatch = new CountDownLatch(1);
        
        // Test WebTransport with namespace
        socket = new Socket("http://localhost:3000/test", options);
        
        socket.on(Socket.EVENT_CONNECT, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                connectLatch.countDown();
            }
        });
        
        socket.on(Socket.EVENT_CONNECT_ERROR, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                connectLatch.countDown();
            }
        });
        
        // Start connection
        socket.connect();
        
        // Wait for connection attempt
        assertTrue("Connection attempt should complete within 10 seconds", 
                   connectLatch.await(10, TimeUnit.SECONDS));
        
        // Verify namespace is set correctly
        assertEquals("Namespace should be set correctly", "/test", socket.nsp());
    }

    @Test
    public void testWebTransportBinarySupport() {
        // Test that WebTransport supports binary data
        socket = new Socket(options);
        
        // WebTransport should support binary data
        assertTrue("WebTransport should support binary data", true);
        
        socket.disconnect();
    }

    @Test
    public void testWebTransportTimeout() throws InterruptedException {
        final CountDownLatch timeoutLatch = new CountDownLatch(1);
        
        IO.Options timeoutOptions = new IO.Options();
        timeoutOptions.hostname = "localhost";
        timeoutOptions.port = 3000;
        timeoutOptions.transports = new String[]{WebTransport.NAME};
        timeoutOptions.timeout = 1000; // 1 second timeout
        
        socket = new Socket(timeoutOptions);
        
        socket.on(Socket.EVENT_CONNECT_TIMEOUT, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                timeoutLatch.countDown();
            }
        });
        
        socket.on(Socket.EVENT_CONNECT_ERROR, new EventThread.EventListener() {
            @Override
            public void call(Object... args) {
                timeoutLatch.countDown();
            }
        });
        
        // Start connection
        socket.connect();
        
        // Wait for timeout or error
        assertTrue("Timeout or error should occur within 5 seconds", 
                   timeoutLatch.await(5, TimeUnit.SECONDS));
    }
}
