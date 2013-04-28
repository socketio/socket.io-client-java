package com.github.nkzawa.socketio.client;


import com.github.nkzawa.socketio.parser.Parser;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class IO {

    private static final Map<String, Manager> managers = new HashMap<String, Manager>();

    public static int protocol = Parser.protocol;

    private IO() {}

    public static Socket socket(String uri) throws URISyntaxException {
        return socket(uri, null);
    }

    public static Socket socket(String uri, Options opts) throws URISyntaxException {
        return socket(new URI(uri), opts);
    }

    public static Socket socket(URI uri) throws URISyntaxException {
        return socket(uri, null);
    }

    public static Socket socket(URI uri, Options opts) throws URISyntaxException {
        if (opts == null) {
            opts = new Options();
        }

        URL parsed;
        try {
            parsed = Url.parse(uri);
        } catch (MalformedURLException e) {
            throw new URISyntaxException(uri.toString(), e.getMessage());
        }
        URI href = parsed.toURI();
        Manager io;

        if (opts.forceNew || !opts.multiplex) {
            io = new Manager(href, opts);
        } else {
            String id = Url.extractId(parsed);
            if (!managers.containsKey(id)) {
                managers.put(id, new Manager(href, opts));
            }
            io = managers.get(id);
        }

        String path = uri.getPath();
        return io.socket(path != null && !path.isEmpty() ? path : "/");
    }


    public static class Options extends com.github.nkzawa.engineio.client.Socket.Options {

        public boolean forceNew;
        public boolean multiplex = true;
        public boolean reconnection;
        public int reconnectionAttempts;
        public long reconnectionDelay;
        public long reconnectionDelayMax;
        public long timeout = -1;

    }
}
