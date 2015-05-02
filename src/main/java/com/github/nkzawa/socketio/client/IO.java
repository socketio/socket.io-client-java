package com.github.nkzawa.socketio.client;


import com.github.nkzawa.socketio.parser.Parser;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class IO {

    private static final Logger logger = Logger.getLogger(IO.class.getName());

    private static final ConcurrentHashMap<String, Manager> managers = new ConcurrentHashMap<String, Manager>();

    /**
     * Protocol version.
     */
    public static int protocol = Parser.protocol;

    public static void setDefaultSSLContext(SSLContext sslContext) {
        Manager.defaultSSLContext = sslContext;
    }

    public static void setDefaultHostnameVerifier(HostnameVerifier hostnameVerifier) {
        Manager.defaultHostnameVerifier = hostnameVerifier;
    }

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

    /**
     * Initializes a {@link Socket} from an existing {@link Manager} for multiplexing.
     *
     * @param uri uri to connect.
     * @param opts options for socket.
     * @return {@link Socket} instance.
     * @throws URISyntaxException
     */
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
        URI source = parsed.toURI();
        Manager io;

        if (opts.forceNew || !opts.multiplex) {
            logger.fine(String.format("ignoring socket cache for %s", source));
            io = new Manager(source, opts);
        } else {
            String id = Url.extractId(parsed);
            if (!managers.containsKey(id)) {
                logger.fine(String.format("new io instance for %s", source));
                managers.putIfAbsent(id, new Manager(source, opts));
            }
            io = managers.get(id);
        }

        return io.socket(parsed.getPath());
    }


    public static class Options extends Manager.Options {

        public boolean forceNew;

        /**
         * Whether to enable multiplexing. Default is true.
         */
        public boolean multiplex = true;
    }
}
