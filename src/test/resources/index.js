var server = require('http').Server()
  , io = require('socket.io')(server)
  , port = parseInt(process.argv[2], 10) || 3000
  , nsp = process.argv[3] || '/';

io.of(nsp).on('connection', function(socket) {
  socket.send('hello client');

  socket.on('message', function() {
    var args = Array.prototype.slice.call(arguments);
    console.log('message:', args);
    socket.send.apply(socket, args);
  });

  socket.on('echo', function() {
    var args = Array.prototype.slice.call(arguments);
    console.log('echo:', args);
    socket.emit.apply(socket, ['echoBack'].concat(args));
  });

  socket.on('ack', function() {
    var args = Array.prototype.slice.call(arguments),
        callback = args.pop();
    console.log('ack:', args);
    callback.apply(null, args);
  });

  socket.on('disconnect', function() {
    console.log('disconnect');
  });

  socket.on('error', function() {
    console.log('error: ' + arguments);
  });
});

server.listen(port, function() {
  console.log('Socket.IO server listening on port', port);
});
