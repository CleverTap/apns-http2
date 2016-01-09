# jetty-apns-http2
Sending APNS notifications using the new HTTP 2 API with Jetty 9.3.6

# Usage
## Add Jetty's ALPN JAR to your boot classpath (download it from here[http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html])
Add the following VM parameters to your run configuration:
```
-Xbootclasspath/p:<path_to_alpn_boot_jar>
```

## Update the code
- Update your certificate path
- Update the endpoint (if required)
- Update your device token

# Further reading
http://www.javaworld.com/article/2916548/java-web-development/http-2-for-java-developers.html?page=2
https://developer.apple.com/library/ios/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/Chapters/APNsProviderAPI.html