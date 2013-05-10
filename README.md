# Socket.IO-client.java
[![Build Status](https://travis-ci.org/nkzawa/socket.io-client.java.png?branch=master)](https://travis-ci.org/nkzawa/socket.io-client.java)

This is the Socket.IO v1.0 Client Library for Java, which is simply ported from the [JavaScript client](https://github.com/LearnBoost/socket.io-client).

See also: [Engine.IO-client.java](https://github.com/nkzawa/engine.io-client.java)

## Usage
Socket.IO-client.java has almost the same api and features with the original JS client. You use `IO#socket` to initialize `Socket`:

```java
socket = IO.socket("http://localhost");
socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

  @Override
  public void call(Object... args) {
    socket.emit("foo", "hi");
    socket.disconnect();
  }

}).on("event", new Emitter.Listener() {

  @Override
  public void call(Object... args) {}

}).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

  @Override
  public void call(Object... args) {}

});
socket.connect();
```

This Library uses [Gson](https://code.google.com/p/google-gson/) to parse and compose JSON strings:

```java
// Sending an object
JsonObject obj = new JsonObject();
obj.addProperty("hello", "server");
socket.emit("foo", obj);

// Receiving an object
socket.on("foo", new Emitter.Listener() {
  @Override
  public void call(Object... args) {
    JsonObject obj = (JsonObject)args[0];
  }
});
```

Options are supplied as follow:

```java
IO.Options opts = new IO.Options();
opts.forceNew = true;
opts.cookie = "foo=1;";

socket = IO.socket("http://localhost", opts);
```

You can get a callback with `Ack` when the server received a message:

```java
socket.emit("foo", "woot", new Ack() {
  @Override
  public void call(Object... args) {}
});
```

See the Javadoc for more details.

http://nkzawa.github.io/socket.io-client.java/apidocs/

## License

MIT

