package com.yourapp.nutrition.repository;

import com.yourapp.nutrition.entity.MealConditionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MealConditionRuleRepository extends JpaRepository<MealConditionRule, Long> {

    List<MealConditionRule> findByConditionIdInAndSuitability(Collection<Long> conditionIds, String suitability);

    List<MealConditionRule> findByMealId(Long mealId);
}
