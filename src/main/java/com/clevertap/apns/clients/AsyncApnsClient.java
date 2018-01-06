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

import com.clevertap.apns.Notification;
import com.clevertap.apns.NotificationResponse;
import com.clevertap.apns.NotificationResponseListener;
import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.CompletableFuture;

/**
 * User: Jude Pereira
 * Date: 06/01/2018
 * Time: 20:30
 */
public class AsyncApnsClient extends SyncApnsClient {
    public AsyncApnsClient(String apnsAuthKey, String teamID, String keyID, boolean production, String defaultTopic, HttpClient.Builder httpClientBuilder) {
        super(apnsAuthKey, teamID, keyID, production, defaultTopic, httpClientBuilder);
    }

    public AsyncApnsClient(InputStream certificate, String password, boolean production, String defaultTopic, HttpClient.Builder httpClientBuilder) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        super(certificate, password, production, defaultTopic, httpClientBuilder);
    }

    @Override
    public boolean isSynchronous() {
        return false;
    }

    @Override
    public void push(Notification notification, NotificationResponseListener listener) {

        try {
            final HttpRequest request = buildRequest(notification);
            final CompletableFuture<HttpResponse<String>> future = getHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandler.asString());

            future.handleAsync((response, throwable) -> {
                try {
                    final NotificationResponse nr = parseResponse(response);
                    if (nr.getHttpStatusCode() == 200) {
                        listener.onSuccess(notification);
                    } else {
                        listener.onFailure(notification, nr);
                    }
                } catch (Throwable t) {
                    listener.onFailure(notification, new NotificationResponse(null, 0, null, t));
                }

                return null;
            });
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public NotificationResponse push(Notification notification) {
        throw new UnsupportedOperationException("Synchronous requests are not supported by this client");
    }
}
