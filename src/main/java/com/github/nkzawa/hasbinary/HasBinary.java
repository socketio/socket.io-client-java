package com.github.nkzawa.hasbinary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class HasBinary {

    private HasBinary() {}

    public static boolean hasBinary(Object data) {
        return _hasBinary(data);
    }

    private static boolean _hasBinary(Object obj) {
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
                if (_hasBinary(v)) {
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
                if (_hasBinary(v)) {
                    return true;
                }
            }
        }

        return false;
    }
}
