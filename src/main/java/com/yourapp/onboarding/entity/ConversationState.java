package com.yourapp.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ConversationState maintains the state machine for each user.
 * 
 * WHY THIS IS CRITICAL:
 * Because WhatsApp is a stateless messaging protocol, the server needs to know:
 * 1. What step of onboarding is this user currently on?
 * 2. When the user taps a button or types a number, what does that answer belong to?
 */
@Entity
@Table(name = "conversation_state", indexes = {
    @Index(name = "idx_conv_phone", columnList = "phone_number", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationState {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "phone_number", length = 20, unique = true, nullable = false)
    private String phoneNumber;

    @Column(length = 50, nullable = false)
    private String state; // NEW_USER, ONBOARDING, ACTIVE, PAUSED

    @Column(name = "current_step", length = 50, nullable = false)
    private String currentStep; // AWAITING_GOAL, AWAITING_AGE_SEX, AWAITING_HEIGHT_WEIGHT, AWAITING_DIET, AWAITING_CUISINE, AWAITING_CONDITIONS, COMPLETE

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
