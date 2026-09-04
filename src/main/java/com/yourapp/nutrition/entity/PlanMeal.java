package com.yourapp.nutrition.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_meals", indexes = {
    @Index(name = "idx_plan_meal", columnList = "plan_id, meal_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    @Column(name = "meal_type", length = 20, nullable = false)
    private String mealType; // BREAKFAST, LUNCH, DINNER, SNACK

    @Column(name = "scheduled_time", length = 20)
    private String scheduledTime; // e.g. "8:30 AM"
}
