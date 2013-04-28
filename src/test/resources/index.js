var server = require('http').Server()
  , io = require('socket.io')(server)
  , port = parseInt(process.argv[2], 10) || 3000;

io.on('connection', function(socket) {
  socket.send('hello client');

  socket.on('message', function(data) {
    console.log('message:', data);
    socket.send(data);
  });

  socket.on('echo', function(data) {
    console.log('echo:', data);
    socket.emit('echoBack', data);
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
