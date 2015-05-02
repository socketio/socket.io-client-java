# Socket.IO-client.java
[![Build Status](https://travis-ci.org/nkzawa/socket.io-client.java.png?branch=master)](https://travis-ci.org/nkzawa/socket.io-client.java)

This is the Socket.IO v1.x Client Library for Java, which is simply ported from the [JavaScript client](https://github.com/Automattic/socket.io-client).

See also:

- [Android chat demo](https://github.com/nkzawa/socket.io-android-chat)
- [engine.io-client.java](https://github.com/nkzawa/engine.io-client.java)

## Installation
The latest artifact is available on Maven Central. To install manually, please refer dependencies to [pom.xml](https://github.com/nkzawa/socket.io-client.java/blob/master/pom.xml).

### Maven
Add the following dependency to your `pom.xml`.

```xml
<dependencies>
  <dependency>
    <groupId>com.github.nkzawa</groupId>
    <artifactId>socket.io-client</artifactId>
    <version>0.5.0</version>
  </dependency>
</dependencies>
```

### Gradle
Add it as a gradle dependency for Android Studio, in `build.gradle`:

```groovy
compile 'com.github.nkzawa:socket.io-client:0.5.0'
```

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

This Library uses [org.json](http://www.json.org/java/) to parse and compose JSON strings:

```java
// Sending an object
JSONObject obj = new JSONObject();
obj.put("hello", "server");
obj.put("binary", new byte[42]);
socket.emit("foo", obj);

// Receiving an object
socket.on("foo", new Emitter.Listener() {
  @Override
  public void call(Object... args) {
    JSONObject obj = (JSONObject)args[0];
  }
});
```

Options are supplied as follows:

```java
IO.Options opts = new IO.Options();
opts.forceNew = true;
opts.reconnection = false;

socket = IO.socket("http://localhost", opts);
```

You can supply query parameters with the `query` option. NB: if you don't want to reuse a cached socket instance when the query parameter changes, you should use the `forceNew` option, the use case might be if your app allows for a user to logout, and a new user to login again:

```java
IO.Options opts = new IO.Options();
opts.forceNew = true;
opts.query = "auth_token=" + authToken;
Socket socket = IO.socket("http://localhost", opts);
```

You can get a callback with `Ack` when the server received a message:

```java
socket.emit("foo", "woot", new Ack() {
  @Override
  public void call(Object... args) {}
});
```

And vice versa:

```java
// ack from client to server
socket.on("foo", new Emitter.Listener() {
  @Override
  public void call(Object... args) {
    Ack ack = (Ack) args[args.length - 1];
    ack.call();
  }
});
```

Use custom SSL settings:

```java
// default SSLContext for all sockets
IO.setDefaultSSLContext(mySSLContext);

// set as an option
opts = new IO.Options();
opts.sslContext = mySSLContext;
socket = IO.socket("https://localhost", opts);
```

See the Javadoc for more details.

http://nkzawa.github.io/socket.io-client.java/apidocs/

## Features
This library supports all of the features the JS client does, including events, options and upgrading transport. Android is fully supported.

## License

MIT

