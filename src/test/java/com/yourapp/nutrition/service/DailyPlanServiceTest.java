package com.yourapp.nutrition.service;

import com.yourapp.nutrition.entity.Meal;
import com.yourapp.nutrition.entity.MealPlan;
import com.yourapp.nutrition.entity.PlanMeal;
import com.yourapp.nutrition.repository.MealPlanRepository;
import com.yourapp.nutrition.repository.MealRepository;
import com.yourapp.nutrition.repository.PlanMealRepository;
import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import com.yourapp.whatsapp.client.WhatsAppApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
class DailyPlanServiceTest {

    @Autowired
    private DailyPlanService dailyPlanService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MealRepository mealRepository;

    @Autowired
    private MealPlanRepository mealPlanRepository;

    @Autowired
    private PlanMealRepository planMealRepository;

    @MockitoBean
    private WhatsAppApiClient apiClient;

    private User testUser;

    @BeforeEach
    void setUp() {
        planMealRepository.deleteAll();
        mealPlanRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .phoneNumber("919988776655")
                .name("Ravi")
                .age(28)
                .sex("MALE")
                .height(178.0)
                .weight(75.0)
                .goal("FAT_LOSS")
                .dietType("VEG")
                .cuisine("NORTH_INDIAN")
                .active(true)
                .build());
    }

    @Test
    @DisplayName("Should generate a complete 4-meal daily plan and deliver to WhatsApp")
    void testGenerateDailyPlan() {
        LocalDate today = LocalDate.now();
        MealPlan plan = dailyPlanService.generateDailyPlan(testUser, today);

        assertNotNull(plan);
        assertNotNull(plan.getId());
        assertTrue(plan.getTotalCalories() > 1500, "Total calories should be above 1500");

        List<PlanMeal> meals = planMealRepository.findByPlanId(plan.getId());
        assertEquals(4, meals.size(), "Should contain Breakfast, Lunch, Dinner, Snack");

        // Verify message was delivered via WhatsAppApiClient
        verify(apiClient, atLeastOnce()).sendButtonMessage(eq(testUser.getPhoneNumber()), any(), any());
    }

    @Test
    @DisplayName("Should swap a meal slot and update the database")
    void testSwapMealSlot() {
        LocalDate today = LocalDate.now();
        MealPlan plan = dailyPlanService.generateDailyPlan(testUser, today);

        PlanMeal initialLunch = planMealRepository.findByPlanIdAndMealType(plan.getId(), "LUNCH").orElseThrow();
        Long initialMealId = initialLunch.getMeal().getId();

        // Trigger Lunch swap
        dailyPlanService.swapMealSlot(testUser, "LUNCH");

        PlanMeal updatedLunch = planMealRepository.findByPlanIdAndMealType(plan.getId(), "LUNCH").orElseThrow();
        assertNotEquals(initialMealId, updatedLunch.getMeal().getId(), "Lunch meal should be swapped to an alternate");
    }

    @Test
    @DisplayName("Should send structured shopping list checklist")
    void testSendShoppingList() {
        LocalDate today = LocalDate.now();
        dailyPlanService.generateDailyPlan(testUser, today);

        dailyPlanService.sendShoppingList(testUser);

        verify(apiClient, atLeastOnce()).sendTextMessage(eq(testUser.getPhoneNumber()), any());
    }

    @Test
    @DisplayName("Should send interactive list swap menu with 4 slots")
    void testSendSwapMenu() {
        dailyPlanService.sendSwapMenu(testUser);
        verify(apiClient, atLeastOnce()).sendListMessage(eq(testUser.getPhoneNumber()), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should enforce clinical contraindications for Diabetic user")
    void testDiabeticContraindications() {
        testUser.setHealthCondition("DIABETES");
        userRepository.save(testUser);

        LocalDate today = LocalDate.now();
        MealPlan plan = dailyPlanService.generateDailyPlan(testUser, today);

        assertNotNull(plan);
        List<PlanMeal> planMeals = planMealRepository.findByPlanId(plan.getId());
        for (PlanMeal pm : planMeals) {
            Meal m = pm.getMeal();
            // Diabetic plan should avoid extreme high-carb meals (>65g)
            assertTrue(m.getCarbohydrates() <= 65.0, 
                    "Diabetic plan should strictly avoid meals with carb load > 65g. Found: " + m.getName() + " with " + m.getCarbohydrates() + "g carbs");
        }
    }
}
