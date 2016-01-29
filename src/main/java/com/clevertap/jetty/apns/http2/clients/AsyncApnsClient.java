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

import com.clevertap.jetty.apns.http2.ApnsClient;
import com.clevertap.jetty.apns.http2.Notification;
import com.clevertap.jetty.apns.http2.NotificationResponse;
import com.clevertap.jetty.apns.http2.NotificationResponseListener;
import com.clevertap.jetty.apns.http2.internal.Constants;
import com.clevertap.jetty.apns.http2.internal.ResponseListener;
import com.clevertap.jetty.apns.http2.internal.Utils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

/**
 * A wrapper around Jetty's HttpClient to send out notifications using Apple's HTTP/2 API.
 */
public class AsyncApnsClient implements ApnsClient {
    private static final Logger logger = LoggerFactory.getLogger(AsyncApnsClient.class);

    protected final HttpClient client;

    protected final String gateway;
    private final int maxRequestsQueued;

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
     * @param semaphore   A semaphore used to control the notifications queued
     */
    public AsyncApnsClient(InputStream certificate, String password, boolean production, int maxRequestsQueued, Semaphore semaphore)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        this.client = Utils.buildClient(certificate, password);

        setMaxConnections(1);
        client.setMaxRequestsQueuedPerDestination(maxRequestsQueued);

        if (production) {
            gateway = Constants.ENDPOINT_PRODUCTION;
        } else {
            gateway = Constants.ENDPOINT_SANDBOX;
        }

        this.semaphore = semaphore;
        this.maxRequestsQueued = maxRequestsQueued;
    }

    public void start() throws IOException {
        try {
            client.start();
            logger.debug("HTTP/2 client started...");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

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

    public boolean isSynchronous() {
        return false;
    }

    @Override
    public void push(String topic, Notification notification, NotificationResponseListener listener) {
        _push(topic, notification, listener);

    }

    /**
     * Sends a notification to the Apple Push Notification Service.
     *
     * @param notification The notification built using
     *                     {@link com.clevertap.jetty.apns.http2.Notification.Builder}
     * @param listener     The listener to be called after the request is complete
     */
    public void push(Notification notification, NotificationResponseListener listener) {
        _push(null, notification, listener);
    }

    public NotificationResponse push(Notification notification) {
        throw new UnsupportedOperationException("Synchronous requests are not supported by this client");
    }

    private void _push(String topic, Notification notification, NotificationResponseListener listener) {
        Request req = Utils.buildRequest(client, topic, notification, gateway);

        semaphore.acquireUninterruptibly();

        req.send(new ResponseListener(semaphore, notification, listener));
    }

    public void shutdown() throws Exception {
        // todo: must maintain a shutting down flag, and throw an IllegalArgumentException for further calls to push()
        semaphore.acquireUninterruptibly(maxRequestsQueued);
        client.stop();
        semaphore.release(maxRequestsQueued);
    }

    @Override
    public NotificationResponse push(String topic, Notification notification)
            throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("Synchronous requests are not supported by this client");
    }
}
