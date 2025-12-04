package io.socket.client;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;


/**
 * Convenient builder class that helps creating
 * {@link io.socket.client.IO.Options Client Option} object as builder pattern.
 * Finally, you can get option object with call {@link #build()} method.
 *
 * @author junbong
 */
public class SocketOptionBuilder {
    /**
     * Construct new builder with default preferences.
     *
     * @return new builder object
     * @see SocketOptionBuilder#builder(IO.Options)
     */
    public static SocketOptionBuilder builder() {
        return new SocketOptionBuilder();
    }


    /**
     * Construct this builder from specified option object.
     * The option that returned from {@link #build()} method
     * is not equals with given option.
     * In other words, builder creates new option object
     * and copy all preferences from given option.
     *
     * @param options option object which to copy preferences
     * @return new builder object
     */
    public static SocketOptionBuilder builder(IO.Options options) {
        return new SocketOptionBuilder(options);
    }


    private final IO.Options options = new IO.Options();


    /**
     * Construct new builder with default preferences.
     */
    protected SocketOptionBuilder() {
        this(null);
    }


    /**
     * Construct this builder from specified option object.
     * The option that returned from {@link #build()} method
     * is not equals with given option.
     * In other words, builder creates new option object
     * and copy all preferences from given option.
     *
     * @param options option object which to copy preferences. Null-ok.
     */
    protected SocketOptionBuilder(IO.Options options) {
        if (options != null) {
            this.setForceNew(options.forceNew)
                .setMultiplex(options.multiplex)
                .setReconnection(options.reconnection)
                .setReconnectionAttempts(options.reconnectionAttempts)
                .setReconnectionDelay(options.reconnectionDelay)
                .setReconnectionDelayMax(options.reconnectionDelayMax)
                .setRandomizationFactor(options.randomizationFactor)
                .setTimeout(options.timeout)
                .setTransports(options.transports)
                .setUpgrade(options.upgrade)
                .setRememberUpgrade(options.rememberUpgrade)
                .setHost(options.host)
                .setHostname(options.hostname)
                .setPort(options.port)
                .setPolicyPort(options.policyPort)
                .setSecure(options.secure)
                .setPath(options.path)
                .setQuery(options.query)
                .setAuth(options.auth)
                .setExtraHeaders(options.extraHeaders);
        }
    }

    public SocketOptionBuilder setForceNew(boolean forceNew) {
        this.options.forceNew = forceNew;
        return this;
    }

    public SocketOptionBuilder setMultiplex(boolean multiplex) {
        this.options.multiplex = multiplex;
        return this;
    }

    public SocketOptionBuilder setReconnection(boolean reconnection) {
        this.options.reconnection = reconnection;
        return this;
    }

    public SocketOptionBuilder setReconnectionAttempts(int reconnectionAttempts) {
        this.options.reconnectionAttempts = reconnectionAttempts;
        return this;
    }

    public SocketOptionBuilder setReconnectionDelay(long reconnectionDelay) {
        this.options.reconnectionDelay = reconnectionDelay;
        return this;
    }

    public SocketOptionBuilder setReconnectionDelayMax(long reconnectionDelayMax) {
        this.options.reconnectionDelayMax = reconnectionDelayMax;
        return this;
    }


    public SocketOptionBuilder setRandomizationFactor(double randomizationFactor) {
        this.options.randomizationFactor = randomizationFactor;
        return this;
    }

    public SocketOptionBuilder setTimeout(long timeout) {
        this.options.timeout = timeout;
        return this;
    }

    public SocketOptionBuilder setTransports(String[] transports) {
        this.options.transports = transports;
        return this;
    }

    public SocketOptionBuilder setUpgrade(boolean upgrade) {
        this.options.upgrade = upgrade;
        return this;
    }

    public SocketOptionBuilder setRememberUpgrade(boolean rememberUpgrade) {
        this.options.rememberUpgrade = rememberUpgrade;
        return this;
    }

    public SocketOptionBuilder setHost(String host) {
        this.options.host = host;
        return this;
    }

    public SocketOptionBuilder setHostname(String hostname) {
        this.options.hostname = hostname;
        return this;
    }

    public SocketOptionBuilder setPort(int port) {
        this.options.port = port;
        return this;
    }

    public SocketOptionBuilder setPolicyPort(int policyPort) {
        this.options.policyPort = policyPort;
        return this;
    }

    public SocketOptionBuilder setQuery(String query) {
        this.options.query = query;
        return this;
    }

    public SocketOptionBuilder setSecure(boolean secure) {
        this.options.secure = secure;
        return this;
    }

    public SocketOptionBuilder setPath(String path) {
        this.options.path = path;
        return this;
    }

    public SocketOptionBuilder setAuth(JSONObject auth) {
        this.options.auth = auth;
        return this;
    }

    public SocketOptionBuilder setExtraHeaders(Map<String, List<String>> extraHeaders) {
        this.options.extraHeaders = extraHeaders;
        return this;
    }

    /**
     * Finally retrieve {@link io.socket.client.IO.Options} object
     * from this builder.
     *
     * @return option that built from this builder
     */
    public IO.Options build() {
        return this.options;
    }
}
