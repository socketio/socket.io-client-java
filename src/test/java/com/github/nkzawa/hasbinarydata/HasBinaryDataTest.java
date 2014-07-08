package com.github.nkzawa.hasbinarydata;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.Charset;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class HasBinaryDataTest {

    @Test
    public void arrayContainsByteArray() throws Exception {
        JSONArray arr = new JSONArray("[1, null, 2]");
        arr.put(1, "asdfasdf".getBytes(Charset.forName("UTF-8")));
        assertTrue(HasBinaryData.hasBinary(arr));
    }

    @Test
    public void byteArray() {
        assertTrue(HasBinaryData.hasBinary(new byte[0]));
    }
}
