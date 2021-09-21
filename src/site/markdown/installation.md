## Compatibility

| Client version | Socket.IO server |
| -------------- | ---------------- |
| 0.9.x  | 1.x |
| 1.x    | 2.x (or 3.1.x / 4.x with [`allowEIO3: true`](https://socket.io/docs/v4/server-options/#alloweio3)) |
| 2.x    | 3.x / 4.x |

## Installation
The latest artifact is available on Maven Central.

### Maven
Add the following dependency to your `pom.xml`.

```xml
<dependencies>
  <dependency>
    <groupId>io.socket</groupId>
    <artifactId>socket.io-client</artifactId>
    <version>2.0.1</version>
  </dependency>
</dependencies>
```

### Gradle
Add it as a gradle dependency for Android Studio, in `build.gradle`:

```groovy
implementation ('io.socket:socket.io-client:2.0.1') {
  // excluding org.json which is provided by Android
  exclude group: 'org.json', module: 'json'
}
```
