package com.github.nkzawa.socketio.client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class Url {

    private Url() {}

    public static URL parse(URI uri) throws MalformedURLException {
        String protocol = uri.getScheme();
        if (protocol == null || !protocol.matches("^https?|wss?$")) {
            uri = uri.resolve("https://" + uri.getAuthority());
        }

        int port = uri.getPort();
        if (protocol != null && ((protocol.matches("^http|ws$") && port == 80) ||
                (protocol.matches("^(http|ws)s$") && port == 443))) {
            uri = uri.resolve("//" + uri.getHost());
        }

        String path = uri.getPath();
        if (path == null || path.length() == 0) {
            uri = uri.resolve("/");
        }

        return uri.toURL();
    }

    public static String extractId(URL url) {
        String protocol = url.getProtocol();
        int port = url.getPort();
        if ((protocol.matches("^http|ws$") && port == 80) ||
                (protocol.matches("^(http|ws)s$") && port == 443)) {
            port = -1;
        }
        return protocol + "://" + url.getHost() + (port != -1 ? ":" + port : "");
    }

}
