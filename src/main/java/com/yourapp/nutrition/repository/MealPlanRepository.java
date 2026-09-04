package com.yourapp.nutrition.repository;

import com.yourapp.nutrition.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    Optional<MealPlan> findByUserIdAndPlanDate(Long userId, LocalDate planDate);
}
