/*
 * Copyright (c) 2018, CleverTap
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of CleverTap nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.clevertap.apns.clients;

import com.clevertap.apns.*;
import com.clevertap.apns.internal.Constants;
import com.clevertap.apns.internal.JWT;
import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;
import jdk.incubator.http.HttpResponse.BodyHandler;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.UUID;

/**
 * User: Jude Pereira
 * Date: 04/01/2018
 * Time: 09:19
 */
public class SyncApnsClient implements ApnsClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private final HttpClient client;
    private final String gateway;
    private final String defaultTopic;
    private final String keyID;
    private final String teamID;
    private final String apnsAuthKey;

    private long lastJWTTokenTS = 0;
    private String cachedJWTToken = null;

    public SyncApnsClient(String apnsAuthKey, String teamID, String keyID, boolean production,
                          String defaultTopic, HttpClient.Builder httpClientBuilder) {

        if (httpClientBuilder == null) {
            httpClientBuilder = ApnsClientBuilder.createDefaultHttpClientBuilder();
        }

        this.client = httpClientBuilder
                .version(HttpClient.Version.HTTP_2)
                .build();

        this.apnsAuthKey = apnsAuthKey;
        this.teamID = teamID;
        this.keyID = keyID;
        this.defaultTopic = defaultTopic;

        this.gateway = production ? Constants.ENDPOINT_PRODUCTION : Constants.ENDPOINT_SANDBOX;
    }

    public SyncApnsClient(InputStream certificate, String password, boolean production,
                          String defaultTopic, HttpClient.Builder httpClientBuilder)
            throws CertificateException, IOException, NoSuchAlgorithmException,
            KeyStoreException, UnrecoverableKeyException, KeyManagementException {

        teamID = keyID = apnsAuthKey = null;

        password = password == null ? "" : password;
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(certificate, password.toCharArray());

        final X509Certificate cert = (X509Certificate) ks.getCertificate(ks.aliases().nextElement());
        CertificateUtils.validateCertificate(production, cert);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());
        KeyManager[] keyManagers = kmf.getKeyManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        sslContext.init(keyManagers, tmf.getTrustManagers(), null);

        if (httpClientBuilder == null) {
            httpClientBuilder = ApnsClientBuilder.createDefaultHttpClientBuilder();
        }

        this.client = httpClientBuilder
                .version(HttpClient.Version.HTTP_2)
                .build();

        this.defaultTopic = defaultTopic;

        this.gateway = production ? Constants.ENDPOINT_PRODUCTION : Constants.ENDPOINT_SANDBOX;
    }

    /**
     * Lazy initialization. See {@link #makeInternalApiAccessible(HttpRequest)}.
     */
    private static Method setHeaderMethod = null;

    /**
     * This makes the internal method setSystemHeader accessible to us via reflection.
     * This is required to set the authorization header. If the authorization header
     * is set using HttpRequest, then it blissfully disregards it, as authorization
     * is a forbidden user header.
     * <p>
     * We've filed a bug (https://bugs.java.com/view_bug.do?bug_id=JDK-8194729)
     * with Oracle, to allow setting the authorization header
     * outside the app. Until that gets resolved, we'll stick to reflection.
     * <p>
     * See https://github.com/CleverTap/apns-http2/wiki/Running-on-Java-9.
     *
     * @param request The HttpRequest object
     */
    private static void makeInternalApiAccessible(HttpRequest request) throws Exception {
        if (setHeaderMethod != null) return;

        synchronized (SyncApnsClient.class) {
            if (setHeaderMethod != null) return;

            Class reqClass = request.getClass();
            try {
                //noinspection unchecked
                setHeaderMethod = reqClass.getDeclaredMethod("setSystemHeader", String.class, String.class);
            } catch (NoSuchMethodException e) {
                throw new Exception("It seems that the setSystemHeader API has been removed " +
                        "(or the bug has been resolved by Oracle). " +
                        "Please file an issue here: https://github.com/CleverTap/apns-http2");
            }

            try {
                setHeaderMethod.setAccessible(true);
            } catch (InaccessibleObjectException e) {
                throw new Exception("Please add \"--add-opens jdk.incubator.httpclient/" +
                        "jdk.incubator.http=com.clevertap.apns\". " +
                        "https://github.com/CleverTap/apns-http2/wiki/Running-on-Java-9");
            }
        }
    }

    @Override
    public boolean isSynchronous() {
        return true;
    }

    @Override
    public void push(Notification notification, NotificationResponseListener listener) {
        throw new UnsupportedOperationException("Asynchronous requests are not supported by this client");
    }

    protected HttpRequest buildRequest(Notification notification) throws Exception {
        final String topic = notification.getTopic() != null ? notification.getTopic() : defaultTopic;
        final String collapseId = notification.getCollapseId();
        final UUID uuid = notification.getUuid();
        final long expiration = notification.getExpiration();
        final Notification.Priority priority = notification.getPriority();
        final byte[] payload = notification.getPayload().getBytes(StandardCharsets.UTF_8);

        final HttpRequest.Builder rb = HttpRequest
                .newBuilder()
                .POST(HttpRequest.BodyProcessor.fromByteArray(payload))
                .uri(new URI(gateway + "/3/device/" + notification.getToken()))
                .header("content-type", "application/json; charset=utf-8")
                .header("content-length", String.valueOf(payload.length));

        if (topic != null) {
            rb.header("apns-topic", topic);
        }

        if (collapseId != null) {
            rb.header("apns-collapse-id", collapseId);
        }

        if (uuid != null) {
            rb.header("apns-id", uuid.toString());
        }

        if (expiration > -1) {
            rb.header("apns-expiration", String.valueOf(expiration));
        }

        if (priority != null) {
            rb.header("apns-priority", String.valueOf(priority.getCode()));
        }

        if (keyID != null && teamID != null && apnsAuthKey != null) {
            // Generate a new JWT token if it's null, or older than 55 minutes
            if (isJWTTokenStale()) {
                synchronized (this) {
                    if (isJWTTokenStale()) {
                        lastJWTTokenTS = System.currentTimeMillis();
                        cachedJWTToken = JWT.getToken(teamID, keyID, apnsAuthKey);
                    }
                }
            }
        }

        rb.timeout(REQUEST_TIMEOUT);

        final HttpRequest request = rb.build();

        // Hack until bug https://bugs.java.com/view_bug.do?bug_id=JDK-8194729 gets resolved by Oracle.
        makeInternalApiAccessible(request);
        setHeaderMethod.invoke(request, "authorization", "bearer " + cachedJWTToken);

        return request;
    }

    @Override
    public NotificationResponse push(Notification notification) {
        try {
            final HttpRequest request = buildRequest(notification);

            HttpResponse<String> response = client.send(request, BodyHandler.asString());
            return parseResponse(response);
        } catch (Throwable t) {
            return new NotificationResponse(null, -1, null, t);
        }
    }

    private boolean isJWTTokenStale() {
        return cachedJWTToken == null || System.currentTimeMillis() - lastJWTTokenTS > 55 * 60 * 1000;
    }

    protected NotificationResponse parseResponse(HttpResponse<String> response) {
        String contentBody = null;
        int statusCode = response.statusCode();

        NotificationRequestError error = null;

        if (statusCode != 200) {
            error = NotificationRequestError.get(statusCode);
            contentBody = response.body();
        }

        return new NotificationResponse(error, statusCode, contentBody, null);
    }

    @Override
    public HttpClient getHttpClient() {
        return client;
    }
}
