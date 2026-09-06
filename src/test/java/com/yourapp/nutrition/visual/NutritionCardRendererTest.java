package com.yourapp.nutrition.visual;

import com.yourapp.nutrition.entity.Meal;
import com.yourapp.nutrition.entity.MealPlan;
import com.yourapp.nutrition.entity.PlanMeal;
import com.yourapp.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NutritionCardRendererTest {

    private final NutritionCardRenderer renderer = new NutritionCardRenderer();

    @Test
    @DisplayName("Should render a valid 1200x850 PNG image for meal plan")
    void testRenderCard() throws IOException {
        User user = User.builder()
                .name("Harish")
                .age(25)
                .sex("MALE")
                .height(175.0)
                .weight(70.0)
                .goal("FAT_LOSS")
                .dietType("VEG")
                .cuisine("SOUTH_INDIAN")
                .healthCondition("DIABETES")
                .build();

        MealPlan plan = MealPlan.builder()
                .planDate(LocalDate.now())
                .targetCalories(2000)
                .totalCalories(1980)
                .totalProtein(135.0)
                .totalCarbs(210.0)
                .totalFat(52.0)
                .build();

        Meal meal = Meal.builder()
                .name("Andhra Pesarattu & Allam Chutney")
                .calories(380)
                .protein(22.0)
                .carbohydrates(50.0)
                .fat(9.0)
                .fiber(9.0)
                .ingredientsSummary("2 Medium Green Moong Dal Dosas (150g), 40g Ginger Chutney")
                .build();

        List<PlanMeal> planMeals = List.of(
                PlanMeal.builder().mealType("BREAKFAST").meal(meal).build(),
                PlanMeal.builder().mealType("LUNCH").meal(meal).build(),
                PlanMeal.builder().mealType("SNACK").meal(meal).build(),
                PlanMeal.builder().mealType("DINNER").meal(meal).build()
        );

        byte[] pngBytes = renderer.renderCard(user, plan, planMeals);

        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > 10000, "PNG image should be non-empty and greater than 10KB");

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(pngBytes));
        assertNotNull(img);
        assertEquals(1200, img.getWidth());
        assertEquals(860, img.getHeight());
    }
}
