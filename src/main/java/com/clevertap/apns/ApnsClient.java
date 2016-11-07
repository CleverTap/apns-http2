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

package com.clevertap.apns;

import okhttp3.OkHttpClient;

/**
 * Interface for general purpose APNS clients.
 */
public interface ApnsClient {

    /**
     * Checks whether the client supports synchronous operations.
     * <p>
     * This is specified when building the client using
     *
     * @return Whether the client supports synchronous operations
     */
    boolean isSynchronous();

    /**
     * Sends a notification asynchronously to the Apple Push Notification Service.
     *
     * @param notification The notification built using
     *                     {@link Notification.Builder}
     * @param listener     The listener to be called after the request is complete
     */
    void push(Notification notification, NotificationResponseListener listener);

    /**
     * Sends a notification synchronously to the Apple Push Notification Service.
     *
     * @param notification The notification built using
     *                     {@link Notification.Builder}
     * @return The notification response
     */
    NotificationResponse push(Notification notification);

    /**
     * Returns the underlying OkHttpClient instance.
     * This can be used for further customizations such as using proxies.
     *
     * @return The underlying OkHttpClient instance
     */
    OkHttpClient getHttpClient();
}
