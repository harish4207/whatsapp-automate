package com.yourapp.nutrition.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "log_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Builder.Default
    @Column(name = "water_intake_ml", nullable = false)
    private Integer waterIntakeMl = 0;

    @Builder.Default
    @Column(name = "logged_calories", nullable = false)
    private Integer loggedCalories = 0;

    @Builder.Default
    @Column(name = "logged_protein", nullable = false)
    private Double loggedProtein = 0.0;

    @Builder.Default
    @Column(name = "meals_completed", nullable = false)
    private Integer mealsCompleted = 0;

    @Column(name = "notes", length = 2000)
    private String notes;
}
