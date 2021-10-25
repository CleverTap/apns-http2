package com.clevertap.apns.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Created by Jude Pereira, at 12:21 on 25/10/2021.
 */
class InvalidTrustManagerExceptionTest {

    @Test
    void constructor() {
        final InvalidTrustManagerException ex = new InvalidTrustManagerException("foo");
        assertEquals("foo", ex.getMessage());
    }
}