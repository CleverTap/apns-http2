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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;

/**
 * An entity containing the payload and the token.
 */
public class Notification {
    private final String payload;
    private final String token;

    /**
     * Constructs a new Notification with a payload and token.
     *
     * @param payload The JSON body (which is used for the request)
     * @param token   The device token
     */
    protected Notification(String payload, String token) {
        this.payload = payload;
        this.token = token;
    }

    /**
     * Retrieves the payload.
     *
     * @return The payload
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Retrieves the token.
     *
     * @return The device token
     */
    public String getToken() {
        return token;
    }

    /**
     * Builds a notification to be sent to APNS.
     */
    public static class Builder {
        private final ObjectMapper mapper = new ObjectMapper();

        private final HashMap<String, Object> root;
        private final HashMap<String, Object> aps;
        private final String token;

        /**
         * Creates a new notification builder.
         *
         * @param token The device token
         */
        public Builder(String token) {
            this.token = token;
            root = new HashMap<>();
            aps = new HashMap<>();
            root.put("aps", aps);
        }

        /**
         * Builds the notification.
         * Also see {@link ApplePushClient#push(Notification, NotificationResponseListener)}
         *
         * @return The notification
         */
        public Notification build() {
            final String payload;
            try {
                payload = mapper.writeValueAsString(root);
            } catch (JsonProcessingException e) {
                // Should not happen
                throw new RuntimeException(e);
            }
            return new Notification(payload, token);
        }
    }
}
