
2.0.1 / 2021-04-27
==================

### Bug Fixes

* fix usage with ws:// scheme ([67fd5f3](https://github.com/socketio/socket.io-client-java/commit/67fd5f34a31c63f7884f82ab39386ad343527590))
* ensure buffered events are sent in order ([4885e7d](https://github.com/socketio/socket.io-client-java/commit/4885e7d59fad78285448694cb5681e8a9ce809ef))
* ensure the payload format is valid ([e8ffe9d](https://github.com/socketio/socket.io-client-java/commit/e8ffe9d1383736f6a21090ab959a2f4fa5a41284))
* emit a CONNECT_ERROR event upon connection failure ([d324e7f](https://github.com/socketio/socket.io-client-java/commit/d324e7f396a444ddd556c3d70a85a28eefb1e02b))


2.0.0 / 2020-12-15
==================

### Features

* add options builder ([#304](https://github.com/socketio/socket.io-client-java/issues/304)) ([49068d3](https://github.com/socketio/socket.io-client-java/commit/49068d3cc504c9b83e29a8d5cb4350360c6ef8ea))
* add support for Socket.IO v3 ([79cb27f](https://github.com/socketio/socket.io-client-java/commit/79cb27fc979ecf1eec9dc2dd4a72c8081149d1e2))


1.0.1 / 2020-12-10
==================

### Bug Fixes

* don't process socket.connect() if we are already re-connecting ([#577](https://github.com/socketio/socket.io-client-java/issues/577)) ([54b7311](https://github.com/socketio/socket.io-client-java/commit/54b73114d19f33a78bec1ce99325893129f8a148))
* handle case where URI.getHost() returns null ([#484](https://github.com/socketio/socket.io-client-java/issues/484)) ([567372e](https://github.com/socketio/socket.io-client-java/commit/567372ecfa6c86bdc72f8bc64985d6511dc87666))


1.0.0 / 2017-07-14
==================

* compatible with socket.io 2.0.x
* update engine.io-client
* custom encoder/decoder support
* fix socket id

0.9.0 / 2017-07-11
==================

* compatible with socket.io 1.7.4
* bump engine.io-client
* send query on connect

0.8.3 / 2016-12-12
==================

* bump `engine.io-client`

0.8.2 / 2016-10-22
==================

* bump `engine.io-client`

0.8.1 / 2016-09-27
==================

* bump `engine.io-client`

0.8.0 / 2016-09-23
==================

* bump `engine.io-client`
* README: fix typos [kylestev, lu-zero]
* test: use TLSv1

0.7.0 / 2016-02-01
==================

* compatible with socket.io 1.4.5
* bump `engine.io-client`
* fix event type when emitting ack with binary data [cirocosta]
* don't reuse same namespace connections
* fix handling of disconnection while in `opening` state
* add ping and pong events
* improve cleanup on `Manager`

0.6.3 / 2015-12-23
==================

* bump `engine.io-client`.
* fix back-off calculation
* code quality improvements [civanyp]

0.6.2 / 2015-10-10
==================

* compatible with socket.io 1.3.7
* bump `engine.io-client`
* fix wrong reconnection state

0.6.1 / 2015-08-31
==================

* change package name to "io.socket"

0.6.0 / 2015-08-09
==================

* bump `engine.io-client`.

0.5.2 / 2015-06-28
==================

* make Socket.events protected [icapurro]
* fix readyState check on Manager#open
* change IO.socket(URI) not to throw URISyntaxException

0.5.1 / 2015-06-06
==================

* bump `engine.io-client`.
* fix timeout option
* fix NullPointerException on ack

0.5.0 / 2015-05-02
==================

* bump `engine.io-client`.
* enhance parser decode [niqo01]
* add a wrong event name check
* add setDefaultHostnameVerifier method

0.4.2 / 2015-03-07
==================

* fix error on reconnection attemps

0.4.1 / 2015-02-08
==================

* bump `engine.io-client`.
* fix program doesn't terminate when closing socket before eatablishing connection.

0.4.0 / 2015-01-26
==================

* compatible with socket.io 1.3.2
* bump `engine.io-client`.
* added `Socket#id()` pointing to session id
* add exponential backoff with randomization
* reset reconnection attempts state after a successul connection
* fix binary arguments in emit with ack [AlfioEmanueleFresta]

0.3.0 / 2014-11-04
==================

* compatible with socket.io 1.2.0
* bump `engine.io-client`.
* fix reconnection after reconnecting manually
* enable to stop reconnecting
* enable to reconnect manually
* add `Socket#connected()`
