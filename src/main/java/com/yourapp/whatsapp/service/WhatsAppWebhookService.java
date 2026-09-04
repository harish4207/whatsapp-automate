package com.yourapp.whatsapp.service;

import com.yourapp.whatsapp.dto.MetaWebhookPayload;
import com.yourapp.whatsapp.entity.MessageLog;
import com.yourapp.whatsapp.repository.MessageLogRepository;
import com.yourapp.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * WhatsAppWebhookService handles asynchronous processing of incoming WhatsApp events.
 * 
 * WHY THIS SERVICE IS @Async:
 * The HTTP controller returns HTTP 200 OK to Meta immediately.
 * This method runs concurrently in the background thread pool ("webhookExecutor").
 * Even if database calls, external API calls, or meal calculations take 5 seconds,
 * Meta never sees a timeout!
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookService {

    private final MessageLogRepository messageLogRepository;
    private final OnboardingService onboardingService;

    @Async("webhookExecutor")
    @Transactional
    public void processWebhookPayloadAsync(MetaWebhookPayload payload) {
        log.info("Starting asynchronous processing of WhatsApp payload...");

        // 1. Check if this is a user message
        Optional<MetaWebhookPayload.Message> optionalMessage = payload.getFirstMessage();
        if (optionalMessage.isEmpty()) {
            handleNonMessageEvent(payload);
            return;
        }

        MetaWebhookPayload.Message message = optionalMessage.get();
        String messageId = message.getId();
        String senderPhone = message.getFrom();
        String messageType = message.getType();

        // 2. IDEMPOTENCY CHECK
        // If Meta already sent this messageId (due to network retry), discard it!
        if (messageLogRepository.existsByMessageId(messageId)) {
            log.warn("[IDEMPOTENCY] Duplicate message_id detected: {}. Discarding to prevent duplicate reply.", messageId);
            return;
        }

        // Extract message preview/content
        String contentPreview = extractContentPreview(message);

        // 3. Log the inbound message
        MessageLog messageLog = MessageLog.builder()
                .messageId(messageId)
                .phoneNumber(senderPhone)
                .direction("INBOUND")
                .messageType(messageType != null ? messageType.toUpperCase() : "UNKNOWN")
                .status("RECEIVED")
                .payloadPreview(contentPreview)
                .build();
        messageLogRepository.save(messageLog);

        log.info("[INBOUND_MESSAGE] From: {}, Type: {}, Content: '{}', MsgId: {}", 
                senderPhone, messageType, contentPreview, messageId);

        try {
            String senderName = payload.getSenderProfileName().orElse(null);
            onboardingService.handleIncomingMessage(senderPhone, message, senderName);

            messageLog.setStatus("PROCESSED");
            messageLogRepository.save(messageLog);
            log.info("[SUCCESS] Message {} processed successfully.", messageId);

        } catch (Exception e) {
            log.error("[ERROR] Failed to process message {}: {}", messageId, e.getMessage(), e);
            messageLog.setStatus("FAILED");
            messageLogRepository.save(messageLog);
        }
    }

    private void handleNonMessageEvent(MetaWebhookPayload payload) {
        // Meta also sends delivery receipts ("sent", "delivered", "read") through this webhook
        if (payload.getEntry() != null && !payload.getEntry().isEmpty()) {
            var changes = payload.getEntry().get(0).getChanges();
            if (changes != null && !changes.isEmpty()) {
                var value = changes.get(0).getValue();
                if (value != null && value.getStatuses() != null && !value.getStatuses().isEmpty()) {
                    var status = value.getStatuses().get(0);
                    log.info("[STATUS_UPDATE] MessageId: {}, Status: {}, Recipient: {}", 
                            status.getId(), status.getStatus(), status.getRecipientId());
                }
            }
        }
    }

    private String extractContentPreview(MetaWebhookPayload.Message message) {
        if ("text".equalsIgnoreCase(message.getType()) && message.getText() != null) {
            return message.getText().getBody();
        } else if ("interactive".equalsIgnoreCase(message.getType()) && message.getInteractive() != null) {
            var interactive = message.getInteractive();
            if (interactive.getButtonReply() != null) {
                return "[Button: " + interactive.getButtonReply().getId() + " - " + interactive.getButtonReply().getTitle() + "]";
            } else if (interactive.getListReply() != null) {
                return "[List: " + interactive.getListReply().getId() + " - " + interactive.getListReply().getTitle() + "]";
            }
        } else if ("image".equalsIgnoreCase(message.getType())) {
            return "[Image: ID=" + (message.getImage() != null ? message.getImage().getId() : "null") + "]";
        } else if ("audio".equalsIgnoreCase(message.getType())) {
            return "[Audio: ID=" + (message.getAudio() != null ? message.getAudio().getId() : "null") + "]";
        }
        return "Non-text content";
    }
}
