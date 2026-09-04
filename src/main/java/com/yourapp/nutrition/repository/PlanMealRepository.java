package com.yourapp.nutrition.repository;

import com.yourapp.nutrition.entity.PlanMeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanMealRepository extends JpaRepository<PlanMeal, Long> {

    List<PlanMeal> findByPlanId(Long planId);

    Optional<PlanMeal> findByPlanIdAndMealType(Long planId, String mealType);
}
