package com.yourapp.nutrition.engine;

import com.yourapp.user.entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * NutritionEngine calculates baseline energy targets and macronutrient distributions
 * using clinically validated formulas (Mifflin-St Jeor).
 * 
 * ZERO LLM / ZERO API COST:
 * Computes target calories and macros in < 1ms deterministically.
 */
@Component
@Slf4j
public class NutritionEngine {

    private static final double ACTIVITY_MULTIPLIER = 1.35; // Moderate/Active lifestyle
    private static final int FAT_LOSS_DEFICIT = 400;
    private static final int MUSCLE_GAIN_SURPLUS = 300;
    private static final double PROTEIN_PER_KG = 1.8; // 1.8g per kg bodyweight

    @Data
    @Builder
    public static class NutritionTarget {
        private int targetCalories;
        private int proteinGrams;
        private int carbsGrams;
        private int fatGrams;
        private int breakfastCalories;
        private int lunchCalories;
        private int dinnerCalories;
        private int snackCalories;
    }

    public NutritionTarget calculateTarget(User user) {
        double weight = user.getWeight() != null ? user.getWeight() : 70.0;
        double height = user.getHeight() != null ? user.getHeight() : 170.0;
        int age = user.getAge() != null ? user.getAge() : 25;
        String sex = user.getSex() != null ? user.getSex() : "MALE";
        String goal = user.getGoal() != null ? user.getGoal() : "MAINTENANCE";

        // 1. Calculate Basal Metabolic Rate (BMR) using Mifflin-St Jeor
        double bmr;
        if ("FEMALE".equalsIgnoreCase(sex)) {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
        } else {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
        }

        // 2. Total Daily Energy Expenditure (TDEE)
        double tdee = bmr * ACTIVITY_MULTIPLIER;

        // 3. Goal Adjustment
        int targetCalories;
        if ("FAT_LOSS".equalsIgnoreCase(goal)) {
            targetCalories = (int) Math.round(tdee - FAT_LOSS_DEFICIT);
            if (targetCalories < 1300) targetCalories = 1300; // Safety floor
        } else if ("MUSCLE_GAIN".equalsIgnoreCase(goal)) {
            targetCalories = (int) Math.round(tdee + MUSCLE_GAIN_SURPLUS);
        } else {
            targetCalories = (int) Math.round(tdee);
        }

        // 4. Macro Allocation
        // Protein: 1.8g per kg of bodyweight
        int proteinGrams = (int) Math.round(weight * PROTEIN_PER_KG);
        int proteinCalories = proteinGrams * 4;

        // Fat: 25% of total calories
        int fatCalories = (int) Math.round(targetCalories * 0.25);
        int fatGrams = fatCalories / 9;

        // Carbohydrates: Remainder
        int remainingCalories = targetCalories - proteinCalories - fatCalories;
        int carbsGrams = Math.max(50, remainingCalories / 4);

        // 5. Per-Meal Calorie Budget
        // Breakfast: 25%, Lunch: 35%, Dinner: 30%, Snack: 10%
        int breakfastCalories = (int) Math.round(targetCalories * 0.25);
        int lunchCalories = (int) Math.round(targetCalories * 0.35);
        int dinnerCalories = (int) Math.round(targetCalories * 0.30);
        int snackCalories = targetCalories - (breakfastCalories + lunchCalories + dinnerCalories);

        log.info("Calculated target for user {}: Calories: {}, Protein: {}g, Carbs: {}g, Fat: {}g",
                user.getPhoneNumber(), targetCalories, proteinGrams, carbsGrams, fatGrams);

        return NutritionTarget.builder()
                .targetCalories(targetCalories)
                .proteinGrams(proteinGrams)
                .carbsGrams(carbsGrams)
                .fatGrams(fatGrams)
                .breakfastCalories(breakfastCalories)
                .lunchCalories(lunchCalories)
                .dinnerCalories(dinnerCalories)
                .snackCalories(snackCalories)
                .build();
    }
}
