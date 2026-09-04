package com.yourapp.nutrition.engine;

import com.yourapp.feedback.entity.MealFeedback;
import com.yourapp.feedback.repository.MealFeedbackRepository;
import com.yourapp.nutrition.entity.Meal;
import com.yourapp.nutrition.entity.MealConditionRule;
import com.yourapp.nutrition.repository.MealConditionRuleRepository;
import com.yourapp.nutrition.repository.MealRepository;
import com.yourapp.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MealScoringEngine executes multi-stage filtering and weighted ranking
 * to pick the highest scoring meal for each meal slot.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MealScoringEngine {

    private final MealRepository mealRepository;
    private final MealConditionRuleRepository conditionRuleRepository;
    private final com.yourapp.nutrition.repository.ConditionRepository conditionRepository;
    private final MealFeedbackRepository feedbackRepository;

    public List<Meal> getRankedCandidates(User user, String mealType, int targetSlotCalories, Set<Long> excludedMealIds) {
        // 1. Determine eligible diet types
        List<String> allowedDietTypes = getAllowedDietTypes(user.getDietType());

        // 2. Fetch candidate meals from database
        List<Meal> candidates = mealRepository.findByMealTypeAndDietTypeIn(mealType, allowedDietTypes);

        // 3. HARD FILTER: Exclude any meal with condition suitability = 'AVOID'
        // (In V1, if user has specific conditions, we filter out contra-indicated meal IDs)
        Set<Long> contraindicatedMealIds = getContraindicatedMealIds(user);

        // 4. Fetch user feedback history for preference scoring
        Map<Long, String> userFeedbackMap = getUserFeedbackMap(user.getId());

        // 5. Score and rank remaining candidates
        return candidates.stream()
                .filter(m -> !excludedMealIds.contains(m.getId()))
                .filter(m -> !contraindicatedMealIds.contains(m.getId()))
                .sorted(Comparator.comparingDouble((Meal m) -> calculateScore(m, user, targetSlotCalories, userFeedbackMap)).reversed())
                .toList();
    }

    private double calculateScore(Meal meal, User user, int targetSlotCalories, Map<Long, String> feedbackMap) {
        double score = 100.0;

        // A. Calorie Closeness (Penalize distance from target calories)
        int calorieDelta = Math.abs(meal.getCalories() - targetSlotCalories);
        score -= (calorieDelta * 0.3); // 100 calorie difference = -30 points

        // B. Cuisine Match Bonus
        if (user.getCuisine() != null && user.getCuisine().equalsIgnoreCase(meal.getCuisine())) {
            score += 40.0;
        }

        // C. Protein Density Bonus
        score += (meal.getProtein() * 1.5);

        // D. User Feedback Scoring
        if (feedbackMap.containsKey(meal.getId())) {
            String feedback = feedbackMap.get(meal.getId());
            if ("LIKED".equalsIgnoreCase(feedback)) {
                score += 30.0;
            } else if ("DISLIKED".equalsIgnoreCase(feedback)) {
                score -= 100.0;
            }
        }

        return score;
    }

    private List<String> getAllowedDietTypes(String userDiet) {
        if (userDiet == null) return List.of("VEG", "NON_VEG");

        return switch (userDiet.toUpperCase()) {
            case "VEG", "VEGETARIAN" -> List.of("VEG", "VEGAN");
            case "EGGETARIAN" -> List.of("VEG", "VEGAN", "EGGETARIAN");
            case "VEGAN" -> List.of("VEGAN");
            case "KETO" -> List.of("KETO");
            case "VEG_NON_VEG", "MIXED" -> List.of("VEG", "NON_VEG", "EGGETARIAN", "VEGAN");
            default -> List.of("VEG", "NON_VEG", "EGGETARIAN", "VEGAN");
        };
    }

    private Set<Long> getContraindicatedMealIds(User user) {
        if (user.getHealthCondition() == null || "NONE".equalsIgnoreCase(user.getHealthCondition())) {
            return Collections.emptySet();
        }

        Optional<com.yourapp.nutrition.entity.Condition> condOpt = conditionRepository.findByNameIgnoreCase(user.getHealthCondition());
        if (condOpt.isEmpty()) {
            return Collections.emptySet();
        }

        List<MealConditionRule> rules = conditionRuleRepository.findByConditionIdInAndSuitability(
                List.of(condOpt.get().getId()), "AVOID"
        );

        Set<Long> avoidMealIds = new HashSet<>();
        for (MealConditionRule rule : rules) {
            avoidMealIds.add(rule.getMealId());
        }
        return avoidMealIds;
    }

    private Map<Long, String> getUserFeedbackMap(Long userId) {
        if (userId == null) return Collections.emptyMap();
        List<MealFeedback> feedbackList = feedbackRepository.findByUserId(userId);
        Map<Long, String> map = new HashMap<>();
        for (MealFeedback fb : feedbackList) {
            map.put(fb.getMealId(), fb.getFeedback());
        }
        return map;
    }
}
