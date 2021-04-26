package io.socket.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class UrlTest {

    private URI parse(String uri) {
        return Url.parse(URI.create(uri)).uri;
    }

    private String extractId(String uri) {
        return Url.parse(URI.create(uri)).id;
    }

    @Test
    public void parse() {
        assertThat(parse("http://username:password@host:8080/directory/file?query#ref").toString(),
                is("http://username:password@host:8080/directory/file?query#ref"));
    }

    @Test
    public void parseRelativePath() {
        URI uri = parse("https://woot.com/test");
        assertThat(uri.getScheme(), is("https"));
        assertThat(uri.getHost(), is("woot.com"));
        assertThat(uri.getPath(), is("/test"));
    }

    @Test
    public void parseNoProtocol() {
        URI uri = parse("//localhost:3000");
        assertThat(uri.getScheme(), is("https"));
        assertThat(uri.getHost(), is("localhost"));
        assertThat(uri.getPort(), is(3000));
    }

    @Test
    public void parseNamespace() {
        assertThat(parse("http://woot.com/woot").getPath(), is("/woot"));
        assertThat(parse("http://google.com").getPath(), is("/"));
        assertThat(parse("http://google.com/").getPath(), is("/"));
    }

    @Test
    public void parseDefaultPort() {
        assertThat(parse("http://google.com/").toString(), is("http://google.com:80/"));
        assertThat(parse("https://google.com/").toString(), is("https://google.com:443/"));
    }

    @Test
    public void testWsProtocol() {
        URI uri = parse("ws://woot.com/test");
        assertThat(uri.getScheme(), is("ws"));
        assertThat(uri.getHost(), is("woot.com"));
        assertThat(uri.getPort(), is(80));
        assertThat(uri.getPath(), is("/test"));
    }

    @Test
    public void testWssProtocol() {
        URI uri = parse("wss://woot.com/test");
        assertThat(uri.getScheme(), is("wss"));
        assertThat(uri.getHost(), is("woot.com"));
        assertThat(uri.getPort(), is(443));
        assertThat(uri.getPath(), is("/test"));
    }

    @Test
    public void extractId() {
        String id1 = extractId("http://google.com:80/");
        String id2 = extractId("http://google.com/");
        String id3 = extractId("https://google.com/");
        assertThat(id1, is(id2));
        assertThat(id1, is(not(id3)));
        assertThat(id2, is(not(id3)));
    }

    @Test
    public void ipv6() {
        String url = "http://[::1]";
        URI parsed = parse(url);
        assertThat(parsed.getScheme(), is("http"));
        assertThat(parsed.getHost(), is("[::1]"));
        assertThat(parsed.getPort(), is(80));
        assertThat(extractId(url), is("http://[::1]:80"));
    }

}
