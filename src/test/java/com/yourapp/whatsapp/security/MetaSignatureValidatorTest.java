package com.yourapp.whatsapp.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaSignatureValidatorTest {

    private final MetaSignatureValidator validator = new MetaSignatureValidator();
    private final String secret = "super_secret_app_key_123";

    @Test
    @DisplayName("Should return true when HMAC-SHA256 signature is valid")
    void testValidSignature() throws Exception {
        ReflectionTestUtils.setField(validator, "appSecret", secret);

        String payload = "{\"test\":\"data\"}";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        // Compute valid signature
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hexHmac = HexFormat.of().formatHex(mac.doFinal(payloadBytes));
        String signatureHeader = "sha256=" + hexHmac;

        assertTrue(validator.isValidSignature(payloadBytes, signatureHeader));
    }

    @Test
    @DisplayName("Should return false when signature does not match (Tampered payload)")
    void testTamperedPayload() {
        ReflectionTestUtils.setField(validator, "appSecret", secret);

        byte[] payloadBytes = "{\"test\":\"tampered\"}".getBytes(StandardCharsets.UTF_8);
        String fakeSignatureHeader = "sha256=112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";

        assertFalse(validator.isValidSignature(payloadBytes, fakeSignatureHeader));
    }

    @Test
    @DisplayName("Should return false when signature header prefix is missing or malformed")
    void testMalformedHeader() {
        ReflectionTestUtils.setField(validator, "appSecret", secret);

        byte[] payloadBytes = "{\"test\":\"data\"}".getBytes(StandardCharsets.UTF_8);
        assertFalse(validator.isValidSignature(payloadBytes, "invalid_header_without_sha256_prefix"));
        assertFalse(validator.isValidSignature(payloadBytes, null));
    }
}
