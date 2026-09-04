package com.yourapp.nutrition.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meals", indexes = {
    @Index(name = "idx_meals_type_diet", columnList = "meal_type, diet_type"),
    @Index(name = "idx_meals_cuisine", columnList = "cuisine")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "meal_type", length = 20, nullable = false)
    private String mealType; // BREAKFAST, LUNCH, DINNER, SNACK

    @Column(length = 50)
    private String cuisine; // NORTH_INDIAN, SOUTH_INDIAN, CONTINENTAL

    @Column(name = "diet_type", length = 50, nullable = false)
    private String dietType; // VEG, NON_VEG, VEGAN, EGGETARIAN, KETO

    @Column(name = "prep_time")
    private Integer prepTime; // in minutes

    @Column(name = "cost_level", length = 20)
    private String costLevel; // BUDGET, MODERATE, PREMIUM

    @Column(nullable = false)
    private Integer calories;

    @Builder.Default
    private Double protein = 0.0;

    @Builder.Default
    private Double carbohydrates = 0.0;

    @Builder.Default
    private Double fat = 0.0;

    @Builder.Default
    private Double fiber = 0.0;

    @Column(name = "ingredients_summary", length = 500)
    private String ingredientsSummary;
}
