package com.adam.warehouse.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public final class PasswordHasher {

    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);

        byte[] derivedKey = deriveKey(password.toCharArray(), salt, ITERATIONS);
        return "%s$%d$%s$%s".formatted(
                PREFIX,
                ITERATIONS,
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(derivedKey)
        );
    }

    public static boolean matches(String password, String storedValue) {
        if (password == null || storedValue == null) {
            return false;
        }

        if (!isHashed(storedValue)) {
            return MessageDigest.isEqual(
                    password.getBytes(StandardCharsets.UTF_8),
                    storedValue.getBytes(StandardCharsets.UTF_8)
            );
        }

        String[] parts = storedValue.split("\\$", -1);
        if (parts.length != 4) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedKey = Base64.getDecoder().decode(parts[3]);
            byte[] actualKey = deriveKey(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(actualKey, expectedKey);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static boolean isHashed(String storedValue) {
        return storedValue != null && storedValue.startsWith(PREFIX + "$");
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterations) {
        PBEKeySpec specification = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);

        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification)
                    .getEncoded();
        } catch (InvalidKeySpecException exception) {
            throw new IllegalStateException("Unable to secure the password.", exception);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Password hashing is not available on this system.", exception);
        } finally {
            specification.clearPassword();
        }
    }
}
