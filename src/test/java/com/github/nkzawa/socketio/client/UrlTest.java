package com.github.nkzawa.socketio.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class UrlTest {

    @Test
    public void parse() throws MalformedURLException, URISyntaxException {
        URL url = Url.parse(new URI("http://woot.com/test"));
        assertThat(url.getProtocol(), is("http"));
        assertThat(url.getHost(), is("woot.com"));
        assertThat(url.getPath(), is("/test"));
    }

    @Test
    public void parse_NoProtocol() throws MalformedURLException, URISyntaxException {
        URL url = Url.parse(new URI("//localhost:3000"));
        assertThat(url.getProtocol(), is("https"));
        assertThat(url.getHost(), is("localhost"));
        assertThat(url.getAuthority(), is("localhost:3000"));
        assertThat(url.getPort(), is(3000));
    }

    @Test
    public void parse_Namaspace() throws MalformedURLException, URISyntaxException {
        assertThat(Url.parse(new URI("http://woot.com/woot")).getPath(), is("/woot"));
        assertThat(Url.parse(new URI("http://google.com")).getPath(), is("/"));
        assertThat(Url.parse(new URI("http://google.com/")).getPath(), is("/"));
    }

    @Test
    public void extractId() throws MalformedURLException, URISyntaxException {
        String id1 = Url.extractId(new URL("http://google.com:80/"));
        String id2 = Url.extractId(new URL("http://google.com/"));
        String id3 = Url.extractId(new URL("https://google.com/"));
        assertThat(id1, is(id2));
        assertThat(id1, is(not(id3)));
    }
}
