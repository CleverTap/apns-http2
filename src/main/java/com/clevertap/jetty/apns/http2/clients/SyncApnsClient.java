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
import com.clevertap.jetty.apns.http2.internal.Utils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * A wrapper around Jetty's HttpClient to send out notifications using Apple's HTTP/2 API.
 */
public class SyncApnsClient implements ApnsClient {
    private static final Logger logger = LoggerFactory.getLogger(SyncApnsClient.class);
    protected final HttpClient client;

    protected final String gateway;
    protected final String defaultTopic;

    /**
     * Creates a new client and automatically loads the key store
     * with the push certificate read from the input stream.
     *
     * @param certificate  The client certificate to be used
     * @param password     The password (if required, else null)
     * @param production   Whether to use the production endpoint or the sandbox endpoint
     * @param defaultTopic A default topic (can be changed per message)
     */
    public SyncApnsClient(InputStream certificate, String password, boolean production, String defaultTopic)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        this.defaultTopic = defaultTopic;
        client = Utils.buildClient(certificate, password, production);

        if (production) {
            gateway = Constants.ENDPOINT_PRODUCTION;
        } else {
            gateway = Constants.ENDPOINT_SANDBOX;
        }
    }

    public boolean isSynchronous() {
        return true;
    }

    @Override
    public HttpClient getHttpClient() {
        return client;
    }

    @Override
    public void push(Notification notification, NotificationResponseListener listener) {
        throw new UnsupportedOperationException("Asynchronous requests are not supported by this client");
    }

    @Override
    public NotificationResponse push(Notification notification) {

        final String notificationTopic = notification.getTopic();
        final String topic = notificationTopic == null ? defaultTopic : notificationTopic;

        Request req = Utils.buildRequest(client, topic, notification, gateway);

        Throwable cause;
        ContentResponse cr;
        try {
            cr = req.send();
            cause = null;
        } catch (Throwable t) {
            cause = t;
            cr = null;
        }

        final int statusCode = cr != null ? cr.getStatus() : -1;
        final NotificationRequestError error;
        final String contentBody;

        if (statusCode != 200) {
            error = NotificationRequestError.get(statusCode);
            contentBody = cr != null ? cr.getContentAsString() : null;
        } else {
            error = null;
            contentBody = null;
        }

        return new NotificationResponse(error, statusCode, contentBody, cause);
    }

    public void start() throws IOException {
        try {
            client.start();
            logger.debug("HTTP/2 client started...");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void shutdown() throws Exception {
        client.stop();
    }
}
