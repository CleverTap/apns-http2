package com.clevertap.apns.integration;

import com.clevertap.apns.ApnsClient;
import com.clevertap.apns.Notification;
import com.clevertap.apns.NotificationResponse;
import com.clevertap.apns.NotificationResponseListener;
import com.clevertap.apns.clients.ApnsClientBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class NotificationTest {
    private static String APN_AUTH_KEY;
    private static String TOKEN;
    private static String TEAM_ID;
    private static String KEY_ID;

    ApnsClient asyncClient;
    @BeforeClass
    public static void init(){
        APN_AUTH_KEY = System.getProperty("com.clevertap.auth_key");
        TOKEN = System.getProperty("com.clevertap.token");
        TEAM_ID = System.getProperty("com.clevertap.team_id");
        KEY_ID = System.getProperty("com.clevertap.key_id");
    }
    @Before
    public void setUp() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        asyncClient = new ApnsClientBuilder().
                inAsynchronousMode().
                withApnsAuthKey(APN_AUTH_KEY).
                withTeamID(TEAM_ID).
                withKeyID(KEY_ID).
                build();
    }
    @Test
    public void sendNotification(){
        Notification notification = new Notification.Builder(TOKEN).alertBody("Hello").build();

        asyncClient.push(notification, new NotificationResponseListener() {
            @Override
            public void onSuccess(Notification notification) {

            }

            @Override
            public void onFailure(Notification notification, NotificationResponse response) {
                Assert.fail("Failed to send notification: "+response.getResponseBody());
            }
        });
    }

}
