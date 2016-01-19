# apns-http2
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

## License
Licensed under the [New 3-Clause BSD License](http://opensource.org/licenses/BSD-3-Clause).

```
Copyright (c) 2016, CleverTap
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of CleverTap nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```