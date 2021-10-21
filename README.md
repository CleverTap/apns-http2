# apns-http2

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=CleverTap_apns-http2&metric=coverage)](https://sonarcloud.io/dashboard?id=CleverTap_apns-http2) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=CleverTap_apns-http2&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=CleverTap_apns-http2)

A Java library for sending notifications via APNS using Apple's new HTTP/2 API.
This library uses OkHttp.
Previous versions included support for Jetty's client,
however, we've removed that due to instability of the Jetty client.

**Note:** Ensure that you have Jetty's ALPN JAR (OkHttp requires it) in your boot classpath. [See here for more information](http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html).
This is required until Java 9 is released, as Java 8 does not have native support for HTTP/2.

## Installation
- Clone this repository, and add it as a dependent maven project
- Maven
```
<dependency>
  <groupId>com.clevertap.apns</groupId>
  <artifactId>apns-http2</artifactId>
  <version>1.0.3</version>
  <type>pom</type>
</dependency>
```
- Maven build steps
```
mvn package
```
The above command will create jars in ***target/*** directory. One out of those jar will be ***apns-http2-1.0.3-jar-with-dependencies.jar*** and ***apns-http2-1.0.3.jar***.


- Gradle  
```
compile 'com.clevertap.apns:apns-http2:1.0.3'
```

## Usage

### Create a client

#### Using provider certificates

```
FileInputStream cert = new FileInputStream("/path/to/certificate.p12");
final ApnsClient client = new ApnsClientBuilder()
        .withProductionGateway()
        .inSynchronousMode()
        .withCertificate(cert)
        .withPassword("")
        .withDefaultTopic("<your app's topic>")
        .build();
```

#### Using provider authentication tokens
```
final ApnsClient client = new ApnsClientBuilder()
        .inSynchronousMode()
        .withProductionGateway()
        .withApnsAuthKey("<your APNS auth key, excluding -----BEGIN PRIVATE KEY----- and -----END PRIVATE KEY----->")
        .withTeamID("<your team ID here>")
        .withKeyID("<your key ID here, present in the auth key file name>")
        .withDefaultTopic("<your app's topic>")
        .build();
```

### Build your notification
The notification builder supports several other features (such as badge, category, etc).
The minimal is shown below:

```
Notification n = new Notification.Builder("<the device token>")
        .alertBody("Hello").build();

```

### Send the notification

#### Asynchronous
 
```
client.push(n, new NotificationResponseListener() {
    @Override
    public void onSuccess(Notification notification) {
        System.out.println("success!");
    }
    @Override
    public void onFailure(Notification notification, NotificationResponse nr) {
        System.out.println("failure: " + nr);
    }
});
```

#### Synchronous

```
NotificationResponse result = client.push(n);
System.out.println(result);
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
