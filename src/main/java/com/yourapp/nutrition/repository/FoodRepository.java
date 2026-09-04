package com.yourapp.nutrition.repository;

import com.yourapp.nutrition.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    Optional<Food> findByNameIgnoreCase(String name);
}
