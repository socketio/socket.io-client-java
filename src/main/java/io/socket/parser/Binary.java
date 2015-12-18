package io.socket.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Binary {

    private static final String KEY_PLACEHOLDER = "_placeholder";

    private static final String KEY_NUM = "num";
    
    private static final Logger logger = Logger.getLogger(Binary.class.getName());

    @SuppressWarnings("unchecked")
    public static DeconstructedPacket deconstructPacket(Packet packet) {
        List<byte[]> buffers = new ArrayList<byte[]>();

        packet.data = _deconstructPacket(packet.data, buffers);
        packet.attachments = buffers.size();

        DeconstructedPacket result = new DeconstructedPacket();
        result.packet = packet;
        result.buffers = buffers.toArray(new byte[buffers.size()][]);
        return result;
    }

    private static Object _deconstructPacket(Object data, List<byte[]> buffers) {
        if (data == null) return null;

        if (data instanceof byte[]) {
            JSONObject placeholder = new JSONObject();
            try {
                placeholder.put(KEY_PLACEHOLDER, true);
                placeholder.put(KEY_NUM, buffers.size());
            } catch (JSONException e) {
                logger.log(Level.WARNING, "An error occured while putting data to JSONObject", e);
                return null;
            }
            buffers.add((byte[])data);
            return placeholder;
        } else if (data instanceof JSONArray) {
            JSONArray newData = new JSONArray();
            JSONArray _data = (JSONArray)data;
            int len = _data.length();
            for (int i = 0; i < len; i ++) {
                try {
                    newData.put(i, _deconstructPacket(_data.get(i), buffers));
                } catch (JSONException e) {
                    logger.log(Level.WARNING, "An error occured while putting packet data to JSONObject", e);
                    return null;
                }
            }
            return newData;
        } else if (data instanceof JSONObject) {
            JSONObject newData = new JSONObject();
            JSONObject _data = (JSONObject)data;
            Iterator<?> iterator = _data.keys();
            while (iterator.hasNext()) {
                String key = (String)iterator.next();
                try {
                    newData.put(key, _deconstructPacket(_data.get(key), buffers));
                } catch (JSONException e) {
                    logger.log(Level.WARNING, "An error occured while putting data to JSONObject", e);
                    return null;
                }
            }
            return newData;
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    public static Packet reconstructPacket(Packet packet, byte[][] buffers) {
        packet.data = _reconstructPacket(packet.data, buffers);
        packet.attachments = -1;
       return packet;
    }

    private static Object _reconstructPacket(Object data, byte[][] buffers) {
        if (data instanceof JSONArray) {
            JSONArray _data = (JSONArray)data;
            int len = _data.length();
            for (int i = 0; i < len; i ++) {
                try {
                    _data.put(i, _reconstructPacket(_data.get(i), buffers));
                } catch (JSONException e) {
                    logger.log(Level.WARNING, "An error occured while putting packet data to JSONObject", e);
                    return null;
                }
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
                try {
                    _data.put(key, _reconstructPacket(_data.get(key), buffers));
                } catch (JSONException e) {
                    logger.log(Level.WARNING, "An error occured while putting data to JSONObject", e);
                    return null;
                }
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


