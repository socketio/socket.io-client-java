package com.github.nkzawa.socketio.parser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Binary {

    private static final String KEY_PLACEHOLDER = "_placeholder";

    private static final String KEY_NUM = "num";


    public static DeconstructedPacket deconstructPacket(Packet packet) {
        List<byte[]> buffers = new ArrayList<byte[]>();

        packet.data = deconstructBinPackRecursive(packet.data, buffers);
        packet.attachments = buffers.size();

        DeconstructedPacket result = new DeconstructedPacket();
        result.packet = packet;
        result.buffers = buffers.toArray(new byte[buffers.size()][]);
        return result;
    }

    private static Object deconstructBinPackRecursive(Object data, List<byte[]> buffers) {
        if (data == null) return null;

        if (data instanceof byte[]) {
            JSONObject placeholder = new JSONObject();
            placeholder.put(KEY_PLACEHOLDER, true);
            placeholder.put(KEY_NUM, buffers.size());
            buffers.add((byte[])data);
            return placeholder;
        } else if (data instanceof JSONArray) {
            JSONArray newData = new JSONArray();
            JSONArray _data = (JSONArray)data;
            int len = _data.length();
            for (int i = 0; i < len; i ++) {
                newData.put(i, deconstructBinPackRecursive(_data.get(i), buffers));
            }
            return newData;
        } else if (data instanceof JSONObject) {
            JSONObject newData = new JSONObject();
            JSONObject _data = (JSONObject)data;
            Iterator<?> iterator = _data.keys();
            while (iterator.hasNext()) {
                String key = (String)iterator.next();
                newData.put(key, deconstructBinPackRecursive(_data.get(key), buffers));
            }
            return newData;
        }
        return data;
    }

    public static Packet reconstructPacket(Packet packet, byte[][] buffers) {
        packet.data = reconstructBinPackRecursive(packet.data, buffers);
        packet.attachments = -1;
       return packet;
    }

    private static Object reconstructBinPackRecursive(Object data, byte[][] buffers) {
        if (data instanceof JSONArray) {
            JSONArray _data = (JSONArray)data;
            int len = _data.length();
            for (int i = 0; i < len; i ++) {
                _data.put(i, reconstructBinPackRecursive(_data.get(i), buffers));
            }
            return _data;
        } else if (data instanceof JSONObject) {
            JSONObject _data = (JSONObject)data;
            if (_data.optBoolean(KEY_PLACEHOLDER)) {
                int num = _data.optInt(KEY_NUM, -1);
                return num >= 0 && num < buffers.length ? buffers[num] : null;
            }
            Iterator<?> iterator = _data.keys();
            while (iterator.hasNext()) {
                String key = (String)iterator.next();
                _data.put(key, reconstructBinPackRecursive(_data.get(key), buffers));
            }
            return _data;
        }
        return data;
    }

    public static class DeconstructedPacket {

        public Packet packet;
        public byte[][] buffers;
    }
}


