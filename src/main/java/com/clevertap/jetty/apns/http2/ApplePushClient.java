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

package com.clevertap.jetty.apns.http2;

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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * A wrapper around HttpClient to send out notifications using Apple's HTTP/2 API.
 */
public class ApplePushClient {
    private static final Logger logger = LoggerFactory.getLogger(ApplePushClient.class);

    protected final HttpClient client;

    private final String gateway;
    private NotificationResponseListener defaultNotificationListener;

    /**
     * Set a default notification response listener.
     *
     * @param listener The notification response listener
     */
    public void setDefaultNotificationListener(NotificationResponseListener listener) {
        this.defaultNotificationListener = listener;
    }

    /**
     * Creates a new client and automatically loads the key store
     * with the push certificate read from the input stream.
     *
     * @param certificate The client certificate to be used
     * @param password    The password (if required, else null)
     * @param production  Whether to use the production endpoint or the sandbox endpoint
     */
    public ApplePushClient(InputStream certificate, String password, boolean production)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        password = password == null ? "" : password;
        SslContextFactory sslContext = new SslContextFactory(false);
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(certificate, password.toCharArray());
        sslContext.setKeyStore(ks);
        sslContext.setKeyStorePassword(password);
        client = new HttpClient(new HttpClientTransportOverHTTP2(new HTTP2Client()), sslContext);
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

        setMaxConnections(1);
        setMaxRequestsQueued(1000);

        logger.debug("HTTP/2 client started...");
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
     * Sets the underlying HttpClient's maximum requests to be queued.
     * <p>
     * Default is 1000;
     *
     * @param maxRequestsQueued The number of queued requests
     */
    public void setMaxRequestsQueued(int maxRequestsQueued) {
        client.setMaxRequestsQueuedPerDestination(maxRequestsQueued);
    }

    /**
     * Sends a notification to the Apple Push Notification Service.
     *
     * @param notification The notification built using
     *                     {@link com.clevertap.jetty.apns.http2.Notification.Builder}
     * @param listener     The listener to be called after the request is complete
     */
    public void push(Notification notification, NotificationResponseListener listener) {
        Request req = client.POST(gateway)
                .path("/3/device/" + notification.getToken())
                .content(new StringContentProvider(notification.getPayload()));

        req.send(new ResponseListener(notification, listener));
    }

    /**
     * Sends a notification to the Apple Push Notification Service.
     * <p>
     * The default notification listener, if set, will be called after the request is complete
     *
     * @param notification The notification built using
     *                     {@link com.clevertap.jetty.apns.http2.Notification.Builder}
     */
    public void push(Notification notification) {
        push(notification, defaultNotificationListener);
    }
}
