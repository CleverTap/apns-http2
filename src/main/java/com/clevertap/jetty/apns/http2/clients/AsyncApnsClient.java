/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 CleverTap
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.clevertap.jetty.apns.http2.clients;

import com.clevertap.jetty.apns.http2.ApnsClient;
import com.clevertap.jetty.apns.http2.Notification;
import com.clevertap.jetty.apns.http2.NotificationResponseListener;
import com.clevertap.jetty.apns.http2.internal.Constants;
import com.clevertap.jetty.apns.http2.internal.ResponseListener;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.Semaphore;

/**
 * A wrapper around Jetty's HttpClient to send out notifications using Apple's HTTP/2 API.
 */
public class AsyncApnsClient implements ApnsClient {
    private static final Logger logger = LoggerFactory.getLogger(AsyncApnsClient.class);

    protected final HttpClient client;

    protected final String gateway;

    /**
     * This semaphore is used as we cannot tell whether Jetty's internal queue is full or not.
     * Hopefully, this will change in the future.
     */
    private final Semaphore semaphore;

    /**
     * Creates a new client and automatically loads the key store
     * with the push certificate read from the input stream.
     *
     * @param certificate The client certificate to be used
     * @param password    The password (if required, else null)
     * @param production  Whether to use the production endpoint or the sandbox endpoint
     */
    public AsyncApnsClient(InputStream certificate, String password, boolean production, int maxRequestsQueued)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        password = password == null ? "" : password;
        SslContextFactory sslContext = new SslContextFactory(false);
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(certificate, password.toCharArray());
        sslContext.setKeyStore(ks);
        sslContext.setKeyStorePassword(password);
        client = new HttpClient(new HttpClientTransportOverHTTP2(new HTTP2Client()), sslContext);

        setMaxConnections(1);
        client.setMaxRequestsQueuedPerDestination(maxRequestsQueued);
        try {
            client.start();
        } catch (Exception e) {
            throw new IOException(e);
        }

        if (production) {
            gateway = Constants.ENDPOINT_PRODUCTION;
        } else {
            gateway = Constants.ENDPOINT_SANDBOX;
        }

        semaphore = new Semaphore(maxRequestsQueued);

        logger.debug("HTTP/2 client started...");
    }

    /**
     * Creates a new client and automatically loads the key store
     * with the push certificate read from the input stream.
     * <p>
     * Same as calling {@link AsyncApnsClient#AsyncApnsClient(InputStream, String, boolean, int)}
     * with maximum queued requests as 2000.
     */
    public AsyncApnsClient(InputStream certificate, String password, boolean production)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        this(certificate, password, production, 2000);
    }

    /**
     * Returns the underlying HTTP client.
     *
     * @return The HttpClient instance to send messages
     */
    public HttpClient getHttpClient() {
        return client;
    }

    /**
     * Sets the underlying HttpClient's maximum connections per destination.
     * Generally, one connection can handle up to 2000 push notifications per second.
     * <p>
     * Default is 1.
     *
     * @param maxConnections The number of connections to keep open
     */
    public void setMaxConnections(int maxConnections) {
        client.setMaxConnectionsPerDestination(maxConnections);
    }

    /**
     * Sends a notification to the Apple Push Notification Service.
     *
     * @param notification The notification built using
     *                     {@link com.clevertap.jetty.apns.http2.Notification.Builder}
     * @param listener     The listener to be called after the request is complete
     */
    public void push(Notification notification, NotificationResponseListener listener) {
        _push(notification, listener);
    }

    private void _push(Notification notification, NotificationResponseListener listener) {
        Request req = client.POST(gateway)
                .path("/3/device/" + notification.getToken())
                .content(new StringContentProvider(notification.getPayload(), Charset.forName("UTF-8")));

        semaphore.acquireUninterruptibly();

        req.send(new ResponseListener(semaphore, notification, listener));
    }

    public void shutdown() throws Exception {
        client.stop();
    }
}
