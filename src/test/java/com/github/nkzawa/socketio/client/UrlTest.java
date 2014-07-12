package com.github.nkzawa.socketio.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class UrlTest {

    @Test
    public void parse() throws MalformedURLException, URISyntaxException {
        assertThat(Url.parse("http://username:password@host:8080/directory/file?query#ref").toString(),
                is("http://username:password@host:8080/directory/file?query#ref"));
    }

    @Test
    public void parseRelativePath() throws MalformedURLException, URISyntaxException {
        URL url = Url.parse("https://woot.com/test");
        assertThat(url.getProtocol(), is("https"));
        assertThat(url.getHost(), is("woot.com"));
        assertThat(url.getPath(), is("/test"));
    }

    @Test
    public void parseNoProtocol() throws MalformedURLException, URISyntaxException {
        URL url = Url.parse("//localhost:3000");
        assertThat(url.getProtocol(), is("https"));
        assertThat(url.getHost(), is("localhost"));
        assertThat(url.getPort(), is(3000));
    }

    @Test
    public void parseNamespace() throws MalformedURLException, URISyntaxException {
        assertThat(Url.parse("http://woot.com/woot").getPath(), is("/woot"));
        assertThat(Url.parse("http://google.com").getPath(), is("/"));
        assertThat(Url.parse("http://google.com/").getPath(), is("/"));
    }

    @Test
    public void parseDefaultPort() throws MalformedURLException, URISyntaxException {
        assertThat(Url.parse("http://google.com/").toString(), is("http://google.com:80/"));
        assertThat(Url.parse("https://google.com/").toString(), is("https://google.com:443/"));
    }

    @Test
    public void extractId() throws MalformedURLException {
        String id1 = Url.extractId("http://google.com:80/");
        String id2 = Url.extractId("http://google.com/");
        String id3 = Url.extractId("https://google.com/");
        assertThat(id1, is(id2));
        assertThat(id1, is(not(id3)));
        assertThat(id2, is(not(id3)));
    }
}
