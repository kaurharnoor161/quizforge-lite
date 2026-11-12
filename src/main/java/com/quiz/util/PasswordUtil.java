package com.quiz.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Salted SHA-256 password hashing helpers. Not instantiable.
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;

    private PasswordUtil() {
        // utility class
    }

    /** Returns 16 random bytes, Base64 encoded (24 characters). */
    public static String generateSalt() {
        byte[] bytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** Returns SHA-256(salt + password) as a 64-character lowercase hex string. */
    public static String hashPassword(String plainPassword, String salt) {
        Objects.requireNonNull(plainPassword, "plainPassword must not be null");
        Objects.requireNonNull(salt, "salt must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + plainPassword).getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform is required to support SHA-256.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /** Constant-time comparison of the freshly computed hash against the stored one. */
    public static boolean verifyPassword(String plainPassword, String salt, String storedHash) {
        Objects.requireNonNull(plainPassword, "plainPassword must not be null");
        Objects.requireNonNull(salt, "salt must not be null");
        Objects.requireNonNull(storedHash, "storedHash must not be null");
        String computed = hashPassword(plainPassword, salt);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
