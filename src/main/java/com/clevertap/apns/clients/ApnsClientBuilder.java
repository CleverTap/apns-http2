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

package com.clevertap.apns.clients;

import com.clevertap.apns.ApnsClient;
import jdk.incubator.http.HttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * A builder to build an APNS client.
 */
public class ApnsClientBuilder {
    private InputStream certificate;
    private boolean production;
    private String password;

    private boolean asynchronous = false;
    private String defaultTopic = null;

    private HttpClient.Builder builder;
    private String apnsAuthKey;
    private String teamID;
    private String keyID;

    /**
     * Creates a default {@link HttpClient} builder that can be customized later and
     * then passed to one of the constructors taking a builder instance. The
     * constructors that don't take builders themselves use this method
     * internally to create their client builders.
     *
     * @return a new {@link HttpClient} builder, initialized with default settings.
     */
    public static HttpClient.Builder createDefaultHttpClientBuilder() {
        // TODO: 04/01/2018 connection pooling and timeouts
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2);
    }

    /**
     * Replaces the default HttpClient builder with this one. The default
     * builder is created internally with {@link #createDefaultHttpClientBuilder() }.
     * A custom builder can also be created by calling that method explicitly,
     * customizing the builder and then passing it to this method.
     *
     * @param clientBuilder An existing HttpClient builder to be used as the base
     * @return this object
     */
    public ApnsClientBuilder withHttpClientBuilder(HttpClient.Builder clientBuilder) {
        this.builder = clientBuilder;
        return this;
    }

    public ApnsClientBuilder withCertificate(InputStream inputStream) {
        certificate = inputStream;
        return this;
    }

    public ApnsClientBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public ApnsClientBuilder withApnsAuthKey(String apnsAuthKey) {
        this.apnsAuthKey = apnsAuthKey;
        return this;
    }

    public ApnsClientBuilder withTeamID(String teamID) {
        this.teamID = teamID;
        return this;
    }

    public ApnsClientBuilder withKeyID(String keyID) {
        this.keyID = keyID;
        return this;
    }

    public ApnsClientBuilder withProductionGateway() {
        this.production = true;
        return this;
    }

    public ApnsClientBuilder withProductionGateway(boolean production) {
        if (production) return withProductionGateway();

        return withDevelopmentGateway();
    }

    public ApnsClientBuilder withDevelopmentGateway() {
        this.production = false;
        return this;
    }

    public ApnsClientBuilder inSynchronousMode() {
        asynchronous = false;
        return this;
    }

    public ApnsClientBuilder inAsynchronousMode() {
        asynchronous = true;
        return this;
    }

    public ApnsClientBuilder withDefaultTopic(String defaultTopic) {
        this.defaultTopic = defaultTopic;
        return this;
    }

    public ApnsClient build() throws CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException,
            UnrecoverableKeyException, KeyManagementException {

        if (builder == null) {
            builder = createDefaultHttpClientBuilder();
        }

        if (certificate != null) {
            if (asynchronous) {
                return new AsyncApnsClient(certificate, password, production, defaultTopic, builder);
            } else {
                return new SyncApnsClient(certificate, password, production, defaultTopic, builder);
            }
        } else if (keyID != null && teamID != null && apnsAuthKey != null) {
            if (asynchronous) {
                return new AsyncApnsClient(apnsAuthKey, teamID, keyID, production, defaultTopic, builder);
            } else {
                return new SyncApnsClient(apnsAuthKey, teamID, keyID, production, defaultTopic, builder);
            }
        } else {
            throw new IllegalArgumentException("Either the token credentials (team ID, key ID, and the private key) " +
                    "or a certificate must be provided");
        }
    }
}
