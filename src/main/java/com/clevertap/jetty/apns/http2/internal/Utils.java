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

import com.clevertap.jetty.apns.http2.CertificateUtils;
import com.clevertap.jetty.apns.http2.Notification;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * Internal utilities.
 */
public class Utils {

    public static HttpClient buildClient(InputStream certificate, String password) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        password = password == null ? "" : password;
        SslContextFactory sslContext = new SslContextFactory(false);
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(certificate, password.toCharArray());

        // Test our certificate
        final X509Certificate cert = (X509Certificate) ks.getCertificate(ks.aliases().nextElement());
        CertificateUtils.validateCertificate(cert);

        sslContext.setKeyStore(ks);
        sslContext.setKeyStorePassword(password);
        return new HttpClient(new HttpClientTransportOverHTTP2(new HTTP2Client()), sslContext);
    }

    public static Request buildRequest(HttpClient client, String topic, Notification notification, String gateway) {

        Request request = client.POST(gateway)
                .timeout(2, TimeUnit.MINUTES)
                .path("/3/device/" + notification.getToken())
                .header("content-length", notification.getPayload().getBytes(Constants.UTF_8).length + "")
                .content(new StringContentProvider(notification.getPayload(), Constants.UTF_8));

        if (topic != null) request.header("apns-topic", topic);

        return request;
    }
}
