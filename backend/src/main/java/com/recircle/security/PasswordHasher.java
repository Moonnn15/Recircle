package com.recircle.security;

import org.springframework.stereotype.Component;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cryptographic Password Hasher producing BCrypt-prefixed ($2a$10$) salted key derivations
 * with constant-time verification. Never stores or logs plain-text passwords.
 */
@Component
public class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    public String encode(String rawPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(salt);
        String derived = deriveKey(rawPassword, salt);
        return "$2a$10$" + saltB64 + "$" + derived;
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || !storedHash.startsWith("$2a$10$")) {
            return false;
        }
        String remainder = storedHash.substring(7);
        int sepIndex = remainder.indexOf('$');
        if (sepIndex <= 0) {
            return false;
        }
        String saltB64 = remainder.substring(0, sepIndex);
        String expectedDerived = remainder.substring(sepIndex + 1);
        byte[] salt = Base64.getUrlDecoder().decode(saltB64);
        String actualDerived = deriveKey(rawPassword, salt);
        return MessageDigest.isEqual(
            expectedDerived.getBytes(StandardCharsets.UTF_8),
            actualDerived.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String deriveKey(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Cryptographic key derivation failed", e);
        }
    }
}
