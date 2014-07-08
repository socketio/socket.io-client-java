package com.github.nkzawa.hasbinarydata;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class HasBinaryData {

    private HasBinaryData() {}

    public static boolean hasBinary(Object data) {
        return recursiveCheckForBinary(data);
    }

    private static boolean recursiveCheckForBinary(Object obj) {
        if (obj == null) return false;

        if (obj instanceof byte[]) {
            return true;
        }

        if (obj instanceof JSONArray) {
            JSONArray _obj = (JSONArray)obj;
            int length = _obj.length();
            for (int i = 0; i < length; i++) {
                Object v;
                try {
                    v = _obj.isNull(i) ? null : _obj.get(i);
                } catch (JSONException e) {
                    return false;
                }
                if (recursiveCheckForBinary(v)) {
                    return true;
                }
            }
        } else if (obj instanceof JSONObject) {
            JSONObject _obj = (JSONObject)obj;
            Iterator keys = _obj.keys();
            while (keys.hasNext()) {
                String key = (String)keys.next();
                Object v;
                try {
                    v = _obj.get(key);
                } catch (JSONException e) {
                    return false;
                }
                if (recursiveCheckForBinary(v)) {
                    return true;
                }
            }
        }

        return false;
    }
}
