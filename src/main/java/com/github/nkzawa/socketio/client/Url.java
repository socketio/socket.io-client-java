package com.github.nkzawa.socketio.client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

public class Url {

    private static Pattern PATTERN_HTTP = Pattern.compile("^http|ws$");
    private static Pattern PATTERN_HTTPS = Pattern.compile("^(http|ws)s$");

    private Url() {}

    public static URL parse(String uri) throws URISyntaxException, MalformedURLException {
        return parse(new URI(uri));
    }

    public static URL parse(URI uri) throws MalformedURLException {
        String protocol = uri.getScheme();
        if (protocol == null || !protocol.matches("^https?|wss?$")) {
            protocol = "https";
        }

        int port = uri.getPort();
        if (port == -1) {
            if (PATTERN_HTTP.matcher(protocol).matches()) {
                port = 80;
            } else if (PATTERN_HTTPS.matcher(protocol).matches()) {
                port = 443;
            }
        }

        String path = uri.getRawPath();
        if (path == null || path.length() == 0) {
            path = "/";
        }

        String userInfo = uri.getRawUserInfo();
        String query = uri.getRawQuery();
        String fragment = uri.getRawFragment();
        return new URL(protocol + "://"
                + (userInfo != null ? userInfo + "@" : "")
                + uri.getHost()
                + (port != -1 ? ":" + port : "")
                + path
                + (query != null ? "?" + query : "")
                + (fragment != null ? "#" + fragment : ""));
    }

    public static String extractId(String url) throws MalformedURLException {
        return extractId(new URL(url));
    }

    public static String extractId(URL url) {
        String protocol = url.getProtocol();
        int port = url.getPort();
        if (port == -1) {
            if (PATTERN_HTTP.matcher(protocol).matches()) {
                port = 80;
            } else if (PATTERN_HTTPS.matcher(protocol).matches()) {
                port = 443;
            }
        }
        return protocol + "://" + url.getHost() + ":" + port;
    }

}
