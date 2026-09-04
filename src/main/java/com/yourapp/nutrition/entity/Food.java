package com.yourapp.nutrition.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "foods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 50)
    private String category; // GRAIN, PROTEIN, VEGETABLE, FRUIT, DAIRY

    private Integer calories;

    @Builder.Default
    private Double protein = 0.0;

    @Builder.Default
    private Double carbohydrates = 0.0;

    @Builder.Default
    private Double fat = 0.0;

    @Builder.Default
    private Double fiber = 0.0;
}
