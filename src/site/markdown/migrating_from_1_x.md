# Migrating from 1.x

The `2.0.0` release is the first release which is compatible with the Socket.IO v3 server. You can find more information about the v3 release here: https://socket.io/blog/socket-io-3-release/

Here is the compatibility table:

| Java client version | Socket.IO server |
| -------------- | ---------------- |
| 0.9.x  | 1.x |
| 1.x    | 2.x (or 3.1.x / 4.x with [`allowEIO3: true`](https://socket.io/docs/v4/server-options/#alloweio3)) |
| 2.x    | 3.x / 4.x |

**Important note:** due to the backward incompatible changes to the Socket.IO protocol, a 2.x Java client will not be able to reach a 2.x server, and vice-versa

Since the Java client matches the Javascript client quite closely, most of the changes listed in the migration guide [here](https://socket.io/docs/v4/migrating-from-2-x-to-3-0) also apply to the Java client:

- [A middleware error will now emit an Error object](#A_middleware_error_will_now_emit_an_Error_object)
- [The Socket `query` option is renamed to `auth`](#The_Socket_query_option_is_renamed_to_auth)
- [The Socket instance will no longer forward the events emitted by its Manager](#The_Socket_instance_will_no_longer_forward_the_events_emitted_by_its_Manager)
- [No more "pong" event](#No_more_.E2.80.9Cpong.E2.80.9D_event)

Additional changes which are specific to the Java client:

- [An `extraHeaders` option is now available](#An_extraHeaders_option_is_now_available)

### A middleware error will now emit an Error object

The `ERROR` event is renamed to `CONNECT_ERROR` and the object emitted is now a `JSONObject`:

Before:

```java
socket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        String error = (String) args[0];
        System.out.println(error); // not authorized
    }
});
```

After:

```java
socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        JSONObject error = (JSONObject) args[0];
        String message = error.getString("message");
        System.out.println(error); // not authorized

        JSONObject data = error.getJSONObject("data"); // additional details (optional)
    }
});
```


### The Socket `query` option is renamed to `auth`

In previous versions, the `query` option was used in two distinct places:

- in the query parameters of the HTTP requests (`GET /socket.io/?EIO=3&abc=def`)
- in the Socket.IO handshake

Which could lead to unexpected behaviors.

New syntax:

```java
IO.Options options = new IO.Options();
options.query = singletonMap("abc", singletonList("def")); // included in the query parameters
options.auth = singletonMap("token", singletonList("1234")); // included in the Socket.IO handshake

Socket socket = IO.socket("https://example.com", options);
```

### The Socket instance will no longer forward the events emitted by its Manager

In previous versions, the Socket instance emitted the events related to the state of the underlying connection. This will not be the case anymore.

You still have access to those events on the Manager instance (the `io()` method of the socket) :

Before:

```java
socket.on(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
    @Override
    public void call(Object... objects) {
        // ...
    }
});
```

After:

```java
socket.io().on(Manager.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
    @Override
    public void call(Object... objects) {
        // ...
    }
});
```

Here is the updated list of events emitted by the Manager:

| Name | Description | Previously (if different) |
| ---- | ----------- | ------------------------- |
| `Manager.EVENT_OPEN` | successful (re)connection | - |
| `Manager.EVENT_ERROR` | (re)connection failure or error after a successful connection | `Manager.EVENT_CONNECT_ERROR` & `Manager.EVENT_CONNECT_TIMEOUT` |
| `Manager.EVENT_CLOSE` | disconnection | - |
| `Manager.EVENT_RECONNECT_ATTEMPT` | reconnection attempt | `Manager.EVENT_RECONNECT_ATTEMPT` & `Manager.EVENT_RECONNECTING` (duplicate) |
| `Manager.EVENT_RECONNECT` | successful reconnection | - |
| `Manager.EVENT_RECONNECT_ERROR` | reconnection failure | - |
| `Manager.EVENT_RECONNECT_FAILED` | reconnection failure after all attempts | - |

Here is the updated list of events emitted by the Socket:

| Name | Description | Previously (if different) |
| ---- | ----------- | ------------------------- |
| `Socket.EVENT_CONNECT` | successful connection to a Namespace | - |
| `Socket.EVENT_CONNECT_ERROR` | connection failure | `Socket.EVENT_ERROR` |
| `Socket.EVENT_DISCONNECT` | disconnection | - |


And finally, here's the updated list of reserved events that you cannot use in your application:

- `connect` (used on the client-side)
- `connect_error` (used on the client-side)
- `disconnect` (used on both sides)
- `disconnecting` (used on the server-side)
- `newListener` and `removeListener` (EventEmitter [reserved events](https://nodejs.org/api/events.html#events_event_newlistener))

```java
socket.emit("connect_error"); // will now throw an exception
```

### No more "pong" event

In Socket.IO v2, you could listen to the `pong` event on the client-side, which included the duration of the last health check round-trip.

Due to the reversal of the heartbeat mechanism (more information [here](https://socket.io/blog/engine-io-4-release/#Heartbeat-mechanism-reversal)), this event has been removed.

Before:

```java
socket.once(Socket.EVENT_PONG, new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        long latency = (long) args[0];
        // ...
    }
});
```

There is no similar API in the new release.

### An `extraHeaders` option is now available

This is a more straightforward way to provide headers that will be included in all HTTP requests.

```java
IO.Options options = new IO.Options();
options.extraHeaders = singletonMap("Authorization", singletonList("Bearer abcd"));

Socket socket = IO.socket("https://example.com", options);
```

Or with the new builder syntax:

```java
IO.Options options = IO.Options.builder()
    .setExtraHeaders(singletonMap("Authorization", singletonList("Bearer abcd")))
    .build();

Socket socket = IO.socket("https://example.com", options);
```
