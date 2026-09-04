package com.yourapp.nutrition.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_preferences", indexes = {
    @Index(name = "idx_pref_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 100, nullable = false)
    private String item; // e.g. "Paneer", "Eggs", "Oats", "Fish"

    @Column(length = 20, nullable = false)
    private String type; // LIKES, DISLIKES

    @Builder.Default
    private Integer strength = 1;
}
