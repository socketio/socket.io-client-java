## Compatibility

| Client version | Socket.IO server |
| -------------- | ---------------- |
| 0.9.x  | 1.x |
| 1.x    | 2.x |
| 2.x    | 3.x |

## Installation
The latest artifact is available on Maven Central.

### Maven
Add the following dependency to your `pom.xml`.

```xml
<dependencies>
  <dependency>
    <groupId>io.socket</groupId>
    <artifactId>socket.io-client</artifactId>
    <version>2.0.0</version>
  </dependency>
</dependencies>
```

### Gradle
Add it as a gradle dependency for Android Studio, in `build.gradle`:

```groovy
compile ('io.socket:socket.io-client:2.0.0') {
  // excluding org.json which is provided by Android
  exclude group: 'org.json', module: 'json'
}
```
