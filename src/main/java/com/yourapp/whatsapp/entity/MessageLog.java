package com.yourapp.whatsapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * MessageLog tracks all incoming and outgoing messages.
 * 
 * WHY THIS IS CRITICAL (IDEMPOTENCY):
 * Meta's webhook system guarantees "at-least-once" delivery. If a network hiccup
 * or momentary latency occurs, Meta will retry sending the exact same message.
 * By placing a UNIQUE constraint on message_id, we ensure that duplicate webhooks
 * are safely identified and discarded without sending double replies to the user.
 */
@Entity
@Table(name = "message_logs", indexes = {
    @Index(name = "idx_message_logs_msg_id", columnList = "message_id", unique = true),
    @Index(name = "idx_message_logs_phone", columnList = "phone_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "direction", length = 10, nullable = false)
    private String direction; // INBOUND, OUTBOUND

    @Column(name = "message_type", length = 20, nullable = false)
    private String messageType; // TEXT, INTERACTIVE, STATUS

    @Column(name = "message_id", length = 255, unique = true, nullable = false)
    private String messageId;

    @Column(name = "status", length = 20, nullable = false)
    private String status; // RECEIVED, PROCESSED, FAILED

    @Column(name = "payload_preview", length = 500)
    private String payloadPreview;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
