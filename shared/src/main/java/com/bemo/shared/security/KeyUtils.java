package com.bemo.shared.security;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Key loading helpers for PEM/base64 RSA, EC and HMAC material. No external crypto libraries
 * required. Accepts either a raw Base64 string or a full PEM block.
 */
public final class KeyUtils {

    private static final String PUBLIC_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String RSA_PUBLIC_HEADER = "-----BEGIN RSA PUBLIC KEY-----";
    private static final String EC_PUBLIC_HEADER = "-----BEGIN EC PUBLIC KEY-----";

    private KeyUtils() {
    }

    public static PublicKey readPublicKey(String keyMaterial, String algorithm) {
        try {
            byte[] der = decode(keyMaterial);
            return KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(der));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid " + algorithm + " public key", e);
        }
    }

    public static PrivateKey readPrivateKey(String keyMaterial, String algorithm) {
        try {
            byte[] der = decode(keyMaterial);
            return KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid " + algorithm + " private key", e);
        }
    }

    public static byte[] hmacKey(String base64OrRaw) {
        return decode(base64OrRaw);
    }

    /** Strips PEM framing (if present) and Base64-decodes to DER bytes. */
    public static byte[] decode(String keyMaterial) {
        if (keyMaterial == null || keyMaterial.isBlank()) {
            throw new IllegalArgumentException("Key material must not be blank");
        }
        String cleaned = keyMaterial
                .replace(PUBLIC_HEADER, "")
                .replace(PRIVATE_HEADER, "")
                .replace(RSA_PUBLIC_HEADER, "")
                .replace(EC_PUBLIC_HEADER, "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replace("-----END EC PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }
}
