
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
