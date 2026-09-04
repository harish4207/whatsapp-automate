package com.yourapp.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * MetaWebhookPayload maps the nested JSON payload delivered by Meta's WhatsApp Cloud API.
 * 
 * STRUCTURE BREAKDOWN:
 * entry[] -> changes[] -> value -> { messages[], contacts[], statuses[], metadata }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetaWebhookPayload {

    private String object;
    private List<Entry> entry;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        private String id;
        private List<Change> changes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        private String field;
        private Value value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value {
        @JsonProperty("messaging_product")
        private String messagingProduct;
        private Metadata metadata;
        private List<Contact> contacts;
        private List<Message> messages;
        private List<Status> statuses;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        @JsonProperty("display_phone_number")
        private String displayPhoneNumber;
        @JsonProperty("phone_number_id")
        private String phoneNumberId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {
        private Profile profile;
        @JsonProperty("wa_id")
        private String waId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String from;
        private String id;
        private String timestamp;
        private String type; // text, interactive, image, audio, etc.
        private Text text;
        private Interactive interactive;
        private Media image;
        private Media audio;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Media {
        private String id;
        private String mime_type;
        private String sha256;
        private Long file_size;
        private String caption;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Text {
        private String body;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Interactive {
        private String type; // button_reply, list_reply
        @JsonProperty("button_reply")
        private ButtonReply buttonReply;
        @JsonProperty("list_reply")
        private ListReply listReply;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ButtonReply {
        private String id;
        private String title;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListReply {
        private String id;
        private String title;
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        private String id;
        private String status; // sent, delivered, read, failed
        private String timestamp;
        @JsonProperty("recipient_id")
        private String recipientId;
    }

    // ==========================================
    // HELPER CONVENIENCE METHODS
    // ==========================================

    /**
     * Extracts the first incoming user message, if present.
     */
    public Optional<Message> getFirstMessage() {
        if (entry == null || entry.isEmpty()) return Optional.empty();
        Entry firstEntry = entry.get(0);
        if (firstEntry.getChanges() == null || firstEntry.getChanges().isEmpty()) return Optional.empty();
        Value value = firstEntry.getChanges().get(0).getValue();
        if (value == null || value.getMessages() == null || value.getMessages().isEmpty()) return Optional.empty();
        return Optional.of(value.getMessages().get(0));
    }

    /**
     * Extracts the sender's profile name from contacts, if available.
     */
    public Optional<String> getSenderProfileName() {
        if (entry == null || entry.isEmpty()) return Optional.empty();
        Entry firstEntry = entry.get(0);
        if (firstEntry.getChanges() == null || firstEntry.getChanges().isEmpty()) return Optional.empty();
        Value value = firstEntry.getChanges().get(0).getValue();
        if (value != null && value.getContacts() != null && !value.getContacts().isEmpty()) {
            Contact contact = value.getContacts().get(0);
            if (contact.getProfile() != null) {
                return Optional.ofNullable(contact.getProfile().getName());
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts the phone_number_id (useful for routing the reply).
     */
    public Optional<String> getPhoneNumberId() {
        if (entry == null || entry.isEmpty()) return Optional.empty();
        Entry firstEntry = entry.get(0);
        if (firstEntry.getChanges() == null || firstEntry.getChanges().isEmpty()) return Optional.empty();
        Value value = firstEntry.getChanges().get(0).getValue();
        if (value != null && value.getMetadata() != null) {
            return Optional.ofNullable(value.getMetadata().getPhoneNumberId());
        }
        return Optional.empty();
    }
}
