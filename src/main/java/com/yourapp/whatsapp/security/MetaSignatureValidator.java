package com.yourapp.whatsapp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * MetaSignatureValidator validates the integrity and authenticity of inbound WhatsApp webhooks.
 * 
 * WHY THIS IS CRITICAL (SECURITY):
 * Meta signs every webhook POST request using HMAC-SHA256 with your META_APP_SECRET.
 * The signature is sent in the "X-Hub-Signature-256" header in the format:
 * "sha256=<hex_encoded_hmac>"
 * 
 * We must verify this signature before trusting the payload to prevent attackers
 * from forging webhook events or spoofing phone numbers.
 */
@Component
@Slf4j
public class MetaSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    @Value("${meta.app.secret:}")
    private String appSecret;

    /**
     * Validates that the raw payload matches the signature in the X-Hub-Signature-256 header.
     * 
     * @param payloadRaw The raw byte array of the HTTP request body
     * @param signatureHeader The value of the X-Hub-Signature-256 header
     * @return true if valid, false if invalid or missing
     */
    public boolean isValidSignature(byte[] payloadRaw, String signatureHeader) {
        if (appSecret == null || appSecret.isBlank()) {
            log.warn("META_APP_SECRET is not configured! Skipping signature check in local dev/testing mode.");
            return true;
        }

        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            log.error("Missing or invalid X-Hub-Signature-256 header prefix. Header: {}", signatureHeader);
            return false;
        }

        String actualSignature = signatureHeader.substring(SIGNATURE_PREFIX.length());

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(payloadRaw);
            String expectedSignature = HexFormat.of().formatHex(hmacBytes);

            // MessageDigest.isEqual performs a constant-time comparison to protect against timing attacks
            boolean matches = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    actualSignature.getBytes(StandardCharsets.UTF_8)
            );

            if (!matches) {
                log.warn("Signature mismatch! Expected: {}, Received: {}", expectedSignature, actualSignature);
            }
            return matches;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC-SHA256 signature", e);
            return false;
        }
    }
}
