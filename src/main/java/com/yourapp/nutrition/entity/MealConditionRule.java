package com.yourapp.nutrition.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meal_condition_rules", indexes = {
    @Index(name = "idx_meal_condition", columnList = "meal_id, condition_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealConditionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meal_id", nullable = false)
    private Long mealId;

    @Column(name = "condition_id", nullable = false)
    private Long conditionId;

    @Column(length = 20, nullable = false)
    private String suitability; // SUITABLE, AVOID, MODERATE

    @Column(columnDefinition = "TEXT")
    private String notes;
}
