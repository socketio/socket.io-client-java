
## [1.0.2](https://github.com/socketio/socket.io-client-java/compare/socket.io-client-1.0.1...socket.io-client-1.0.2) (2022-07-11)

From the "1.x" branch.

### Bug Fixes

* ensure buffered events are sent in order ([8bd35da](https://github.com/socketio/socket.io-client-java/commit/8bd35da19c1314318fe122876d22e30ae3673ff9))
* ensure randomizationFactor is always between 0 and 1 ([cb966d5](https://github.com/socketio/socket.io-client-java/commit/cb966d5a64790c0584ad97cf55c205cae8bd1287))
* ensure the payload format is valid ([8664499](https://github.com/socketio/socket.io-client-java/commit/8664499b6f31154f49783531f778dac5387b766b))
* fix usage with ws:// scheme ([e57160a](https://github.com/socketio/socket.io-client-java/commit/e57160a00ca1fbb38396effdbc87eb10d6759a51))
* increase the readTimeout value of the default OkHttpClient ([2d87497](https://github.com/socketio/engine.io-client-java/commit/2d874971c2428a7a444b3a33afe66aedcdce3a96)) (from `engine.io-client`)



## [2.1.0](https://github.com/socketio/socket.io-client-java/compare/socket.io-client-2.0.1...socket.io-client-2.1.0) (2022-07-10)


### Bug Fixes

* ensure randomizationFactor is always between 0 and 1 ([0cbf01e](https://github.com/socketio/socket.io-client-java/commit/0cbf01eb2501b3098eacd22594966a719b20c31e))
* prevent socket from reconnecting after middleware failure ([95ecf22](https://github.com/socketio/socket.io-client-java/commit/95ecf222d25de390d8c0f2ffade37b608cf448eb))
* increase the readTimeout value of the default OkHttpClient ([fb531fa](https://github.com/socketio/engine.io-client-java/commit/fb531fab30968a4b65a402c81f37e92dd5671f33)) (from `engine.io-client`)

### Features

* emit with timeout ([fca3b95](https://github.com/socketio/socket.io-client-java/commit/fca3b9507d5bc79d3c41ab6e119efccd23669ca6))

This feature allows to send a packet and expect an acknowledgement from the server within the given delay.

Syntax:

```java
socket.emit("hello", "world", new AckWithTimeout(5000) {
    @Override
    public void onTimeout() {
        // ...
    }

    @Override
    public void onSuccess(Object... args) {
        // ...
    }
});
```

* implement catch-all listeners ([c7d50b8](https://github.com/socketio/socket.io-client-java/commit/c7d50b8ae9787e9ebdff50aa5d36f88433fc50b9))

Syntax:

```java
socket.onAnyIncoming(new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        // ...
    }
});

socket.onAnyOutgoing(new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        // ...
    }
});
```



## [2.0.1](https://github.com/socketio/socket.io-client-java/compare/socket.io-client-2.0.0...socket.io-client-2.0.1) (2021-04-27)


### Bug Fixes

* fix usage with ws:// scheme ([67fd5f3](https://github.com/socketio/socket.io-client-java/commit/67fd5f34a31c63f7884f82ab39386ad343527590))
* ensure buffered events are sent in order ([4885e7d](https://github.com/socketio/socket.io-client-java/commit/4885e7d59fad78285448694cb5681e8a9ce809ef))
* ensure the payload format is valid ([e8ffe9d](https://github.com/socketio/socket.io-client-java/commit/e8ffe9d1383736f6a21090ab959a2f4fa5a41284))
* emit a CONNECT_ERROR event upon connection failure ([d324e7f](https://github.com/socketio/socket.io-client-java/commit/d324e7f396a444ddd556c3d70a85a28eefb1e02b))



## [2.0.0](https://github.com/socketio/socket.io-client-java/compare/socket.io-client-1.0.1...socket.io-client-2.0.0) (2020-12-14)


### Features

* add options builder ([#304](https://github.com/socketio/socket.io-client-java/issues/304)) ([49068d3](https://github.com/socketio/socket.io-client-java/commit/49068d3cc504c9b83e29a8d5cb4350360c6ef8ea))
* add support for Socket.IO v3 ([79cb27f](https://github.com/socketio/socket.io-client-java/commit/79cb27fc979ecf1eec9dc2dd4a72c8081149d1e2)), closes [/github.com/socketio/socket.io-protocol#difference-between-v5-and-v4](https://github.com//github.com/socketio/socket.io-protocol/issues/difference-between-v5-and-v4)



## [1.0.1](https://github.com/socketio/socket.io-client-java/compare/socket.io-client-1.0.0...socket.io-client-1.0.1) (2020-12-10)


### Bug Fixes

* don't process socket.connect() if we are already re-connecting ([#577](https://github.com/socketio/socket.io-client-java/issues/577)) ([54b7311](https://github.com/socketio/socket.io-client-java/commit/54b73114d19f33a78bec1ce99325893129f8a148))
* handle case where URI.getHost() returns null ([#484](https://github.com/socketio/socket.io-client-java/issues/484)) ([567372e](https://github.com/socketio/socket.io-client-java/commit/567372ecfa6c86bdc72f8bc64985d6511dc87666))
