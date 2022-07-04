# Android

<!-- MACRO{toc} -->

## How to keep a Socket.IO client running in the background?

Long story short, you probably shouldn't. The Socket.IO client is not meant to be used in a [background service](https://developer.android.com/guide/components/services?hl=en), as it will keep an open TCP connection to the server and quickly drain the battery of your users.

It is totally usable in the foreground though.

See also: https://developer.android.com/training/connectivity

## How to reach an HTTP server?

Starting with Android 9 (API level 28) you need to explicitly allow cleartext traffic to be able to reach an HTTP server (e.g. a local server at `http://192.168.0.10`):

- either for all domains:

`app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest>
    <uses-permission android:name="android.permission.INTERNET" />

    <application android:usesCleartextTraffic="true">
        ...
    </application>
</manifest>
```

- or for a restricted list of domains:

`app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest>
    <uses-permission android:name="android.permission.INTERNET" />

    <application android:networkSecurityConfig="@xml/network_security_config">
        ...
    </application>
</manifest>
```

`app/src/main/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">192.168.0.10</domain>
    </domain-config>
</network-security-config>
```

Reference: https://developer.android.com/training/articles/security-config
