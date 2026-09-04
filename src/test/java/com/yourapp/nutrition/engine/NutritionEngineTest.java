package com.yourapp.nutrition.engine;

import com.yourapp.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NutritionEngineTest {

    private final NutritionEngine nutritionEngine = new NutritionEngine();

    @Test
    @DisplayName("Should calculate accurate targets for Male Fat Loss")
    void testMaleFatLossTarget() {
        User user = User.builder()
                .sex("MALE")
                .age(28)
                .height(178.0)
                .weight(75.0)
                .goal("FAT_LOSS")
                .build();

        var target = nutritionEngine.calculateTarget(user);

        // BMR = (10 * 75) + (6.25 * 178) - (5 * 28) + 5 = 750 + 1112.5 - 140 + 5 = 1727.5
        // TDEE = 1727.5 * 1.35 = 2332.125
        // FAT_LOSS (-400) = ~1932 kcal
        assertTrue(target.getTargetCalories() >= 1850 && target.getTargetCalories() <= 2000, 
                "Expected calories around 1932, got: " + target.getTargetCalories());

        // Protein: 75 * 1.8 = 135g
        assertEquals(135, target.getProteinGrams());

        // Carbs and Fat should be positive and balanced
        assertTrue(target.getCarbsGrams() > 100);
        assertTrue(target.getFatGrams() > 30);
    }

    @Test
    @DisplayName("Should calculate accurate targets for Female Muscle Gain")
    void testFemaleMuscleGainTarget() {
        User user = User.builder()
                .sex("FEMALE")
                .age(25)
                .height(165.0)
                .weight(58.0)
                .goal("MUSCLE_GAIN")
                .build();

        var target = nutritionEngine.calculateTarget(user);

        // BMR = (10 * 58) + (6.25 * 165) - (5 * 25) - 161 = 580 + 1031.25 - 125 - 161 = 1325.25
        // TDEE = 1325.25 * 1.35 = 1789.08
        // MUSCLE_GAIN (+300) = ~2089 kcal
        assertTrue(target.getTargetCalories() >= 2000 && target.getTargetCalories() <= 2150,
                "Expected calories around 2089, got: " + target.getTargetCalories());

        // Protein: 58 * 1.8 = ~104g
        assertEquals(104, target.getProteinGrams());
    }
}
