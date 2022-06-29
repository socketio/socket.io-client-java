
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
