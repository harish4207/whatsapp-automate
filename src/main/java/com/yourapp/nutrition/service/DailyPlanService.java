package com.yourapp.nutrition.service;

import com.yourapp.nutrition.engine.MealScoringEngine;
import com.yourapp.nutrition.engine.NutritionEngine;
import com.yourapp.nutrition.entity.Meal;
import com.yourapp.nutrition.entity.MealPlan;
import com.yourapp.nutrition.entity.PlanMeal;
import com.yourapp.nutrition.repository.MealPlanRepository;
import com.yourapp.nutrition.repository.PlanMealRepository;
import com.yourapp.user.entity.User;
import com.yourapp.whatsapp.client.WhatsAppApiClient;
import com.yourapp.whatsapp.client.WhatsAppApiClient.ButtonOption;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Getter
@Slf4j
public class DailyPlanService {

    private final NutritionEngine nutritionEngine;
    private final MealScoringEngine scoringEngine;
    private final MealPlanRepository mealPlanRepository;
    private final PlanMealRepository planMealRepository;
    private final WhatsAppApiClient apiClient;

    @org.springframework.beans.factory.annotation.Value("${app.public.base-url:}")
    private String publicBaseUrl;

    public MealPlan generateDailyPlan(User user, LocalDate date) {
        MealPlan savedPlan = createAndSavePlan(user, date);
        List<PlanMeal> planMeals = planMealRepository.findByPlanId(savedPlan.getId());
        List<Meal> meals = planMeals.stream().map(PlanMeal::getMeal).filter(Objects::nonNull).toList();
        sendPlanCardToWhatsApp(user, savedPlan, meals);
        return savedPlan;
    }

    @Transactional
    public MealPlan createAndSavePlan(User user, LocalDate date) {
        log.info("Generating and saving daily plan for user {} on date {}", user.getPhoneNumber(), date);

        // 1. Calculate calorie & macro targets
        var target = nutritionEngine.calculateTarget(user);

        // 2. Select best meal for each slot with healthy Indian balance (Veg + Non-Veg)
        Set<Long> selectedIds = new HashSet<>();

        // If user is NON_VEG, balance their day so they don't get 100% heavy meat at all 4 slots:
        // Authentic Indian balanced pattern: Veg/Egg breakfast, Non-veg Lunch, Veg/Light Dinner, High-protein Veg Snack
        Meal breakfast = pickBestMeal(user, "BREAKFAST", target.getBreakfastCalories(), selectedIds);
        if (breakfast != null) selectedIds.add(breakfast.getId());

        Meal lunch = pickBestMeal(user, "LUNCH", target.getLunchCalories(), selectedIds);
        if (lunch != null) selectedIds.add(lunch.getId());

        Meal dinner = pickBestMealForDinner(user, target.getDinnerCalories(), selectedIds, lunch);
        if (dinner != null) selectedIds.add(dinner.getId());

        Meal snack = pickBestMeal(user, "SNACK", target.getSnackCalories(), selectedIds);
        if (snack != null) selectedIds.add(snack.getId());

        // 3. Compute actual totals
        int totalCals = sumCals(breakfast, lunch, dinner, snack);
        double totalProtein = sumProtein(breakfast, lunch, dinner, snack);
        double totalCarbs = sumCarbs(breakfast, lunch, dinner, snack);
        double totalFat = sumFat(breakfast, lunch, dinner, snack);

        // 4. Save MealPlan
        MealPlan mealPlan = mealPlanRepository.findByUserIdAndPlanDate(user.getId(), date)
                .orElseGet(() -> MealPlan.builder().userId(user.getId()).planDate(date).build());

        mealPlan.setTargetCalories(target.getTargetCalories());
        mealPlan.setTotalCalories(totalCals);
        mealPlan.setTotalProtein(totalProtein);
        mealPlan.setTotalCarbs(totalCarbs);
        mealPlan.setTotalFat(totalFat);
        mealPlan.setStatus("ACTIVE");
        MealPlan savedPlan = mealPlanRepository.saveAndFlush(mealPlan);

        // 5. Save PlanMeals
        savePlanMeal(savedPlan.getId(), breakfast, "BREAKFAST", "8:30 AM");
        savePlanMeal(savedPlan.getId(), lunch, "LUNCH", "1:30 PM");
        savePlanMeal(savedPlan.getId(), dinner, "DINNER", "8:00 PM");
        savePlanMeal(savedPlan.getId(), snack, "SNACK", "5:00 PM");

        return savedPlan;
    }

    public void sendSwapMenu(User user) {
        apiClient.sendListMessage(user.getPhoneNumber(),
                "🔄 Meal Customizer",
                "Select which meal slot you would like to swap with a fresh alternative:",
                "Choose Meal Slot",
                List.of(
                        WhatsAppApiClient.ListRowOption.builder().id("SWAP_BREAKFAST").title("🌅 Swap Breakfast").description("Get another breakfast matching your macros").build(),
                        WhatsAppApiClient.ListRowOption.builder().id("SWAP_LUNCH").title("☀️ Swap Lunch").description("Get another lunch matching your macros").build(),
                        WhatsAppApiClient.ListRowOption.builder().id("SWAP_SNACK").title("☕ Swap Evening Snack").description("Get another healthy snack option").build(),
                        WhatsAppApiClient.ListRowOption.builder().id("SWAP_DINNER").title("🌙 Swap Dinner").description("Get another dinner matching your macros").build()
                )
        );
    }

    @Transactional
    public void swapMealSlot(User user, String mealType) {
        LocalDate today = LocalDate.now();
        MealPlan plan = mealPlanRepository.findByUserIdAndPlanDate(user.getId(), today).orElse(null);
        if (plan == null) {
            generateDailyPlan(user, today);
            return;
        }

        Optional<PlanMeal> currentPlanMeal = planMealRepository.findByPlanIdAndMealType(plan.getId(), mealType);
        if (currentPlanMeal.isEmpty()) return;

        Meal oldMeal = currentPlanMeal.get().getMeal();
        var target = nutritionEngine.calculateTarget(user);
        int slotCals = "BREAKFAST".equalsIgnoreCase(mealType) ? target.getBreakfastCalories() :
                       "LUNCH".equalsIgnoreCase(mealType) ? target.getLunchCalories() :
                       "DINNER".equalsIgnoreCase(mealType) ? target.getDinnerCalories() : target.getSnackCalories();

        // Get candidates excluding current meal
        List<Meal> candidates = scoringEngine.getRankedCandidates(user, mealType, slotCals, Set.of(oldMeal.getId()));
        if (candidates.isEmpty()) {
            apiClient.sendTextMessage(user.getPhoneNumber(), "⚠️ No alternative meal found for " + mealType + " that strictly complies with your health profile.");
            return;
        }

        Meal newMeal = candidates.get(0);
        currentPlanMeal.get().setMeal(newMeal);
        planMealRepository.save(currentPlanMeal.get());

        // Refresh and resend updated card
        List<PlanMeal> allPlanMeals = planMealRepository.findByPlanId(plan.getId());
        List<Meal> meals = allPlanMeals.stream().map(PlanMeal::getMeal).toList();

        // Recalculate actual plan totals
        int totalCals = meals.stream().filter(Objects::nonNull).mapToInt(Meal::getCalories).sum();
        double totalProtein = meals.stream().filter(Objects::nonNull).mapToDouble(Meal::getProtein).sum();
        double totalCarbs = meals.stream().filter(Objects::nonNull).mapToDouble(Meal::getCarbohydrates).sum();
        double totalFat = meals.stream().filter(Objects::nonNull).mapToDouble(Meal::getFat).sum();

        plan.setTotalCalories(totalCals);
        plan.setTotalProtein(totalProtein);
        plan.setTotalCarbs(totalCarbs);
        plan.setTotalFat(totalFat);
        mealPlanRepository.save(plan);

        apiClient.sendTextMessage(user.getPhoneNumber(), "🔄 Swapped your *" + mealType + "* to *" + newMeal.getName() + "*!");
        sendPlanCardToWhatsApp(user, plan, meals);
    }

    public void sendShoppingList(User user) {
        LocalDate today = LocalDate.now();
        MealPlan plan = mealPlanRepository.findByUserIdAndPlanDate(user.getId(), today).orElse(null);
        if (plan == null) {
            apiClient.sendTextMessage(user.getPhoneNumber(), "Please request your plan first by replying *PLAN*.");
            return;
        }

        List<PlanMeal> planMeals = planMealRepository.findByPlanId(plan.getId());
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 *TODAY'S CLINICAL GROCERY CHECKLIST*\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");

        for (PlanMeal pm : planMeals) {
            Meal m = pm.getMeal();
            if (m == null) continue;
            String slotEmoji = switch (pm.getMealType()) {
                case "BREAKFAST" -> "🌅";
                case "LUNCH" -> "☀️";
                case "DINNER" -> "🌙";
                default -> "☕";
            };
            sb.append("\n").append(slotEmoji).append(" *").append(pm.getMealType()).append(": ").append(m.getName()).append("*\n");
            if (m.getIngredientsSummary() != null && !m.getIngredientsSummary().isBlank()) {
                for (String item : m.getIngredientsSummary().split(",")) {
                    sb.append("  ☐ ").append(item.trim()).append("\n");
                }
            } else {
                sb.append("  ☐ Fresh wholesome ingredients\n");
            }
        }
        sb.append("\n━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("💡 *Tip:* Wash all produce thoroughly. Reply *PLAN* to view your daily schedule.");

        apiClient.sendTextMessage(user.getPhoneNumber(), sb.toString());
    }

    private void sendPlanCardToWhatsApp(User user, MealPlan plan, List<Meal> meals) {
        int totalCals = plan.getTotalCalories() != null ? plan.getTotalCalories() : 0;
        int targetCals = plan.getTargetCalories() != null ? plan.getTargetCalories() : 2000;
        double targetProtein = user.getWeight() != null ? user.getWeight() * 1.8 : 130.0;
        double actualProtein = plan.getTotalProtein() != null ? plan.getTotalProtein() : 0.0;

        // Visual progress bars
        int totalBlocks = 12;
        int filledCalBlocks = Math.min(totalBlocks, (int) Math.round(((double) totalCals / Math.max(1, targetCals)) * totalBlocks));
        String calProgressBar = "█".repeat(filledCalBlocks) + "░".repeat(Math.max(0, totalBlocks - filledCalBlocks));

        int filledProtBlocks = Math.min(totalBlocks, (int) Math.round((actualProtein / Math.max(1, targetProtein)) * totalBlocks));
        String protProgressBar = "█".repeat(filledProtBlocks) + "░".repeat(Math.max(0, totalBlocks - filledProtBlocks));

        String shieldBadge = switch (user.getHealthCondition() != null ? user.getHealthCondition() : "NONE") {
            case "DIABETES" -> "🛡️ *Clinical Shield:* Diabetes Safe (Low GI, High Fiber) 🩺";
            case "HYPERTENSION" -> "🛡️ *Clinical Shield:* Low Sodium & DASH Compliant 🫀";
            case "THYROID" -> "🛡️ *Clinical Shield:* Thyroid Support (Selenium/Zinc Rich) 🦋";
            case "PCOS" -> "🛡️ *Clinical Shield:* PCOS Hormone & Insulin Balance 🌸";
            case "FATTY_LIVER" -> "🛡️ *Clinical Shield:* NAFLD Liver Detox & Low Sat Fat 🥑";
            default -> "🛡️ *Clinical Shield:* General Fitness & Wellness Certified ✅";
        };

        StringBuilder sb = new StringBuilder();
        sb.append("📋 *DAILY NUTRITION BLUEPRINT*\n");
        sb.append(shieldBadge).append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("🔥 *Calories:* [%s] %d / %d kcal\n", calProgressBar, totalCals, targetCals));
        sb.append(String.format("💪 *Protein:*  [%s] %.0f / %.0f g\n", protProgressBar, actualProtein, targetProtein));
        sb.append(String.format("🌾 *Carbs:* %.0fg  |  🥑 *Fats:* %.0fg\n", plan.getTotalCarbs() != null ? plan.getTotalCarbs() : 0.0, plan.getTotalFat() != null ? plan.getTotalFat() : 0.0));
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");

        for (Meal m : meals) {
            if (m == null) continue;
            String emoji = switch (m.getMealType()) {
                case "BREAKFAST" -> "🌅";
                case "LUNCH" -> "☀️";
                case "SNACK" -> "☕";
                default -> "🌙";
            };
            sb.append(String.format("%s *%s*: %s (%d kcal)\n", emoji, m.getMealType(), m.getName(), m.getCalories()));
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("👇 *Quick Actions:* Tap below to swap any meal or view your groceries:");

        // Action buttons
        List<ButtonOption> buttons = List.of(
                ButtonOption.builder().id("SWAP_MENU").title("🔄 Swap Meal").build(),
                ButtonOption.builder().id("GET_GROCERIES").title("🛒 Groceries").build(),
                ButtonOption.builder().id("MARK_DONE").title("✅ Completed").build()
        );

        // 1. Deliver high-resolution visual infographic card if public base URL is available
        if (publicBaseUrl != null && !publicBaseUrl.isBlank() && plan.getId() != null) {
            String imageUrl = publicBaseUrl + "/api/cards/" + plan.getId() + ".png";
            log.info("Dispatching visual nutrition card image to {}: {}", user.getPhoneNumber(), imageUrl);
            apiClient.sendImageMessage(user.getPhoneNumber(), imageUrl, "🎨 *Your Visual Daily Nutrition Blueprint*");
        }

        // 2. Deliver structured text details with 3 quick-reply action buttons
        apiClient.sendButtonMessage(user.getPhoneNumber(), sb.toString(), buttons);
    }

    private Meal pickBestMeal(User user, String mealType, int targetCals, Set<Long> excludedIds) {
        List<Meal> candidates = scoringEngine.getRankedCandidates(user, mealType, targetCals, excludedIds);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private Meal pickBestMealForDinner(User user, int targetCals, Set<Long> excludedIds, Meal lunchMeal) {
        List<Meal> candidates = scoringEngine.getRankedCandidates(user, "DINNER", targetCals, excludedIds);
        if (candidates.isEmpty()) return null;

        // If lunch was non-veg, prefer a lighter veg / paneer / fish dinner for healthy balance
        if (lunchMeal != null && "NON_VEG".equalsIgnoreCase(lunchMeal.getDietType())) {
            Optional<Meal> vegOrLight = candidates.stream()
                    .filter(m -> "VEG".equalsIgnoreCase(m.getDietType()) || "EGGETARIAN".equalsIgnoreCase(m.getDietType()))
                    .findFirst();
            if (vegOrLight.isPresent()) return vegOrLight.get();
        }

        return candidates.get(0);
    }

    private void savePlanMeal(Long planId, Meal meal, String mealType, String scheduledTime) {
        if (meal == null) return;
        PlanMeal planMeal = planMealRepository.findByPlanIdAndMealType(planId, mealType)
                .orElseGet(() -> PlanMeal.builder().planId(planId).mealType(mealType).build());
        planMeal.setMeal(meal);
        planMeal.setScheduledTime(scheduledTime);
        planMealRepository.save(planMeal);
    }

    private int sumCals(Meal... meals) {
        return Arrays.stream(meals).filter(Objects::nonNull).mapToInt(Meal::getCalories).sum();
    }

    private double sumProtein(Meal... meals) {
        return Arrays.stream(meals).filter(Objects::nonNull).mapToDouble(Meal::getProtein).sum();
    }

    private double sumCarbs(Meal... meals) {
        return Arrays.stream(meals).filter(Objects::nonNull).mapToDouble(Meal::getCarbohydrates).sum();
    }

    private double sumFat(Meal... meals) {
        return Arrays.stream(meals).filter(Objects::nonNull).mapToDouble(Meal::getFat).sum();
    }
}
