package com.recircle.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recircle.entity.User;
import com.recircle.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RFC 7519 Compliant HMAC-SHA256 JWT Service.
 */
@Service
public class JwtService {

    private final String jwtSecret;
    private final long jwtExpirationMs;
    private final long refreshExpirationMs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtService(
        @Value("${recircle.security.jwt-secret}") String jwtSecret,
        @Value("${recircle.security.jwt-expiration-ms:86400000}") long jwtExpirationMs,
        @Value("${recircle.security.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(User user) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            long nowSec = Instant.now().getEpochSecond();
            long expSec = Instant.now().plusMillis(jwtExpirationMs).getEpochSecond();

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", String.valueOf(user.getId()));
            payload.put("email", user.getEmail());
            payload.put("name", user.getFullName());
            payload.put("role", user.getRole().name());
            payload.put("verified", user.isCampusVerified());
            payload.put("iat", nowSec);
            payload.put("exp", expSec);

            String headerB64 = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String payloadB64 = base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            String signingInput = headerB64 + "." + payloadB64;
            String signatureB64 = signHmacSha256(signingInput);

            return signingInput + "." + signatureB64;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT access token", e);
        }
    }

    public String generateRefreshTokenValue() {
        return "rc_rt_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public AuthPrincipal validateAndExtract(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String signingInput = parts[0] + "." + parts[1];
            String expectedSig = signHmacSha256(signingInput);
            if (!MessageDigest.isEqual(
                expectedSig.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8)
            )) {
                return null;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {});

            long exp = ((Number) claims.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                return null;
            }

            Long userId = Long.valueOf(String.valueOf(claims.get("sub")));
            String email = String.valueOf(claims.get("email"));
            UserRole role = UserRole.valueOf(String.valueOf(claims.get("role")));
            boolean verified = Boolean.TRUE.equals(claims.get("verified"));

            return new AuthPrincipal(userId, email, role, verified);
        } catch (Exception e) {
            return null;
        }
    }

    private String signHmacSha256(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(rawHmac);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
