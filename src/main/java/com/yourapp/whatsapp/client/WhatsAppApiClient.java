package com.yourapp.whatsapp.client;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * WhatsAppApiClient handles all outbound communication to Meta's WhatsApp Cloud API.
 * 
 * DESIGN HIGHLIGHT:
 * Built with Spring Boot 3's modern RestClient — synchronous, non-blocking with Java 21,
 * lightweight memory footprint, replacing legacy RestTemplate.
 */
@Component
@Slf4j
public class WhatsAppApiClient {

    private final RestClient restClient;
    private final String phoneNumberId;
    private final String accessToken;
    private final boolean isConfigured;

    public WhatsAppApiClient(
            @Value("${meta.api.url:https://graph.facebook.com/v19.0}") String apiUrl,
            @Value("${meta.phone.number.id:}") String phoneNumberId,
            @Value("${meta.access.token:}") String accessToken) {

        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
        this.isConfigured = (accessToken != null && !accessToken.isBlank() && phoneNumberId != null && !phoneNumberId.isBlank());

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        if (!this.isConfigured) {
            log.warn("WhatsAppApiClient is running in MOCK / LOCAL mode. Real messages will be logged instead of hitting Meta API.");
        }
    }

    /**
     * Send standard formatted Markdown text message
     */
    public boolean sendTextMessage(String toPhone, String text) {
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", toPhone,
                "type", "text",
                "text", Map.of("preview_url", false, "body", text)
        );

        return executeSend(payload, "TEXT to " + toPhone);
    }

    /**
     * Send High-Resolution Media Image (JPEG / PNG) with optional caption
     */
    public boolean sendImageMessage(String toPhone, String imageUrl, String captionText) {
        java.util.Map<String, Object> imageObj = new java.util.HashMap<>();
        imageObj.put("link", imageUrl);
        if (captionText != null && !captionText.isBlank()) {
            imageObj.put("caption", captionText);
        }

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", toPhone,
                "type", "image",
                "image", imageObj
        );

        return executeSend(payload, "IMAGE to " + toPhone);
    }

    /**
     * Send Quick-Reply Interactive Buttons (Up to 3 buttons)
     */
    public boolean sendButtonMessage(String toPhone, String bodyText, List<ButtonOption> buttons) {
        List<Map<String, Object>> buttonMaps = buttons.stream()
                .limit(3)
                .map(b -> Map.<String, Object>of(
                        "type", "reply",
                        "reply", Map.of("id", b.getId(), "title", b.getTitle())
                ))
                .toList();

        String clampedBody = bodyText != null ? bodyText : "";
        if (clampedBody.length() > 1024) {
            clampedBody = clampedBody.substring(0, 1020) + "...";
        }

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", toPhone,
                "type", "interactive",
                "interactive", Map.of(
                        "type", "button",
                        "body", Map.of("text", clampedBody),
                        "action", Map.of("buttons", buttonMaps)
                )
        );

        return executeSend(payload, "BUTTONS to " + toPhone);
    }

    /**
     * Send Interactive Dropdown List Menu (Up to 10 rows)
     */
    public boolean sendListMessage(String toPhone, String headerText, String bodyText, String buttonTitle, List<ListRowOption> rows) {
        List<Map<String, String>> rowMaps = rows.stream()
                .limit(10)
                .map(r -> {
                    String title = r.getTitle() != null ? r.getTitle() : "";
                    if (title.length() > 24) {
                        title = title.substring(0, 24);
                    }
                    String desc = r.getDescription() != null ? r.getDescription() : "";
                    if (desc.length() > 72) {
                        desc = desc.substring(0, 72);
                    }
                    return Map.of(
                            "id", r.getId(),
                            "title", title,
                            "description", desc
                    );
                })
                .toList();

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", toPhone,
                "type", "interactive",
                "interactive", Map.of(
                        "type", "list",
                        "header", Map.of("type", "text", "text", headerText),
                        "body", Map.of("text", bodyText),
                        "action", Map.of(
                                "button", buttonTitle,
                                "sections", List.of(Map.of("title", "Options", "rows", rowMaps))
                        )
                )
        );

        return executeSend(payload, "LIST to " + toPhone);
    }

    /**
     * Mark an incoming message as read (Blue checkmarks on WhatsApp)
     */
    public boolean markAsRead(String messageId) {
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "status", "read",
                "message_id", messageId
        );

        return executeSend(payload, "MARK_READ: " + messageId);
    }

    private boolean executeSend(Map<String, Object> payload, String logDescription) {
        if (!isConfigured) {
            log.info("[MOCK OUTBOUND WHATSAPP] {}: {}", logDescription, payload);
            return true;
        }

        try {
            String uri = "/" + phoneNumberId + "/messages";
            var response = restClient.post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[OUTBOUND SUCCESS] {}: Status {}", logDescription, response.getStatusCode());
            return true;
        } catch (Exception e) {
            log.error("[OUTBOUND ERROR] Failed to send {}: {}", logDescription, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Downloads media bytes from Meta WhatsApp Cloud API using the media ID.
     * Step 1: GET https://graph.facebook.com/v19.0/{mediaId} to get the direct download URL
     * Step 2: GET the media URL with Bearer auth to retrieve the raw byte array
     */
    public byte[] downloadMedia(String mediaId) {
        if (!isConfigured || mediaId == null || mediaId.isBlank()) {
            log.warn("Cannot download media {}: WhatsApp API client is unconfigured.", mediaId);
            return null;
        }

        try {
            // 1. Get media URL
            Map metaResponse = restClient.get()
                    .uri("/" + mediaId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (metaResponse == null || !metaResponse.containsKey("url")) {
                log.error("Failed to retrieve download URL for media ID: {}", mediaId);
                return null;
            }

            String mediaDownloadUrl = (String) metaResponse.get("url");
            log.info("Retrieved download URL for media {}: fetching binary stream...", mediaId);

            // 2. Download binary bytes with Bearer token
            return RestClient.create().get()
                    .uri(mediaDownloadUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(byte[].class);

        } catch (Exception e) {
            log.error("Exception while downloading media ID {}: {}", mediaId, e.getMessage(), e);
            return null;
        }
    }

    @Data
    @Builder
    public static class ButtonOption {
        private String id;
        private String title;
    }

    @Data
    @Builder
    public static class ListRowOption {
        private String id;
        private String title;
        private String description;
    }
}
