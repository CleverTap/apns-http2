package com.judepereira.jetty.apns.http2;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.FileInputStream;
import java.security.KeyStore;

public class Main {

    public static void main(String[] args) throws Exception {
        HTTP2Client http2Client = new HTTP2Client();
        http2Client.start();

        KeyStore ks = KeyStore.getInstance("PKCS12");

        // Ensure that the password is the same as the one used later in setKeyStorePassword()
        ks.load(new FileInputStream("MyProductionOrDevelopmentCertificate.p12"), "".toCharArray());

        SslContextFactory ssl = new SslContextFactory(true);
        ssl.setKeyStore(ks);
        ssl.setKeyStorePassword("");

        HttpClient client = new HttpClient(new HttpClientTransportOverHTTP2(http2Client), ssl);
        client.start();

        // Change the API endpoint to api.development.push.apple.com if you're using a development certificate
        Request req = client.POST("https://api.push.apple.com")
                // Update your :path "/3/device/<your token>"
                .path("/3/device/b2482deaf55521b2ccd755d5817a39784cc0044e24s3523a4708c2fa08983bdf")
                .content(new StringContentProvider("{ \"aps\" : { \"alert\" : \"Hello\" } }"));

        ContentResponse response = req.send();
        System.out.println("response code: " + response.getStatus());

        // The response body is empty for successful requests
        System.out.println("response body: " + response.getContentAsString());
    }
}
