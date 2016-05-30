/*
 * Copyright (c) 2016, CleverTap
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

package com.clevertap.jetty.apns.http2.clients;

import com.clevertap.jetty.apns.http2.*;
import com.clevertap.jetty.apns.http2.internal.Constants;
import okhttp3.*;
import okio.BufferedSink;
import org.eclipse.jetty.client.HttpClient;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * User: Jude Pereira
 * Date: 23/03/2016
 * Time: 20:44
 */
public class SyncOkHttpApnsClient implements ApnsClient {

    private final String defaultTopic;
    private final OkHttpClient client;
    private final String gateway;
    private static final MediaType mediaType = MediaType.parse("application/json");

    /**
     * Creates a new client and automatically loads the key store
     * with the push certificate read from the input stream.
     *
     * @param certificate  The client certificate to be used
     * @param password     The password (if required, else null)
     * @param production   Whether to use the production endpoint or the sandbox endpoint
     * @param defaultTopic A default topic (can be changed per message)
     */
    public SyncOkHttpApnsClient(InputStream certificate, String password, boolean production,
                                String defaultTopic, ConnectionPool connectionPool)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            IOException, UnrecoverableKeyException, KeyManagementException {
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

        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS);

        builder.sslSocketFactory(sslSocketFactory);

        connectionPool = connectionPool == null ? new ConnectionPool(10, 10, TimeUnit.MINUTES) : connectionPool;
        builder.connectionPool(connectionPool);

        this.defaultTopic = defaultTopic;

        client = builder.build();

        if (production) {
            gateway = Constants.ENDPOINT_PRODUCTION;
        } else {
            gateway = Constants.ENDPOINT_SANDBOX;
        }
    }

    @Override
    public boolean isSynchronous() {
        return true;
    }

    @Override
    public HttpClient getHttpClient() {
        return null;
    }

    @Override
    public void push(Notification notification, NotificationResponseListener listener) {
        throw new UnsupportedOperationException("Asynchronous requests are not supported by this client");
    }

    @Override
    public NotificationResponse push(Notification notification) {
        final String topic = notification.getTopic() != null ? notification.getTopic() : defaultTopic;
        Request.Builder rb = new Request.Builder()
                .url(gateway + "/3/device/" + notification.getToken())

                .post(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return mediaType;
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        sink.write(notification.getPayload().getBytes(Constants.UTF_8));
                    }
                })
                .header("content-length", notification.getPayload().getBytes(Constants.UTF_8).length + "");

        if (topic != null) {
            rb.header("apns-topic", topic);
        }

        final Request request = rb.build();
        Response response = null;
        NotificationRequestError error = null;
        String contentBody = null;
        int statusCode = -1;

        Throwable cause = null;
        try {
            response = client.newCall(request).execute();
            statusCode = response.code();

            if (response.code() != 200) {
                error = NotificationRequestError.get(statusCode);
                contentBody = response.body() != null ? response.body().string() : null;
            }
        } catch (Throwable t) {
            cause = t;
        } finally {
            if (response != null) {
                response.body().close();
            }
        }

        return new NotificationResponse(error, statusCode, contentBody, cause);
    }

    @Override
    public void start() throws IOException {

    }

    @Override
    public void shutdown() throws Exception {

    }
}
