---
name: Bug report
about: Create a report to help us improve
title: ''
labels: 'bug'
assignees: ''

---

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**

Please fill the following code example:

Socket.IO server version: `x.y.z`

*Server*

```js
import { Server } from "socket.io";

const io = new Server(8080);

io.on("connection", (socket) => {
  // ...
});
```

Socket.IO java client version: `x.y.z`

*Client*

```java
public class MyApplication {
    public static void main(String[] args) throws URISyntaxException {
        IO.Options options = IO.Options.builder()
                .build();

        Socket socket = IO.socket("http://localhost:8080", options);

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("connect");
            }
        });

        socket.open();
    }
}
```

**Expected behavior**
A clear and concise description of what you expected to happen.

**Platform:**
 - Device: [e.g. Samsung S8]
 - OS: [e.g. Android 9.2]

**Additional context**
Add any other context about the problem here.