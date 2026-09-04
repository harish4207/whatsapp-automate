package com.yourapp.nutrition.repository;

import com.yourapp.nutrition.entity.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MealRepository extends JpaRepository<Meal, Long> {

    List<Meal> findByMealType(String mealType);

    List<Meal> findByMealTypeAndDietTypeIn(String mealType, Collection<String> dietTypes);

    @Query("SELECT m FROM Meal m WHERE m.mealType = :mealType AND m.dietType IN :dietTypes AND m.id NOT IN :excludedIds")
    List<Meal> findEligibleMeals(
            @Param("mealType") String mealType,
            @Param("dietTypes") Collection<String> dietTypes,
            @Param("excludedIds") Collection<Long> excludedIds
    );
}
