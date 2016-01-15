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

/**
 * User: Jude Pereira
 * Date: 15/01/2016
 * Time: 21:58
 */
public class ApplePushClient {
    private static final Logger logger = LoggerFactory.getLogger(ApplePushClient.class);

    protected final HttpClient client;

    private final String gateway;
    private NotificationResponseListener defaultNotificationListener;

    public void setDefaultNotificationListener(NotificationResponseListener listener) {
        this.defaultNotificationListener = listener;
    }

    public ApplePushClient(InputStream certificate, String password, boolean production)
            throws KeyStoreException, IOException {
        SslContextFactory sslContext = new SslContextFactory(false);
        KeyStore ks = KeyStore.getInstance("PKCS12");
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

        logger.debug("HTTP/2 client started...");
    }

    public ApplePushClient(HttpClient httpClient, boolean production) {
        this.client = httpClient;

        if (production) {
            gateway = Constants.ENDPOINT_PRODUCTION;
        } else {
            gateway = Constants.ENDPOINT_SANDBOX;
        }
    }

    public void setMaxConnections(int maxConnections) {
        client.setMaxConnectionsPerDestination(maxConnections);
    }

    public void setMaxRequestsQueued(int maxRequestsQueued) {
        client.setMaxRequestsQueuedPerDestination(maxRequestsQueued);
    }

    public void push(Notification notification, NotificationResponseListener listener) {
        Request req = client.POST(gateway)
                .path("/3/device/" + notification.getToken())
                .content(new StringContentProvider(notification.getPayload()));

        req.send(new ResponseListener(notification, listener));
    }

    public void push(Notification notification) {
        push(notification, defaultNotificationListener);
    }
}
