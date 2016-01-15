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

package com.clevertap.jetty.apns.http2.internal;

import com.clevertap.jetty.apns.http2.Notification;
import com.clevertap.jetty.apns.http2.NotificationResponseListener;
import com.clevertap.jetty.apns.http2.NotificationRequestError;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;

/**
 * A response listener for the request, based on the
 * BufferingResponseListener class of Jetty.
 * <p>
 * Also see {@link NotificationResponseListener}.
 */
public final class ResponseListener extends BufferingResponseListener {
    private final Notification notification;
    private final NotificationResponseListener nrl;

    public ResponseListener(Notification notification, NotificationResponseListener nrl) {
        this.notification = notification;
        this.nrl = nrl;
    }

    @Override
    public void onComplete(Result result) {
        Response response = result.getResponse();
        final int status = response.getStatus();

        if (nrl != null) {
            if (status == 200) {
                nrl.onSuccess(notification);
            } else {
                nrl.onFailure(notification, NotificationRequestError.get(status), getContentAsString());
            }
        }
    }
}
