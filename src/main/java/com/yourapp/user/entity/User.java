package com.yourapp.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_phone", columnList = "phone_number", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", length = 20, unique = true, nullable = false)
    private String phoneNumber;

    @Column(length = 100)
    private String name;

    private Integer age;

    @Column(length = 10)
    private String sex; // MALE, FEMALE, OTHER

    private Double height; // in cm

    private Double weight; // in kg

    @Column(length = 50)
    private String goal; // FAT_LOSS, MAINTENANCE, MUSCLE_GAIN

    @Column(name = "diet_type", length = 100)
    private String dietType; // VEG, NON_VEG, VEGAN, EGGETARIAN, KETO, MULTIPLE

    @Column(length = 100)
    private String cuisine; // NORTH_INDIAN, SOUTH_INDIAN, NORTH_SOUTH_INDIAN, CONTINENTAL, ALL

    @Column(length = 20)
    private String budget; // BUDGET, MODERATE, PREMIUM

    @Column(name = "health_condition", length = 255)
    private String healthCondition; // Supports multiple e.g. DIABETES, HYPERTENSION, THYROID, PCOS

    @Column(name = "cooking_time")
    private Integer cookingTime; // in minutes

    @Column(name = "clinical_notes", length = 1000)
    private String clinicalNotes; // Long-term empathetic memory (e.g. cravings, digestive issues, knee pain)

    @Column(name = "last_mood", length = 50)
    private String lastMood; // Energetic, Tired, Bloated, Stressed

    @Builder.Default
    @Column(name = "streak_days")
    private Integer streakDays = 1; // Consecutive days active/logged

    @Builder.Default
    @Column(name = "yoga_program_day")
    private Integer yogaProgramDay = 1; // Healthyday 14-Day Free Yoga Program tracker (Day 1 to 14)

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
