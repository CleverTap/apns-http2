package com.clevertap.jetty.apn.http2;
import java.io.FileInputStream;

import com.clevertap.jetty.apns.http2.ApnsClient;
import com.clevertap.jetty.apns.http2.Notification;
import com.clevertap.jetty.apns.http2.NotificationRequestError;
import com.clevertap.jetty.apns.http2.NotificationResponseListener;
import com.clevertap.jetty.apns.http2.clients.ApnsClientBuilder;

public class Test {

	public static void main(String[] args) throws Exception {
		
		FileInputStream cert = new FileInputStream("/Users/fcioffi/coding/345mxm_java-be/_clients/100-context/certs/idpush_01tribe_enterprise_it_context_demo1_all.p12");
		final ApnsClient client = new ApnsClientBuilder()
		                .withProductionGateway()
		                .inAsynchronousMode()
		                .withCertificate(cert)
		                .withPassword("01tribe")
		                .build();
		
		client.start();
		
		Notification.Builder nb = new Notification.Builder("<827d6b5d 0fc849b7 a7f19718 6403ab35 b616bc09 648b3193 a00c3c24 2a33c6b7>".replace("<", "").replace(">", "").replace(" ", ""));
		nb = nb.alertBody("Hello");
				
		Notification n = nb.build();
		
		for (int i = 0; i < 1000; i++) {
			
			client.push("it.context.demo1", n, new NotificationResponseListener() {
			    @Override
			    public void onSuccess(Notification notification) {
			    }
	
			    @Override
			    public void onFailure(Notification notification, NotificationRequestError err, String responseContent) {
			        System.out.println("failure: " + err.name());
			        System.out.println("content: " + responseContent);
			    }
			});
		}
	}
}
