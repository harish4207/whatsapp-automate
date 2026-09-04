package com.yourapp.nutrition.controller;

import com.yourapp.nutrition.entity.MealPlan;
import com.yourapp.nutrition.entity.PlanMeal;
import com.yourapp.nutrition.repository.MealPlanRepository;
import com.yourapp.nutrition.repository.PlanMealRepository;
import com.yourapp.nutrition.visual.NutritionCardRenderer;
import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NutritionCardController dynamically serves visual PNG Nutrition Blueprint cards
 * accessible by Meta Cloud API and web clients.
 */
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Slf4j
public class NutritionCardController {

    private final MealPlanRepository mealPlanRepository;
    private final PlanMealRepository planMealRepository;
    private final UserRepository userRepository;
    private final NutritionCardRenderer cardRenderer;

    @GetMapping(value = "/{planId}.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getPlanCardImage(@PathVariable Long planId) {
        log.info("Serving visual nutrition card for Plan ID: {}", planId);

        MealPlan plan = mealPlanRepository.findById(planId).orElse(null);
        if (plan == null) {
            log.warn("Plan not found for ID: {}", planId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User user = userRepository.findById(plan.getUserId()).orElse(null);
        if (user == null) {
            log.warn("User not found for Plan ID: {}", planId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<PlanMeal> planMeals = planMealRepository.findByPlanId(planId);

        byte[] imageBytes = cardRenderer.renderCard(user, plan, planMeals);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(imageBytes.length);
        headers.setCacheControl("public, max-age=3600");

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK - WhatsApp Nutrition Engine Running");
    }
}
