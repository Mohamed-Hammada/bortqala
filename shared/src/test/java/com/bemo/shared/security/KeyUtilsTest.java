package com.bemo.shared.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class KeyUtilsTest {

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        return generator.generateKeyPair();
    }

    @Test
    void roundTripsBase64Keys() throws Exception {
        KeyPair pair = rsaKeyPair();

        String publicBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String privateBase64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

        assertEquals(pair.getPublic(), KeyUtils.readPublicKey(publicBase64, "RSA"));
        assertEquals(pair.getPrivate(), KeyUtils.readPrivateKey(privateBase64, "RSA"));
    }

    @Test
    void roundTripsPemKeys() throws Exception {
        KeyPair pair = rsaKeyPair();

        String publicPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(pair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
        String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";

        assertNotNull(KeyUtils.readPublicKey(publicPem, "RSA"));
        assertNotNull(KeyUtils.readPrivateKey(privatePem, "RSA"));
    }

    @Test
    void hmacKeyDecodesBase64() {
        byte[] raw = new byte[]{1, 2, 3, 4};
        String encoded = Base64.getEncoder().encodeToString(raw);
        assertArrayEquals(raw, KeyUtils.hmacKey(encoded));
    }

    @Test
    void blankMaterialIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> KeyUtils.decode("   "));
    }
}
