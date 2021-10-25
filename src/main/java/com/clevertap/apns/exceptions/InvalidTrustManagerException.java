package com.clevertap.apns.exceptions;

import java.security.cert.CertificateException;

public class InvalidTrustManagerException extends CertificateException {
    public InvalidTrustManagerException(String s) {
        super(s);
    }
}
