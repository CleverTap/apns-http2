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

import com.clevertap.apns.clients.AsyncOkHttpApnsClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.UUID;

/**
 * An entity containing the payload and the token.
 */
public class Notification {
    private final String payload;
    private final String token;
    private final String topic;
    private final String collapseId;
    private final long expiration;
    private final Priority priority;
    private final String pushType;
    private final UUID uuid;

    public enum Priority {
        IMMEDIATE(10),
        POWERCONSIDERATION(5);

        private final int code;

        private Priority(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }


    /**
     * Constructs a new Notification with a payload and token.
     *
     * @param payload    The JSON body (which is used for the request)
     * @param token      The device token
     * @param topic      The topic for this notification
     * @param collapseId The collapse ID
     * @param expiration A UNIX epoch date expressed in seconds (UTC)
     * @param priority   The priority of the notification (10 or 5)
     * @param uuid       A canonical UUID that identifies the notification
     * @param pushType   Type of push to be sent (background/alert etc)
     */
    protected Notification(String payload, String token, String topic, String collapseId,
        long expiration, Priority priority, UUID uuid, String pushType) {
        this.payload = payload;
        this.token = token;
        this.topic = topic;
        this.collapseId = collapseId;
        this.expiration = expiration;
        this.priority = priority;
        this.uuid = uuid;
        this.pushType = pushType;
    }

    /**
     * Retrieves the topic.
     *
     * @return The topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Retrieves the collapseId.
     *
     * @return The collapseId
     */
    public String getCollapseId() {
        return collapseId;
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

    public long getExpiration() {
        return expiration;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getPushType() {
        return pushType;
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * Builds a notification to be sent to APNS.
     */
    public static class Builder {
        private final ObjectMapper mapper = new ObjectMapper();

        private final HashMap<String, Object> root, aps, alert;
        private final String token;
        private String topic = null;
        private String collapseId = null;
        private long expiration = -1; // defaults to -1, as 0 is a valid value (included only if greater than -1)
        private Priority priority;
        private UUID uuid;
        private String pushType;

        /**
         * Creates a new notification builder.
         *
         * @param token The device token
         */
        public Builder(String token) {
            this.token = token;
            root = new HashMap<>();
            aps = new HashMap<>();
            alert = new HashMap<>();
        }

        public Builder mutableContent(boolean mutable) {
            if (mutable) {
                aps.put("mutable-content", 1);
            } else {
                aps.remove("mutable-content");
            }

            return this;
        }

        public Builder mutableContent() {
            return this.mutableContent(true);
        }

        public Builder contentAvailable(boolean contentAvailable) {
            if (contentAvailable) {
                aps.put("content-available", 1);
            } else {
                aps.remove("content-available");
            }

            return this;
        }

        public Builder contentAvailable() {
            return this.contentAvailable(true);
        }

        public Builder alertBody(String body) {
            alert.put("body", body);
            return this;
        }

        public Builder alertTitle(String title) {
            alert.put("title", title);
            return this;
        }

        public Builder urlArgs(String[] args) {
            aps.put("url-args", args);
            return this;
        }

        public Builder sound(String sound) {
            if (sound != null) {
                aps.put("sound", sound);
            } else {
                aps.remove("sound");
            }

            return this;
        }

        public Builder category(String category) {
            if (category != null) {
                aps.put("category", category);
            } else {
                aps.remove("category");
            }
            return this;
        }

        public Builder badge(int badge) {
            aps.put("badge", badge);
            return this;
        }

        public Builder customField(String key, Object value) {
            root.put(key, value);
            return this;
        }

        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder collapseId(String collapseId) {
            this.collapseId = collapseId;
            return this;
        }

        public Builder expiration(long expiration) {
            this.expiration = expiration;
            return this;
        }

        public Builder uuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder pushType(String pushType) {
            this.pushType = pushType;
            return this;
        }

        public int size() {
            try {
                return build().getPayload().getBytes("UTF-8").length;
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Builds the notification. Also see {@link AsyncOkHttpApnsClient#push(Notification,
         * NotificationResponseListener)}
         *
         * @return The notification
         */
        public Notification build() {
            root.put("aps", aps);

            //No need to add the alert if it's empty.
            if (alert.size() > 0) {
              aps.put("alert", alert);
            }

            final String payload;
            try {
                payload = mapper.writeValueAsString(root);
            } catch (JsonProcessingException e) {
                // Should not happen
                throw new RuntimeException(e);
            }
            return new Notification(payload, token, topic, collapseId, expiration, priority, uuid,
                pushType);
        }
    }
}
