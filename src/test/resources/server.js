var fs = require('fs');

var server;
if (process.env.SSL) {
  server = require('https').createServer({
    key: fs.readFileSync(__dirname + '/key.pem'),
    cert: fs.readFileSync(__dirname + '/cert.pem')
  });
} else {
  server = require('http').createServer();
}

var io = require('socket.io')(server);
var port = process.env.PORT || 3000;
var nsp = process.argv[2] || '/';
var slice = Array.prototype.slice;

io.of(nsp).on('connection', function(socket) {
  socket.send('hello client');

  socket.on('message', function() {
    var args = slice.call(arguments);
    socket.send.apply(socket, args);
  });

  socket.on('echo', function() {
    var args = slice.call(arguments);
    socket.emit.apply(socket, ['echoBack'].concat(args));
  });

  socket.on('ack', function() {
    var args = slice.call(arguments);
    var callback = args.pop();
    callback.apply(null, args);
  });

  socket.on('callAck', function() {
    socket.emit('ack', function() {
      var args = slice.call(arguments);
      socket.emit.apply(socket, ['ackBack'].concat(args));
    });
  });

  socket.on('getAckDate', function(data, callback) {
    callback(new Date());
  });

  socket.on('disconnect', function() {
    console.log('disconnect');
  });

  socket.on('error', function() {
    console.log('error: ', arguments);
  });
});

server.listen(port, function() {
  console.log('Socket.IO server listening on port', port);
});
