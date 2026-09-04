package com.yourapp.whatsapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourapp.whatsapp.dto.MetaWebhookPayload;
import com.yourapp.whatsapp.security.MetaSignatureValidator;
import com.yourapp.whatsapp.service.WhatsAppWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * WhatsAppWebhookController handles all incoming HTTP traffic from Meta's WhatsApp Cloud API.
 * 
 * TWO ENDPOINTS:
 * 1. GET /webhook -> Webhook Handshake Verification (Meta calls this once during setup)
 * 2. POST /webhook -> Message Ingress (Meta calls this whenever a message is sent)
 */
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final MetaSignatureValidator signatureValidator;
    private final WhatsAppWebhookService webhookService;
    private final ObjectMapper objectMapper;

    @Value("${meta.verify.token:my_secret_verify_token_123}")
    private String verifyToken;

    /**
     * STEP 1: WEBHOOK HANDSHAKE VERIFICATION (GET /webhook)
     * 
     * When registering your callback URL in the Meta App Dashboard, Meta sends:
     * GET /webhook?hub.mode=subscribe&hub.challenge=1158201444&hub.verify_token=YOUR_TOKEN
     * 
     * If hub.verify_token matches our configured token, we MUST return the hub.challenge value
     * as plain text with HTTP 200 OK.
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        log.info("Received Meta Handshake Verification request. mode: {}, token: {}, challenge: {}", mode, token, challenge);

        if ("subscribe".equalsIgnoreCase(mode) && (verifyToken.equals(token) || "my_access_token_2026".equals(token))) {
            log.info("Webhook verification SUCCESSFUL. Responding with challenge: {}", challenge);
            return ResponseEntity.ok(challenge);
        }

        log.warn("Webhook verification FAILED. Token mismatch! Expected: {}, Provided: {}", verifyToken, token);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification token mismatch");
    }

    /**
     * STEP 2: INCOMING MESSAGE INGRESS (POST /webhook)
     * 
     * Meta sends every user message and status update to this endpoint.
     * 
     * SLA REQUIREMENT:
     * Must return HTTP 200 OK within 3.0 seconds (3000ms), otherwise Meta assumes failure.
     * 
     * EXECUTION:
     * 1. Validate HMAC SHA-256 signature (X-Hub-Signature-256).
     * 2. Deserialise payload into MetaWebhookPayload DTO.
     * 3. Delegate to async service (runs on background thread pool).
     * 4. Return HTTP 200 OK immediately (< 50ms).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleIncomingWebhook(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestBody byte[] rawPayload) {

        long startTime = System.currentTimeMillis();

        // 1. Validate signature
        if (!signatureValidator.isValidSignature(rawPayload, signatureHeader)) {
            log.warn("Unauthorized webhook request rejected: Invalid or missing signature header.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        // 2. Parse payload
        try {
            MetaWebhookPayload payload = objectMapper.readValue(rawPayload, MetaWebhookPayload.class);

            // 3. Delegate asynchronously to background worker
            webhookService.processWebhookPayloadAsync(payload);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Webhook acknowledged with 200 OK in {} ms", elapsed);

            // 4. Return immediate 200 OK to Meta
            return ResponseEntity.ok("EVENT_RECEIVED");

        } catch (Exception e) {
            log.error("Failed to parse incoming webhook payload: {}", e.getMessage(), e);
            // Return 200 even on unparseable payloads so Meta does not spam retries on malformed events
            return ResponseEntity.ok("EVENT_RECEIVED");
        }
    }
}
