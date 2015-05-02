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
