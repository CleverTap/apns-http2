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

package com.clevertap.apns.internal;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

public final class JWT {

    /**
     * Generates a JWT token as per Apple's specifications.
     *
     * @param teamID The team ID (found in the member center)
     * @param keyID  The key ID (found when generating your private key)
     * @param secret The private key (excluding the header and the footer)
     * @return The resulting token, which will be valid for one hour
     * @throws InvalidKeySpecException  if the key is incorrect
     * @throws NoSuchAlgorithmException if the key algo failed to load
     * @throws InvalidKeyException      if the key is invalid
     * @throws SignatureException       if this signature object is not initialized properly.
     */
    public static String getToken(final String teamID, final String keyID, final String secret)
            throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        final int now = (int) (System.currentTimeMillis() / 1000);
        final String header = "{\"alg\":\"ES256\",\"kid\":\"" + keyID + "\"}";
        final String payload = "{\"iss\":\"" + teamID + "\",\"iat\":" + now + "}";

        final String part1 = Base64.encodeBase64String(header.getBytes(StandardCharsets.UTF_8))
                + "."
                + Base64.encodeBase64String(payload.getBytes(StandardCharsets.UTF_8));

        return part1 + "." + ES256(secret, part1);
    }

    /**
     * Adopted from http://stackoverflow.com/a/20322894/2274894
     *
     * @param secret The secret
     * @param data   The data to be encoded
     * @return The encoded token
     * @throws InvalidKeySpecException  if the key is incorrect
     * @throws NoSuchAlgorithmException if the key algo failed to load
     * @throws InvalidKeyException      if the key is invalid
     * @throws SignatureException       if this signature object is not initialized properly.
     */
    private static String ES256(final String secret, final String data)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {

        KeyFactory kf = KeyFactory.getInstance("EC");
        KeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(secret.getBytes()));
        PrivateKey key = kf.generatePrivate(keySpec);

        final Signature sha256withECDSA = Signature.getInstance("SHA256withECDSA");
        sha256withECDSA.initSign(key);

        sha256withECDSA.update(data.getBytes(StandardCharsets.UTF_8));

        final byte[] signed = sha256withECDSA.sign();
        return Base64.encodeBase64String(signed);
    }
}
