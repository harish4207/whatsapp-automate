package com.yourapp.nutrition.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "allergies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Allergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false, unique = true)
    private String name; // e.g. PEANUTS, DAIRY, GLUTEN, SHELLFISH, SOY
}
