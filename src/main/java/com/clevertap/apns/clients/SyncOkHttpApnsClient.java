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

import com.clevertap.apns.*;
import com.clevertap.apns.internal.Constants;
import com.clevertap.apns.internal.JWT;
import okhttp3.*;
import okio.BufferedSink;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

/**
 * A wrapper around OkHttp's http client to send out notifications using Apple's HTTP/2 API.
 */
public class SyncOkHttpApnsClient implements ApnsClient {

    private final String defaultTopic;
    private final String apnsAuthKey;
    private final String teamID;
    private final String keyID;
    protected final OkHttpClient client;
    private final String gateway;
    private static final MediaType mediaType = MediaType.parse("application/json");

    private long lastJWTTokenTS = 0;
    private String cachedJWTToken = null;

    private boolean isVoip;

    /**
     * Creates a new client which uses token authentication API.
     *
     * @param apnsAuthKey   The private key - exclude -----BEGIN PRIVATE KEY----- and -----END PRIVATE KEY-----
     * @param teamID        The team ID
     * @param keyID         The key ID (retrieved from the file name)
     * @param production    Whether to use the production endpoint or the sandbox endpoint
     * @param defaultTopic  A default topic (can be changed per message)
     * @param clientBuilder An OkHttp client builder, possibly pre-initialized, to build the actual client
     */
    public SyncOkHttpApnsClient(String apnsAuthKey, String teamID, String keyID, boolean production,
                                String defaultTopic, OkHttpClient.Builder clientBuilder) {
        this(apnsAuthKey, teamID, keyID, production, defaultTopic, clientBuilder, 443);
    }

    /**
     * Creates a new client which uses token authentication API.
     *
     * @param apnsAuthKey    The private key - exclude -----BEGIN PRIVATE KEY----- and -----END PRIVATE KEY-----
     * @param teamID         The team ID
     * @param keyID          The key ID (retrieved from the file name)
     * @param production     Whether to use the production endpoint or the sandbox endpoint
     * @param defaultTopic   A default topic (can be changed per message)
     * @param clientBuilder  An OkHttp client builder, possibly pre-initialized, to build the actual client
     * @param connectionPort The port to establish a connection with APNs. Either 443 or 2197
     */
    public SyncOkHttpApnsClient(String apnsAuthKey, String teamID, String keyID, boolean production,
                                String defaultTopic, OkHttpClient.Builder clientBuilder, int connectionPort) {
        this.apnsAuthKey = apnsAuthKey;
        this.teamID = teamID;
        this.keyID = keyID;
        client = clientBuilder.build();

        this.defaultTopic = defaultTopic;

        gateway = (production ? Constants.ENDPOINT_PRODUCTION : Constants.ENDPOINT_SANDBOX) + ":" + connectionPort;
    }

    /**
     * Creates a new client which uses token authentication API.
     *
     * @param apnsAuthKey    The private key - exclude -----BEGIN PRIVATE KEY----- and -----END PRIVATE KEY-----
     * @param teamID         The team ID
     * @param keyID          The key ID (retrieved from the file name)
     * @param production     Whether to use the production endpoint or the sandbox endpoint
     * @param defaultTopic   A default topic (can be changed per message)
     * @param connectionPool A connection pool to use. If null, a new one will be generated
     */
    public SyncOkHttpApnsClient(String apnsAuthKey, String teamID, String keyID, boolean production,
                                String defaultTopic, ConnectionPool connectionPool) {

        this(apnsAuthKey, teamID, keyID, production, defaultTopic, getBuilder(connectionPool));
    }

    /**
     * Creates a new client and automatically loads the key store
     * with the push certificate read from the input stream.
     *
     * @param certificate  The client certificate to be used
     * @param password     The password (if required, else null)
     * @param production   Whether to use the production endpoint or the sandbox endpoint
     * @param defaultTopic A default topic (can be changed per message)
     * @param builder      An OkHttp client builder, possibly pre-initialized, to build the actual client
     * @throws UnrecoverableKeyException If the key cannot be recovered
     * @throws KeyManagementException    if the key failed to be loaded
     * @throws CertificateException      if any of the certificates in the keystore could not be loaded
     * @throws NoSuchAlgorithmException  if the algorithm used to check the integrity of the keystore cannot be found
     * @throws IOException               if there is an I/O or format problem with the keystore data,
     *                                   if a password is required but not given, or if the given password was incorrect
     * @throws KeyStoreException         if no Provider supports a KeyStoreSpi implementation for the specified type
     */
    public SyncOkHttpApnsClient(InputStream certificate, String password, boolean production,
                                String defaultTopic, OkHttpClient.Builder builder)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            IOException, UnrecoverableKeyException, KeyManagementException {
        this(certificate, password, production, defaultTopic, builder, 443);
    }

    /**
     * Creates a new client and automatically loads the key store
     * with the push certificate read from the input stream.
     *
     * @param certificate    The client certificate to be used
     * @param password       The password (if required, else null)
     * @param production     Whether to use the production endpoint or the sandbox endpoint
     * @param defaultTopic   A default topic (can be changed per message)
     * @param builder        An OkHttp client builder, possibly pre-initialized, to build the actual client
     * @param connectionPort The port to establish a connection with APNs. Either 443 or 2197
     * @throws UnrecoverableKeyException If the key cannot be recovered
     * @throws KeyManagementException    if the key failed to be loaded
     * @throws CertificateException      if any of the certificates in the keystore could not be loaded
     * @throws NoSuchAlgorithmException  if the algorithm used to check the integrity of the keystore cannot be found
     * @throws IOException               if there is an I/O or format problem with the keystore data,
     *                                   if a password is required but not given, or if the given password was incorrect
     * @throws KeyStoreException         if no Provider supports a KeyStoreSpi implementation for the specified type
     */
    public SyncOkHttpApnsClient(InputStream certificate, String password, boolean production,
                                String defaultTopic, OkHttpClient.Builder builder, int connectionPort)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            IOException, UnrecoverableKeyException, KeyManagementException {

        teamID = keyID = apnsAuthKey = null;

        password = password == null ? "" : password;
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(certificate, password.toCharArray());

        final X509Certificate cert = (X509Certificate) ks.getCertificate(ks.aliases().nextElement());
        CertificateUtils.validateCertificate(production, cert);

        this.isVoip = CertificateUtils.checkIsVoipCertificate(cert);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());
        KeyManager[] keyManagers = kmf.getKeyManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        sslContext.init(keyManagers, tmf.getTrustManagers(), null);

        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        builder.sslSocketFactory(sslSocketFactory);

        client = builder.build();

        this.defaultTopic = defaultTopic;
        gateway = (production ? Constants.ENDPOINT_PRODUCTION : Constants.ENDPOINT_SANDBOX) + ":" + connectionPort;
    }

    /**
     * Creates a new client and automatically loads the key store
     * with the push certificate read from the input stream.
     *
     * @param certificate    The client certificate to be used
     * @param password       The password (if required, else null)
     * @param production     Whether to use the production endpoint or the sandbox endpoint
     * @param defaultTopic   A default topic (can be changed per message)
     * @param connectionPool A connection pool to use. If null, a new one will be generated
     * @throws UnrecoverableKeyException If the key cannot be recovered
     * @throws KeyManagementException    if the key failed to be loaded
     * @throws CertificateException      if any of the certificates in the keystore could not be loaded
     * @throws NoSuchAlgorithmException  if the algorithm used to check the integrity of the keystore cannot be found
     * @throws IOException               if there is an I/O or format problem with the keystore data,
     *                                   if a password is required but not given, or if the given password was incorrect
     * @throws KeyStoreException         if no Provider supports a KeyStoreSpi implementation for the specified type
     */
    public SyncOkHttpApnsClient(InputStream certificate, String password, boolean production,
                                String defaultTopic, ConnectionPool connectionPool)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            IOException, UnrecoverableKeyException, KeyManagementException {

        this(certificate, password, production, defaultTopic, getBuilder(connectionPool));
    }

    /**
     * Creates a default builder that can be customized later and then passed to one of
     * the constructors taking a builder instance. The constructors that don't take
     * builders themselves use this method internally to create their client builders.
     *
     * @param connectionPool A connection pool to use. If null, a new one will be generated
     * @return a new OkHttp client builder, intialized with default settings.
     */
    private static OkHttpClient.Builder getBuilder(ConnectionPool connectionPool) {
        OkHttpClient.Builder builder = ApnsClientBuilder.createDefaultOkHttpClientBuilder();
        if (connectionPool != null) {
            builder.connectionPool(connectionPool);
        }

        return builder;
    }

    @Override
    public boolean isSynchronous() {
        return true;
    }

    @Override
    public void push(Notification notification, NotificationResponseListener listener) {
        throw new UnsupportedOperationException("Asynchronous requests are not supported by this client");
    }

    protected final Request buildRequest(Notification notification) {
        final String topic = notification.getTopic() != null ? notification.getTopic() : defaultTopic;
        final String collapseId = notification.getCollapseId();
        final UUID uuid = notification.getUuid();
        final long expiration = notification.getExpiration();
        final Notification.Priority priority = notification.getPriority();
        final String pushType = notification.getPushType();
        Request.Builder rb = new Request.Builder()
                .url(gateway + "/3/device/" + notification.getToken())
                .post(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return mediaType;
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        sink.write(notification.getPayload().getBytes(Constants.UTF_8));
                    }
                })
                .header("content-length", notification.getPayload().getBytes(Constants.UTF_8).length + "");

        if (collapseId != null) {
            rb.header("apns-collapse-id", collapseId);
        }

        if (uuid != null) {
            rb.header("apns-id", uuid.toString());
        }

        if (expiration > -1) {
            rb.header("apns-expiration", String.valueOf(expiration));
        }

        if (priority != null) {
            rb.header("apns-priority", String.valueOf(priority.getCode()));
        }

        if (this.isVoip) {
            rb.header("apns-push-type", "voip");

            if (topic != null) {
                rb.header("apns-topic", topic + ".voip");
            }
        }
        else {
            if (topic != null) {
                rb.header("apns-topic", topic);
            }
          
            if (pushType != null) {
                rb.header("apns-push-type", pushType);
            }
        }

        if (keyID != null && teamID != null && apnsAuthKey != null) {

            // Generate a new JWT token if it's null, or older than 55 minutes
            if (cachedJWTToken == null || System.currentTimeMillis() - lastJWTTokenTS > 55 * 60 * 1000) {
                try {
                    lastJWTTokenTS = System.currentTimeMillis();
                    cachedJWTToken = JWT.getToken(teamID, keyID, apnsAuthKey);
                } catch (InvalidKeySpecException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
                    return null;
                }
            }

            rb.header("authorization", "bearer " + cachedJWTToken);
        }

        return rb.build();
    }


    @Override
    public NotificationResponse push(Notification notification) {
        final Request request = buildRequest(notification);
        Response response = null;

        try {
            response = client.newCall(request).execute();
            return parseResponse(response);
        } catch (Throwable t) {
            return new NotificationResponse(null, -1, null, t);
        } finally {
            if (response != null) {
                response.body().close();
            }
        }
    }

    @Override
    public OkHttpClient getHttpClient() {
        return client;
    }

    protected NotificationResponse parseResponse(Response response) throws IOException {
        String contentBody = null;
        int statusCode = response.code();

        NotificationRequestError error = null;

        if (response.code() != 200) {
            error = NotificationRequestError.get(statusCode);
            contentBody = response.body() != null ? response.body().string() : null;
        }

        return new NotificationResponse(error, statusCode, contentBody, null);
    }
}
