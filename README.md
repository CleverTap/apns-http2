# jetty-apns-http2
A Java library for sending notifications via APNS using Apple's new HTTP/2 API. This library uses Jetty's Http2Client (from Jetty 9.3.7).

**Note:** Ensure that you have Jetty's ALPN JAR in your boot classpath. [See here for more information](http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html). This is required until Java 9 is released, as Java 8 does not have native support for HTTP/2.

## Installation
- Clone this repository, and add it as a dependent maven project

## Usage
### Create an instance of the client using your certificate
```
FileInputStream cert = new FileInputStream("/path/to/certificate.p12");
final AsyncApnsClient client = new AsyncApnsClient(cert, "", true);
```
### Start the client
```
client.start();
```
### Use Notification.Builder to build your notification
The notification builder supports several other features (such as badge, category, etc). The minimal is shown below:
```
Notification n = new Notification.Builder("my token")
        .alertBody("Hello").build();

```
### Send the notification
```
client.push(n, new NotificationResponseListener() {
    @Override
    public void onSuccess(Notification notification) {
        System.out.println("success!");
    }

    @Override
    public void onFailure(Notification notification, NotificationRequestError err, String responseContent) {
        System.out.println("failure: " + err.name());
        System.out.println("content: " + responseContent);
    }
});
```
