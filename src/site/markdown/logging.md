# Logging

This library uses JUL (`java.util.logging`) for its debug logs.

Here's how you can display those logs, depending on your logging library:

<!-- MACRO{toc} -->

## Usage with JUL

`src/main/resources/logging.properties`

```properties
handlers = java.util.logging.ConsoleHandler

java.util.logging.ConsoleHandler.level = ALL

.level = INFO
io.socket.level = FINE
```

`src/main/java/MyApp.java`

```java
public class MyApp {
    private static final Logger logger = Logger.getLogger("MyApp");

    public static void main(String[] argz) throws Exception {
        InputStream stream = MyApp.class.getResourceAsStream("logging.properties");
        LogManager.getLogManager().readConfiguration(stream);

        Socket socket = IO.socket(URI.create("https://example.com"));

        socket.on(Socket.EVENT_CONNECT, args -> logger.info("connected!"));

        socket.connect();
    }
}
```

Reference: https://docs.oracle.com/en/java/javase/17/core/java-logging-overview.html

## Usage with Log4j2

`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <dependencies>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-api</artifactId>
            <version>2.18.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>2.18.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-jul</artifactId>
            <version>2.18.0</version>
        </dependency>
        ...
    </dependencies>
    ...
</project>
```

Maven repository:

- https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-api
- https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
- https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-jul

`src/main/resources/log4j2.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="console" target="SYSTEM_OUT" follow="true">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n" />
        </Console>
    </Appenders>

    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="console" />
        </Root>

        <Logger name="io.socket" level="TRACE" />
    </Loggers>
</Configuration>
```

`src/main/java/MyApp.java`

Either by setting the `java.util.logging.manager` environment variable:

```java
public class MyApp {
    private static final Logger logger;

    static {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        logger = LogManager.getLogger(MyApp.class);
    }

    public static void main(String[] argz) throws Exception {
        Socket socket = IO.socket(URI.create("https://example.com"));

        socket.on(Socket.EVENT_CONNECT, args -> logger.info("connected!"));

        socket.connect();
    }
}
```

Or with the `Log4jBridgeHandler` class:

```java
public class MyApp {
    private static final Logger logger = LogManager.getLogger(MyApp.class);

    public static void main(String[] argz) throws Exception {
        Log4jBridgeHandler.install(true, "", true);

        Socket socket = IO.socket(URI.create("https://example.com"));

        socket.on(Socket.EVENT_CONNECT, args -> logger.info("connected!"));

        socket.connect();
    }
}
```

Reference: https://logging.apache.org/log4j/2.x/log4j-jul/index.html

## Usage with Slf4j + logback

`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <dependencies>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.2.11</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>jul-to-slf4j</artifactId>
            <version>1.7.36</version>
        </dependency>
        ...
    </dependencies>
    ...
</project>
```

Maven repository:

- https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
- https://mvnrepository.com/artifact/org.slf4j/jul-to-slf4j

`src/main/resources/logback.xml`

```xml
<configuration>
    <contextListener class="ch.qos.logback.classic.jul.LevelChangePropagator">
        <resetJUL>true</resetJUL>
    </contextListener>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="io.socket" level="DEBUG" />

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

`src/main/java/MyApp.java`

```java
public class MyApp {
    private static final Logger logger = LoggerFactory.getLogger(MyApp.class);

    static {
        SLF4JBridgeHandler.install();
    }

    public static void main(String[] argz) throws Exception {
        Socket socket = IO.socket(URI.create("https://example.com"));

        socket.on(Socket.EVENT_CONNECT, args -> logger.info("connected!"));

        socket.connect();
    }
}
```

Reference: https://www.slf4j.org/manual.html
