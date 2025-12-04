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

// Create server without connection state recovery
var io = require('socket.io')(server, {
  pingInterval: 2000
});

var port = process.env.PORT || 3001; // Different port to avoid conflicts
var nsp = process.argv[2] || '/';

server.listen(port, () => {
  console.log(`Test server without recovery running on port ${port}`);
});

io.of(nsp).on('connection', (socket) => {
  console.log(`New connection: ${socket.id}`);
  
  socket.on('disconnect', () => {
    console.log(`Client disconnected: ${socket.id}`);
  });
});