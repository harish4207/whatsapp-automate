package com.yourapp.nutrition.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_plans", indexes = {
    @Index(name = "idx_user_plan_date", columnList = "user_id, plan_date", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "target_calories")
    private Integer targetCalories;

    @Column(name = "total_calories")
    private Integer totalCalories;

    @Column(name = "total_protein")
    private Double totalProtein;

    @Column(name = "total_carbs")
    private Double totalCarbs;

    @Column(name = "total_fat")
    private Double totalFat;

    @Builder.Default
    @Column(length = 20)
    private String status = "ACTIVE"; // ACTIVE, COMPLETED, ARCHIVED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
