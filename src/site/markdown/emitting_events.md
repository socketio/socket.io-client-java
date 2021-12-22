# Emitting events

See also: https://socket.io/docs/v4/emitting-events/

**Table of content**

<!-- MACRO{toc} -->

There are several ways to send events between the server and the client.

## Basic emit

The Socket.IO API is inspired from the Node.js [EventEmitter](https://nodejs.org/docs/latest/api/events.html#events_events):

*Server*

```js
io.on("connection", (socket) => {
  socket.emit("hello", "world");
});
```

*Client*

```java
socket.on("hello", new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        System.out.println(args[0]); // world
    }
});
```

This also works in the other direction:

*Server*

```js
io.on("connection", (socket) => {
  socket.on("hello", (arg) => {
    console.log(arg); // world
  });
});
```

*Client*

```java
socket.emit("hello", "world");
```

You can send any number of arguments, and all serializable datastructures are supported, including binary objects like [Buffer](https://nodejs.org/docs/latest/api/buffer.html#buffer_buffer) or [TypedArray](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/TypedArray).

*Server*

```js
io.on("connection", (socket) => {
  socket.on("hello", (...args) => {
    console.log(args); // [ 1, '2', <Buffer 61 62 63>, { test: '42' } ]
  });
});
```

*Client*

```java
byte[] buffer = "abc".getBytes(StandardCharsets.UTF_8);
JSONObject object = new JSONObject();
object.put("test", "42");

socket.emit("hello", 1, "2", bytes, object);
```

## Acknowledgements

Events are great, but in some cases you may want a more classic request-response API. In Socket.IO, this feature is named acknowledgements.

You can add a callback as the last argument of the `emit()`, and this callback will be called once the other side acknowledges the event:

### From client to server

*Client*

```java
// Java 7
socket.emit("update item", 1, new JSONObject(singletonMap("name", "updated")), new Ack() {
    @Override
    public void call(Object... args) {
        JSONObject response = (JSONObject) args[0];
        System.out.println(response.getString("status")); // "ok"
    }
});

// Java 8 and above
socket.emit("update item", 1, new JSONObject(singletonMap("name", "updated")), (Ack) args -> {
    JSONObject response = (JSONObject) args[0];
    System.out.println(response.getString("status")); // "ok"
});
```

*Server*

```js
io.on("connection", (socket) => {
  socket.on("update item", (arg1, arg2, callback) => {
    console.log(arg1); // 1
    console.log(arg2); // { name: "updated" }
    callback({
      status: "ok"
    });
  });
});
```

### From server to client

*Server*

```js
io.on("connection", (socket) => {
  socket.emit("hello", "please acknowledge", (response) => {
    console.log(response); // prints "hi!"
  });
});
```

*Client*

```java
// Java 7
socket.on("hello", new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        System.out.println(args[0]); // "please acknowledge"
        if (args.length > 1 && args[1] instanceof Ack) {
            ((Ack) args[1]).call("hi!");
        }
    }
});

// Java 8 and above
socket.on("hello", args -> {
    System.out.println(args[0]); // "please acknowledge"
    if (args.length > 1 && args[1] instanceof Ack) {
        ((Ack) args[1]).call("hi!");
    }
});
```
