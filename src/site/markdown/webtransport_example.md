# WebTransport Example

This example demonstrates how to use WebTransport with the Socket.IO Java client.

## Prerequisites

- Java 8 or higher
- A Socket.IO server that supports WebTransport (version 4.7.0+)
- HTTP/3 support on both client and server

## Basic WebTransport Usage

```java
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import io.socket.engineio.client.transports.WebTransport;

import java.net.URI;

public class WebTransportExample {
    private static volatile String currentTransportName = null;
    
    public static void main(String[] args) {
        // Create Socket.IO options with WebTransport enabled
        IO.Options options = IO.Options.builder()
                .setTransports(new String[] { WebTransport.NAME, WebSocket.NAME, Polling.NAME })
                .build();

        // Create socket connection
        Socket socket = IO.socket(URI.create("https://localhost:3000"), options);

        // Listen for transport events to track current transport
        socket.io().on("transport", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args.length > 0 && args[0] instanceof io.socket.engineio.client.Transport) {
                    io.socket.engineio.client.Transport transport = (io.socket.engineio.client.Transport) args[0];
                    currentTransportName = transport.name;
                    System.out.println("Transport event: " + currentTransportName);
                }
            }
        });

        // Set up event listeners
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("Connected with transport: " + currentTransportName);
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("Disconnected");
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.err.println("Connection error: " + args[0]);
            }
        });

        // Connect to server
        socket.connect();
    }
}
```

## SSL Configuration for Development

When working with self-signed certificates in development, you need to configure the OkHttp client to trust all certificates:

```java
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebTransport;
import io.socket.engineio.client.transports.WebSocket;
import io.socket.engineio.client.transports.Polling;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class WebTransportDevelopmentExample {
    public static void main(String[] args) throws Exception {
        // Create trust-all SSL configuration (DEVELOPMENT ONLY!)
        X509TrustManager trustAllCerts = new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustAllCerts}, new SecureRandom());

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), trustAllCerts)
                .hostnameVerifier((hostname, session) -> true)
                .build();

        IO.Options options = IO.Options.builder()
                .setTransports(new String[] { WebTransport.NAME, WebSocket.NAME, Polling.NAME })
                .setCallFactory(okHttpClient)      // For HTTP requests
                .setWebSocketFactory(okHttpClient) // For WebSocket connections
                .build();

        Socket socket = IO.socket(URI.create("https://localhost:3000"), options);
        
        // Rest of your socket code...
        socket.connect();
    }
}
```

## Advanced WebTransport Configuration

```java
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.client.SocketOptionBuilder;
import io.socket.engineio.client.transports.WebTransport;
import io.socket.engineio.client.transports.WebSocket;
import io.socket.engineio.client.transports.Polling;

import java.net.URI;
import java.util.HashMap;

public class AdvancedWebTransportExample {
    public static void main(String[] args) {

        // Create Socket.IO options with advanced configuration
        IO.Options options = SocketOptionBuilder.builder()
                .setTransports(new String[] { WebTransport.NAME, "websocket", "polling" })
                .setReconnection(true)
                .setReconnectionAttempts(5)
                .setReconnectionDelay(2000)
                .build();
        
        // Configure WebTransport-specific options (advanced)
        if (options.transportOptions == null) {
            options.transportOptions = new HashMap<>();
        }
        
        io.socket.engineio.client.Transport.Options webTransportOptions = 
            new io.socket.engineio.client.Transport.Options();
        webTransportOptions.hostname = "example.com";  // Override hostname if needed
        webTransportOptions.port = 443;               // Override port if needed
        webTransportOptions.secure = true;            // WebTransport requires HTTPS
        webTransportOptions.path = "/socket.io/";     // Socket.IO path
        
        options.transportOptions.put("webtransport", webTransportOptions);

        // Create socket connection
        Socket socket = IO.socket(URI.create("https://example.com"), options);

        // Listen for transport events
        socket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Transport transport = (Transport) args[0];
                System.out.println("Transport created: " + transport.getName());
                
                // Listen for request headers
                transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, java.util.List<String>> headers = 
                            (Map<String, java.util.List<String>>) args[0];
                        System.out.println("Request headers: " + headers);
                    }
                });
                
                // Listen for response headers
                transport.on(Transport.EVENT_RESPONSE_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, java.util.List<String>> headers = 
                            (Map<String, java.util.List<String>>) args[0];
                        System.out.println("Response headers: " + headers);
                    }
                });
            }
        });

        // Set up application event listeners
        socket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("Received message: " + args[0]);
            }
        });

        socket.on("data", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("Received data: " + args[0]);
            }
        });

        // Connect to server
        socket.connect();

        // Send some test messages
        socket.emit("message", "Hello from WebTransport client!");
        socket.emit("data", "Test data payload");
    }
}
```

## WebTransport-Only Configuration

If you want to use only WebTransport (no fallback to other transports):

```java
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.client.SocketOptionBuilder;
import io.socket.engineio.client.transports.WebTransport;

import java.net.URI;

public class WebTransportOnlyExample {
    public static void main(String[] args) {
        // Create options with only WebTransport
        IO.Options options = SocketOptionBuilder.builder()
                .setTransports(new String[] { WebTransport.NAME })
                .build();

        // Create socket connection
        Socket socket = IO.socket(URI.create("https://localhost:3000"), options);

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("Connected with WebTransport!");
                
                // Verify we're using WebTransport
                String transportName = socket.io().engine.transport.name;
                if (WebTransport.NAME.equals(transportName)) {
                    System.out.println("Successfully using WebTransport");
                } else {
                    System.err.println("Expected WebTransport but got: " + transportName);
                }
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.err.println("WebTransport connection failed: " + args[0]);
                System.err.println("Make sure your server supports WebTransport and HTTP/3");
            }
        });

        // Connect to server
        socket.connect();
    }
}
```

## Error Handling

```java
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.client.SocketOptionBuilder;
import io.socket.engineio.client.transports.WebTransport;

import java.net.URI;

public class WebTransportErrorHandling {
    public static void main(String[] args) {
        IO.Options options = SocketOptionBuilder.builder()
                .setTransports(new String[] { WebTransport.NAME, "websocket", "polling" })
                .build();

        Socket socket = IO.socket(URI.create("https://localhost:3000"), options);

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Exception error = (Exception) args[0];
                System.err.println("Connection error: " + error.getMessage());
                
                // Check if it's a WebTransport-specific error
                if (error.getMessage().contains("WebTransport") || 
                    error.getMessage().contains("HTTP/3") ||
                    error.getMessage().contains("QUIC")) {
                    System.err.println("WebTransport connection failed. Falling back to other transports.");
                }
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String reason = (String) args[0];
                System.out.println("Disconnected: " + reason);
                
                if ("transport error".equals(reason)) {
                    System.err.println("Transport error occurred. Check server WebTransport support.");
                }
            }
        });

        socket.connect();
    }
}
```

## Server Requirements

To use WebTransport, your Socket.IO server must:

1. **Support WebTransport**: Use Socket.IO server version 4.7.0 or higher
2. **Enable HTTP/3**: Configure your server to support HTTP/3 and QUIC
3. **Use HTTPS**: WebTransport requires secure connections

### Node.js Server Example

```javascript
const { Server } = require("socket.io");
const { createServer } = require("https");
const { readFileSync } = require("fs");

// Create HTTPS server
const httpsServer = createServer({
  key: readFileSync("key.pem"),
  cert: readFileSync("cert.pem")
});

// Create Socket.IO server with WebTransport support
const io = new Server(httpsServer, {
  transports: ["polling", "websocket", "webtransport"]
});

io.on("connection", (socket) => {
  console.log(`Client connected with transport: ${socket.conn.transport.name}`);
  
  socket.on("message", (data) => {
    console.log("Received:", data);
    socket.emit("message", `Echo: ${data}`);
  });
});

httpsServer.listen(3000, () => {
  console.log("Server listening on https://localhost:3000");
});
```

## Troubleshooting

### Common Issues

1. **"WebTransport not supported"**: Ensure your server supports WebTransport (Socket.IO 4.7.0+)
2. **"HTTP/3 not available"**: Check that your server has HTTP/3 enabled
3. **"Connection timeout"**: Increase `webTransportConnectTimeout` or check network connectivity
4. **"SSL/TLS errors"**: Verify SSL certificates or set `webTransportTrustAllCertificates(true)` for development

### Debugging

Enable debug logging to troubleshoot WebTransport issues:

```java
import java.util.logging.Logger;
import java.util.logging.Level;

// Enable debug logging
Logger.getLogger("io.socket.client").setLevel(Level.FINE);
Logger.getLogger("io.socket.client.WebTransport").setLevel(Level.FINE);
```

## Performance Benefits

WebTransport provides several advantages over traditional WebSocket connections:

- **Reduced latency**: QUIC protocol reduces connection establishment time
- **Better multiplexing**: Multiple streams without head-of-line blocking
- **Improved reliability**: Better handling of network conditions and packet loss
- **Modern protocol**: Built on HTTP/3 and QUIC standards

For applications requiring real-time communication with high performance requirements, WebTransport can provide significant improvements over traditional WebSocket connections.
