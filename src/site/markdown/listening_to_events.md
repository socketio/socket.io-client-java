# Listening to events

See also: https://socket.io/docs/v4/listening-to-events/

**Table of content**

<!-- MACRO{toc} -->

There are several ways to handle events that are transmitted between the server and the client.

## EventEmitter methods

### socket.on(eventName, listener)

Adds the *listener* function to the end of the listeners array for the event named *eventName*.

```java
socket.on("details", new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        // ...
    }
});
```

### socket.once(eventName, listener)

Adds a **one-time** *listener* function for the event named *eventName*.

```java
socket.once("details", new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        // ...
    }
});
```

### socket.off(eventName, listener)

Removes the specified *listener* from the listener array for the event named *eventName*.

```java
Emitter.Listener listener = new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        calls.add("two");
    }
};

socket.on("details", listener);

// and then later...
socket.off("details", listener);
```

### socket.off(eventName)

Removes all listeners for the specific *eventName*.

```java
socket.off("details");
```

### socket.off()

Removes all listeners (for any event).

```java
socket.off();
```
