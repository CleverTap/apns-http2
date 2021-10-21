package com.clevertap.apns;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Created by Jude Pereira, at 11:51 on 21/10/2021.
 */
class NotificationResponseTest {

    @Test
    void simple() {
        final Exception ex = new Exception();
        final NotificationResponse nr = new NotificationResponse(
                NotificationRequestError.InternalServerError, 200, "my_response", ex);
        assertEquals(NotificationRequestError.InternalServerError, nr.getError());
        assertEquals(200, nr.getHttpStatusCode());
        assertEquals("my_response", nr.getResponseBody());
        assertSame(ex, nr.getCause());
        assertEquals(
                "NotificationResponse{error=InternalServerError, httpStatusCode=200, responseBody='my_response', cause=java.lang.Exception}",
                nr.toString());
    }
}