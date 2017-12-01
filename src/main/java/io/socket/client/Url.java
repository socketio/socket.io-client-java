package io.socket.client;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Url {

    private static Pattern PATTERN_HTTP = Pattern.compile("^http|ws$");
    private static Pattern PATTERN_HTTPS = Pattern.compile("^(http|ws)s$");
    /**
     * Expected format: "[id:password@]host[:port]"
     */
    private static Pattern PATTERN_AUTHORITY = Pattern.compile("^(.*@)?([^:]+)(:\\d+)?$");

    private Url() {}

    public static URL parse(String uri) throws URISyntaxException {
        return parse(new URI(uri));
    }

    public static URL parse(URI uri) {
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
        String _host = uri.getHost();
        if (_host == null) {
            // might happen on some of Samsung Devices such as S4.
            _host = extractHostFromAuthorityPart(uri.getRawAuthority());
        }
        try {
            return new URL(protocol + "://"
                    + (userInfo != null ? userInfo + "@" : "")
                    + _host
                    + (port != -1 ? ":" + port : "")
                    + path
                    + (query != null ? "?" + query : "")
                    + (fragment != null ? "#" + fragment : ""));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
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

    private static String extractHostFromAuthorityPart(String authority)
    {
        if (authority == null) {
            throw new RuntimeException("unable to parse the host from the authority");
        }

        Matcher matcher = PATTERN_AUTHORITY.matcher(authority);

        // If the authority part does not match the expected format.
        if (!matcher.matches()) {
            throw new RuntimeException("unable to parse the host from the authority");
        }

        // Return the host part.
        return matcher.group(2);
    }

}
