package com.bemo.shared.util;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Random string generators for references, codes and one-time tokens.
 */
public final class RandomString {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String DIGITS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomString() {
    }

    public static String alphanumeric(int length) {
        return generate(ALPHANUMERIC, length);
    }

    public static String numeric(int length) {
        return generate(DIGITS, length);
    }

    /** Cryptographically random hex string with {@code byteCount} bytes (2 hex chars each). */
    public static String hex(int byteCount) {
        byte[] bytes = new byte[byteCount];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String generate(String alphabet, int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length must be >= 1");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
