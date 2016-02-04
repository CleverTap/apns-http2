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

package com.clevertap.jetty.apns.http2.internal;

import com.clevertap.jetty.apns.http2.Notification;
import com.clevertap.jetty.apns.http2.NotificationRequestError;
import com.clevertap.jetty.apns.http2.NotificationResponseListener;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

/**
 * A response listener for the request, based on the
 * BufferingResponseListener class of Jetty.
 * <p>
 * Also see {@link NotificationResponseListener}.
 */
public final class ResponseListener extends BufferingResponseListener {
    private static final Logger logger = LoggerFactory.getLogger(ResponseListener.class);
    private final Semaphore semaphore;
    private final Notification notification;
    private final NotificationResponseListener nrl;

    public ResponseListener(Semaphore semaphore, Notification notification, NotificationResponseListener nrl) {
        this.semaphore = semaphore;
        this.notification = notification;
        this.nrl = nrl;
    }

    @Override
    public void onComplete(Result result) {
        try {
            if (nrl != null) {
                Response response = result.getResponse();
                int status = response.getStatus();
                if (status == 200) {
                    nrl.onSuccess(notification);
                } else {
                    nrl.onFailure(notification, NotificationRequestError.get(status), getContentAsString());
                }
            }
        } catch (Throwable t) {
            logger.error("Failed to execute the response listener", t);
        } finally {
            semaphore.release();
        }
    }
}
