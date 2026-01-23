package com.htai.exe201phapluatso.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating and validating short-lived signed tokens for payment status polling.
 * Uses HMAC-SHA256 with Base64URL encoding.
 */
public class PaymentStatusTokenService {
    private static final Logger log = LoggerFactory.getLogger(PaymentStatusTokenService.class);

    private static final String ALGORITHM = "HmacSHA256";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String secret;
    private final long ttlSeconds;

    public PaymentStatusTokenService(String secret, long ttlMinutes) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("Token secret cannot be null or empty");
        }
        this.secret = secret;
        this.ttlSeconds = ttlMinutes * 60; // Convert to seconds
        log.debug("PaymentStatusTokenService initialized with TTL: {} minutes", ttlMinutes);
    }

    /**
     * Generate a signed token for the given orderCode.
     * Format: header.payload.signature (Base64URL encoded)
     */
    public String generate(long orderCode) {
        try {
            Instant now = Instant.now();
            long exp = now.getEpochSecond() + ttlSeconds;

            // Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderCode", orderCode);
            payload.put("exp", exp);
            payload.put("nonce", generateNonce()); // Add entropy, not for replay prevention

            String payloadJson = OBJECT_MAPPER.writeValueAsString(payload);
            String payloadB64 = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Header (minimal)
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            String headerJson = OBJECT_MAPPER.writeValueAsString(header);
            String headerB64 = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));

            // Signature
            String message = headerB64 + "." + payloadB64;
            String signature = hmacSha256(message, secret);
            String signatureB64 = base64UrlEncode(signature.getBytes(StandardCharsets.UTF_8));

            String token = headerB64 + "." + payloadB64 + "." + signatureB64;

            log.debug("Generated payment status token for orderCode: {}", orderCode);
            return token;

        } catch (Exception e) {
            log.error("Failed to generate payment status token for orderCode: {}", orderCode, e);
            throw new RuntimeException("Failed to generate payment status token", e);
        }
    }

    /**
     * Validate token and extract orderCode.
     * Returns orderCode if valid, throws exception if invalid.
     */
    public long validateAndExtractOrderCode(String token, long expectedOrderCode) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("Token is required");
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token format");
            }

            String headerB64 = parts[0];
            String payloadB64 = parts[1];
            String signatureB64 = parts[2];

            // Verify signature
            String message = headerB64 + "." + payloadB64;
            String expectedSignature = hmacSha256(message, secret);
            String providedSignature = new String(base64UrlDecode(signatureB64), StandardCharsets.UTF_8);

            if (!constantTimeEquals(expectedSignature.getBytes(StandardCharsets.UTF_8),
                                   providedSignature.getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("Invalid token signature");
            }

            // Parse payload
            String payloadJson = new String(base64UrlDecode(payloadB64), StandardCharsets.UTF_8);
            Map<String, Object> payload = OBJECT_MAPPER.readValue(payloadJson, Map.class);

            // Check expiration
            Number expObj = (Number) payload.get("exp");
            if (expObj == null) {
                throw new IllegalArgumentException("Token missing expiration");
            }
            long exp = expObj.longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new IllegalArgumentException("Token expired");
            }

            // Check orderCode
            Number orderCodeObj = (Number) payload.get("orderCode");
            if (orderCodeObj == null) {
                throw new IllegalArgumentException("Token missing orderCode");
            }
            long tokenOrderCode = orderCodeObj.longValue();
            if (tokenOrderCode != expectedOrderCode) {
                throw new IllegalArgumentException("Token orderCode mismatch");
            }

            return tokenOrderCode;

        } catch (Exception e) {
            // Don't log the token value for security
            log.debug("Token validation failed for orderCode {}: {}", expectedOrderCode, e.getMessage());
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    private String hmacSha256(String message, String secret) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        mac.init(secretKey);
        byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] base64UrlDecode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    /**
     * Generate a random nonce for entropy (not for replay prevention).
     */
    private String generateNonce() {
        return Long.toString(Instant.now().toEpochMilli()) + "_" +
               Long.toString((long) (Math.random() * Long.MAX_VALUE));
    }

    /**
     * Constant-time byte array comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}

