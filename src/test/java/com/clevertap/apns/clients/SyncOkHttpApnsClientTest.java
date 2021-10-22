package com.clevertap.apns.clients;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import com.clevertap.apns.ApnsClient;
import com.clevertap.apns.LocalHttpServer;
import com.clevertap.apns.Notification;
import com.clevertap.apns.NotificationResponse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;


public class SyncOkHttpApnsClientTest {

    protected static final String DEFAULT_TOPIC = "com.clevertap.testTopic";
    protected static final String CERT_PASSWD = "cert-password";
    protected static final String DEVICE_TOKEN = "vaild-device-token";
    protected static final String INVALID_DEVICE_TOKEN = "invaild-device-token";

    protected static HeldCertificate rootCertificate;
    protected static HeldCertificate serverCertificate;
    protected static HeldCertificate clientCertificate;
    protected static HandshakeCertificates serverCertificateChain;
    protected static HandshakeCertificates clientCertificateChain;

    @BeforeAll
    public static void initCertificates() {
        rootCertificate = new HeldCertificate.Builder()
                .certificateAuthority(0)
                .build();

        serverCertificate = new HeldCertificate.Builder()
                .addSubjectAlternativeName("localhost")
                .commonName("localhost")
                .signedBy(rootCertificate)
                .build();

        clientCertificate = new HeldCertificate.Builder()
                .commonName("push")
                .signedBy(rootCertificate)
                .build();

        serverCertificateChain = new HandshakeCertificates.Builder()
                .heldCertificate(serverCertificate)
                .addTrustedCertificate(rootCertificate.certificate())
                .build();

        // Don't add client cert to client cert chain b/c it will be added via the apns api
        clientCertificateChain = new HandshakeCertificates.Builder()
                .addTrustedCertificate(rootCertificate.certificate())
                .build();
    }

    /**
     * Convert client cert to PKCS12 Format and return as InputStream.
     * @return
     */
    protected InputStream getClientCertPKCS12() {
        try {
            KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
            pkcs12.load(null, null);
            Certificate chain[] = {clientCertificate.certificate()};
            pkcs12.setKeyEntry("privatekeyalias", clientCertificate.keyPair().getPrivate(), CERT_PASSWD.toCharArray(), chain);

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            pkcs12.store(outStream, CERT_PASSWD.toCharArray());

            return new ByteArrayInputStream(outStream.toByteArray());
        } catch(Exception e) {
            fail(e.getMessage());
        }
        return null;
    }

    /**
     * Changes Gateway-URL of the ApnsClient instance to the given URL via reflection.
     *
     * @param client  ApnsClient instance which gatewayUrl shall be changed
     * @param gatewayUrl  URL to set
     */
    protected void setClientGatewayUrl(ApnsClient client, HttpUrl gatewayUrl) {
        try {
            String url = gatewayUrl.toString();

            // strip trailling slash
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            Field field = client.getClass().getDeclaredField("gateway");
            field.setAccessible(true);

            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(client, url);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }



    /**
     * Build ApnsClient with valid client cert in synchronous mode.
     * @return apnsClient
     */
    private ApnsClient buildClientWithCert(boolean withOkHttpClientBuilder) {
        try {
            ApnsClientBuilder builder = new ApnsClientBuilder()
                    .withDefaultTopic(DEFAULT_TOPIC)
                    .withCertificate(getClientCertPKCS12())
                    .withPassword(CERT_PASSWD)
                    .inSynchronousMode()
                    .withProductionGateway();

            if (withOkHttpClientBuilder) {
                builder.withOkHttpClientBuilder(new OkHttpClient.Builder().sslSocketFactory(clientCertificateChain.sslSocketFactory(), clientCertificateChain.trustManager()));
            }

            return builder.build();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return null;
    }

    @Test
    void pushTestWithCert() {
        MockWebServer server = new MockWebServer();
        try {
            server.useHttps(serverCertificateChain.sslSocketFactory(), false);
            server.requestClientAuth();
            server.enqueue(new MockResponse().setResponseCode(200).setBody("Hello world!"));

            ApnsClient client = buildClientWithCert(true);
            setClientGatewayUrl(client, server.url(""));

            NotificationResponse response = client.push(
                    new Notification.Builder(DEVICE_TOKEN)
                            .alertBody("Notification Body")
                            .alertTitle("Alert Title")
                            .badge(10)
                            .sound("sound")
                            .build()
            );
            assertEquals("HTTP-Response-Code 200", 200, response.getHttpStatusCode());

            RecordedRequest request = server.takeRequest();
            assertEquals("/3/device/" + DEVICE_TOKEN, request.getPath());
            assertEquals(DEFAULT_TOPIC, request.getHeader("apns-topic"));

            X509Certificate clientCert = (X509Certificate) request.getHandshake().peerCertificates().get(0);
            X509Certificate clientChain[] = {clientCert};
            serverCertificateChain.trustManager().checkClientTrusted(clientChain, "RSA");

        } catch (Exception e) {
            fail(e.getMessage());
        }

        try {
            server.close();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void pushTestWithCertificateWithLocalHttpServer() throws Exception {
        LocalHttpServer localHttpServer = new LocalHttpServer();
        localHttpServer.init();
        HttpUrl url = HttpUrl.parse(localHttpServer.getUrl());
        ApnsClient client = buildClientWithCert(true);
        setClientGatewayUrl(client, url);

        NotificationResponse response = client.push(
                new Notification.Builder(DEVICE_TOKEN)
                        .alertBody("Notification Body")
                        .alertTitle("Alert Title")
                        .badge(10)
                        .sound("sound")
                        .build()
        );
        assertEquals("Server should be hit and should return 200", 200, response.getHttpStatusCode());

        // Should have the same result as above if the trust manager isn't passed as well
        client = buildClientWithCert(false);
        setClientGatewayUrl(client, url);
        response = client.push(
                new Notification.Builder(DEVICE_TOKEN)
                        .alertBody("Notification Body")
                        .alertTitle("Alert Title")
                        .badge(10)
                        .sound("sound")
                        .build()
        );
        assertEquals("Server should be hit and should return 200 without trust manager set", 200, response.getHttpStatusCode());

        localHttpServer.shutDownServer();
    }
}