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

/**
 * A wrapper around possible responses from the push gateway.
 */
public class NotificationResponse {
    private final NotificationRequestError error;
    private final int httpStatusCode;
    private final String responseBody;
    private final Throwable cause;

    public NotificationResponse(NotificationRequestError error, int httpStatusCode, String responseBody, Throwable cause) {
        this.error = error;
        this.httpStatusCode = httpStatusCode;
        this.responseBody = responseBody;
        this.cause = cause;
    }

    /**
     * Returns the throwable from the underlying HttpClient.
     *
     * @return The throwable
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Returns the error.
     *
     * @return The error (null if no error)
     */
    public NotificationRequestError getError() {
        return error;
    }

    /**
     * Returns the real HTTP status code.
     *
     * @return The HTTP status code
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * Returns the content body (null for a successful response).
     *
     * @return The content body (null for a successful response)
     */
    public String getResponseBody() {
        return responseBody;
    }

    @Override
    public String toString() {
        return "NotificationResponse{" +
                "error=" + error +
                ", httpStatusCode=" + httpStatusCode +
                ", responseBody='" + responseBody + '\'' +
                ", cause=" + cause +
                '}';
    }
}
