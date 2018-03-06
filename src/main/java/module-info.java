/**
 * User: Jude Pereira
 * Date: 04/01/2018
 * Time: 23:37
 */
module com.clevertap.apns {
	exports com.clevertap.apns;
	exports com.clevertap.apns.clients;
	
    requires jdk.incubator.httpclient;
    requires jackson.core;
    requires jackson.databind;
}