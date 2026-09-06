package com.yourapp.onboarding.service;

import com.yourapp.onboarding.entity.ConversationState;
import com.yourapp.onboarding.repository.ConversationStateRepository;
import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import com.yourapp.whatsapp.client.WhatsAppApiClient;
import com.yourapp.whatsapp.client.WhatsAppApiClient.ButtonOption;
import com.yourapp.whatsapp.client.WhatsAppApiClient.ListRowOption;
import com.yourapp.whatsapp.dto.MetaWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OnboardingService orchestrates the conversational state machine.
 * 
 * WHY STATE MACHINE:
 * Each user message arrives independently. This service inspects the user's
 * current step in `conversation_state`, processes their input, updates their
 * profile, and prompts them for the next piece of information.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final UserRepository userRepository;
    private final ConversationStateRepository stateRepository;
    private final WhatsAppApiClient apiClient;
    private final com.yourapp.nutrition.service.DailyPlanService dailyPlanService;
    private final com.yourapp.ai.GeminiNutritionistService geminiNutritionistService;
    private final com.yourapp.nutrition.repository.DailyProgressRepository progressRepository;

    private static final Pattern VITALS_PATTERN = Pattern.compile("(\\d{1,3})\\s*[,\\s]\\s*(\\d{2,3})\\s*(?:cm)?\\s*[,\\s]\\s*(\\d{2,3})\\s*(?:kg)?", Pattern.CASE_INSENSITIVE);

    @Transactional
    public void handleIncomingMessage(String phoneNumber, MetaWebhookPayload.Message message, String senderName) {
        log.info("Handling incoming message for {}. Type: {}", phoneNumber, message.getType());

        // 1. Fetch or initialize User and State
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> createUser(phoneNumber, senderName));

        ConversationState state = stateRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> createInitialState(user.getId(), phoneNumber));

        // 2. Route based on state & step
        if ("ACTIVE".equalsIgnoreCase(state.getState())) {
            handleActiveUserState(user, message);
            return;
        }

        // Onboarding flow
        processOnboardingStep(user, state, message);
    }

    private void processOnboardingStep(User user, ConversationState state, MetaWebhookPayload.Message message) {
        String step = state.getCurrentStep();
        log.info("Processing step '{}' for user {}", step, user.getPhoneNumber());

        switch (step) {
            case "START":
            case "AWAITING_GOAL":
                handleGoalStep(user, state, message);
                break;
            case "AWAITING_SEX":
                handleSexStep(user, state, message);
                break;
            case "AWAITING_VITALS":
                handleVitalsStep(user, state, message);
                break;
            case "AWAITING_DIET":
                handleDietStep(user, state, message);
                break;
            case "AWAITING_CUISINE":
                handleCuisineStep(user, state, message);
                break;
            case "AWAITING_CONDITIONS":
                handleConditionsStep(user, state, message);
                break;
            default:
                sendGoalPrompt(user.getPhoneNumber());
                state.setCurrentStep("AWAITING_GOAL");
                stateRepository.save(state);
                break;
        }
    }

    private void handleGoalStep(User user, ConversationState state, MetaWebhookPayload.Message message) {
        String buttonId = extractButtonId(message);

        if (buttonId == null || !buttonId.startsWith("GOAL_")) {
            sendGoalPrompt(user.getPhoneNumber());
            if (!"AWAITING_GOAL".equals(state.getCurrentStep())) {
                state.setCurrentStep("AWAITING_GOAL");
                stateRepository.save(state);
            }
            return;
        }

        String goal = buttonId.replace("GOAL_", "");
        user.setGoal(goal);
        userRepository.save(user);

        state.setCurrentStep("AWAITING_SEX");
        stateRepository.save(state);

        // Prompt for Sex
        apiClient.sendButtonMessage(user.getPhoneNumber(),
                "Great! What is your biological sex?",
                List.of(
                        ButtonOption.builder().id("SEX_MALE").title("Male 👨").build(),
                        ButtonOption.builder().id("SEX_FEMALE").title("Female 👩").build(),
                        ButtonOption.builder().id("SEX_OTHER").title("Other 🧑").build()
                )
        );
    }

    private void handleSexStep(User user, ConversationState state, MetaWebhookPayload.Message message) {
        String buttonId = extractButtonId(message);

        if (buttonId == null || !buttonId.startsWith("SEX_")) {
            apiClient.sendButtonMessage(user.getPhoneNumber(),
                    "Please select your sex using the buttons below:",
                    List.of(
                            ButtonOption.builder().id("SEX_MALE").title("Male 👨").build(),
                            ButtonOption.builder().id("SEX_FEMALE").title("Female 👩").build(),
                            ButtonOption.builder().id("SEX_OTHER").title("Other 🧑").build()
                    )
            );
            return;
        }

        user.setSex(buttonId.replace("SEX_", ""));
        userRepository.save(user);

        state.setCurrentStep("AWAITING_VITALS");
        stateRepository.save(state);

        // Prompt for Age, Height, Weight
        apiClient.sendTextMessage(user.getPhoneNumber(),
                "Got it! Now reply with your *Age, Height (cm), and Weight (kg)* in a single line.\n\n" +
                "📌 *Example:* `25, 175cm, 70kg`"
        );
    }

    private void handleVitalsStep(User user, ConversationState state, MetaWebhookPayload.Message message) {
        String text = extractText(message);
        if (text == null) {
            apiClient.sendTextMessage(user.getPhoneNumber(), "Please reply with your Age, Height, and Weight. Example: `25, 175cm, 70kg`");
            return;
        }

        Matcher matcher = VITALS_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            int age = Integer.parseInt(matcher.group(1));
            double height = Double.parseDouble(matcher.group(2));
            double weight = Double.parseDouble(matcher.group(3));

            user.setAge(age);
            user.setHeight(height);
            user.setWeight(weight);
            userRepository.save(user);

            state.setCurrentStep("AWAITING_DIET");
            stateRepository.save(state);

            // Send Interactive List Menu for Diet Type
            apiClient.sendListMessage(user.getPhoneNumber(),
                    "Diet Preference",
                    "Choose the dietary style that best fits your lifestyle:",
                    "Select Diet",
                    List.of(
                            ListRowOption.builder().id("DIET_VEG_NON_VEG").title("Veg + Non-Veg 🍛🍗").description("Balanced mix of Veg & Non-Veg meals").build(),
                            ListRowOption.builder().id("DIET_VEG").title("Pure Vegetarian 🥬").description("Lacto-veg (Dal, Paneer, Veggies, Dairy)").build(),
                            ListRowOption.builder().id("DIET_EGGETARIAN").title("Eggetarian 🥚").description("Vegetarian diet including whole eggs").build(),
                            ListRowOption.builder().id("DIET_NON_VEG").title("Non-Vegetarian 🍗").description("Chicken, Fish, Eggs & Dairy").build(),
                            ListRowOption.builder().id("DIET_VEGAN").title("Vegan 🌱").description("100% plant-based, strictly zero dairy").build(),
                            ListRowOption.builder().id("DIET_KETO").title("Keto 🥑").description("Ultra low-carb, high healthy fats").build()
                    )
            );
        } else {
            apiClient.sendTextMessage(user.getPhoneNumber(),
                    "⚠️ Could not parse your response. Please format it like this:\n`Age, Height cm, Weight kg`\n\n📌 *Example:* `26, 178cm, 72kg`"
            );
        }
    }

    private void handleDietStep(User user, ConversationState state, MetaWebhookPayload.Message message) {
        String selectedId = extractInteractiveId(message);
        if (selectedId == null || !selectedId.startsWith("DIET_")) {
            apiClient.sendTextMessage(user.getPhoneNumber(), "Please select your diet from the list menu.");
            return;
        }

        user.setDietType(selectedId.replace("DIET_", ""));
        userRepository.save(user);

        state.setCurrentStep("AWAITING_CUISINE");
        stateRepository.save(state);

        // Prompt for Cuisine
        apiClient.sendButtonMessage(user.getPhoneNumber(),
                "Which cuisine do you prefer for your daily meals?",
                List.of(
                        ButtonOption.builder().id("CUISINE_NORTH_INDIAN").title("North Indian 🍛").build(),
                        ButtonOption.builder().id("CUISINE_SOUTH_INDIAN").title("South Indian 🥞").build(),
                        ButtonOption.builder().id("CUISINE_CONTINENTAL").title("Continental 🥗").build()
                )
        );
    }

    private void handleCuisineStep(User user, ConversationState state, MetaWebhookPayload.Message message) {
        String buttonId = extractButtonId(message);
        if (buttonId == null || !buttonId.startsWith("CUISINE_")) {
            apiClient.sendButtonMessage(user.getPhoneNumber(),
                    "Please select your preferred cuisine:",
                    List.of(
                            ButtonOption.builder().id("CUISINE_NORTH_INDIAN").title("North Indian 🍛").build(),
                            ButtonOption.builder().id("CUISINE_SOUTH_INDIAN").title("South Indian 🥞").build(),
                            ButtonOption.builder().id("CUISINE_CONTINENTAL").title("Continental 🥗").build()
                    )
            );
            return;
        }

        user.setCuisine(buttonId.replace("CUISINE_", ""));
        userRepository.save(user);

        state.setCurrentStep("AWAITING_CONDITIONS");
        stateRepository.save(state);

        // Prompt for Health Conditions using Interactive List Menu (6 options)
        apiClient.sendListMessage(user.getPhoneNumber(),
                "Health & Medical Profile 🩺",
                "Do you have any existing health conditions? We apply strict clinical contraindications to keep you 100% safe:",
                "Select Condition",
                List.of(
                        ListRowOption.builder().id("COND_NONE").title("General Fitness ✅").description("No chronic conditions; balanced nutrition").build(),
                        ListRowOption.builder().id("COND_DIABETES").title("Diabetes / Sugar 🩺").description("Low GI, fiber-rich, strictly zero simple sugars").build(),
                        ListRowOption.builder().id("COND_HYPERTENSION").title("High BP / Hyper 🫀").description("Low sodium, potassium-rich, DASH principles").build(),
                        ListRowOption.builder().id("COND_THYROID").title("Thyroid Support 🦋").description("Selenium & zinc rich, avoids raw goitrogens").build(),
                        ListRowOption.builder().id("COND_PCOS").title("PCOS / PCOD 🌸").description("Hormone balancing, anti-inflammatory, low GI").build(),
                        ListRowOption.builder().id("COND_FATTY_LIVER").title("Fatty Liver Care 🥑").description("Low saturated fats, choline, liver detox support").build()
                )
        );
    }

    private void handleConditionsStep(User user, ConversationState state, MetaWebhookPayload.Message message) {
        String interactiveId = extractInteractiveId(message);
        if (interactiveId == null || !interactiveId.startsWith("COND_")) {
            apiClient.sendListMessage(user.getPhoneNumber(),
                    "Health & Medical Profile 🩺",
                    "Please select one of the following health profiles:",
                    "Select Condition",
                    List.of(
                            ListRowOption.builder().id("COND_NONE").title("General Fitness ✅").description("No chronic conditions; balanced nutrition").build(),
                            ListRowOption.builder().id("COND_DIABETES").title("Diabetes / Sugar 🩺").description("Low GI, fiber-rich, strictly zero simple sugars").build(),
                            ListRowOption.builder().id("COND_HYPERTENSION").title("High BP / Hyper 🫀").description("Low sodium, potassium-rich, DASH principles").build(),
                            ListRowOption.builder().id("COND_THYROID").title("Thyroid Support 🦋").description("Selenium & zinc rich, avoids raw goitrogens").build(),
                            ListRowOption.builder().id("COND_PCOS").title("PCOS / PCOD 🌸").description("Hormone balancing, anti-inflammatory, low GI").build(),
                            ListRowOption.builder().id("COND_FATTY_LIVER").title("Fatty Liver Care 🥑").description("Low saturated fats, choline, liver detox support").build()
                    )
            );
            return;
        }

        String condition = interactiveId.replace("COND_", "");
        user.setHealthCondition(condition);
        userRepository.save(user);

        // Onboarding complete!
        state.setState("ACTIVE");
        state.setCurrentStep("COMPLETE");
        stateRepository.save(state);

        String conditionDisplay = switch (condition) {
            case "DIABETES" -> "Diabetes Safe (Low GI) 🩺";
            case "HYPERTENSION" -> "Hypertension / Low Sodium 🫀";
            case "THYROID" -> "Thyroid Support 🦋";
            case "PCOS" -> "PCOS / Hormone Balance 🌸";
            case "FATTY_LIVER" -> "Fatty Liver / Low Sat Fat 🥑";
            default -> "General Fitness & Health ✅";
        };

        String summary = String.format(
                "🎉 *Profile Complete!*\n\n" +
                "👤 *Name:* %s\n" +
                "🎯 *Goal:* %s\n" +
                "📏 *Stats:* %s yrs | %.0f cm | %.0f kg\n" +
                "🥗 *Diet:* %s (%s cuisine)\n" +
                "🛡️ *Shield:* %s\n\n" +
                "⚡ *Compiling your scientifically calibrated daily meal plan now...*",
                user.getName() != null ? user.getName() : "Friend",
                user.getGoal(),
                user.getAge(),
                user.getHeight(),
                user.getWeight(),
                user.getDietType(),
                user.getCuisine(),
                conditionDisplay
        );

        apiClient.sendTextMessage(user.getPhoneNumber(), summary);

        // Feature Showcase Guide: Ensure user knows all capabilities
        String featureGuide = """
                ✨ *WELCOME TO YOUR SMART NUTRITION ASSISTANT!*
                
                Here is what you can do anytime:
                
                1. 📸 *Food Plate Scanner*: Just take a photo of any meal or snack and send it. Dr. Aanya will analyze the calories, protein & clinical safety!
                2. 🎙️ *Voice Notes*: Hold down the WhatsApp mic and speak in Telugu, Hindi, or English!
                3. 📝 *Natural Calorie Logger*: Just text *"I ate 2 idlis"* or *"Had chicken curry"* to auto-log your calories!
                4. 💧 *Hydration Tracker*: Reply *WATER* to log cups with visual progress bars.
                5. 👩‍🍳 *Healthy Recipes*: Text *"Recipe <Dish Name>"* for condition-safe 4-step Indian cooking guides.
                6. 🔄 *Meal Swapping*: Tap *Swap Meal* below your daily plan to choose alternative dishes.
                
                🚀 *Here is your customized Daily Blueprint:*
                """;
        apiClient.sendTextMessage(user.getPhoneNumber(), featureGuide);

        // Immediately generate and deliver the first day's plan
        dailyPlanService.generateDailyPlan(user, java.time.LocalDate.now());
    }

    private void handleActiveUserState(User user, MetaWebhookPayload.Message message) {
        String text = extractText(message);
        String actionId = extractInteractiveId(message);

        if ("RESET".equalsIgnoreCase(text) || "START".equalsIgnoreCase(text)) {
            ConversationState state = stateRepository.findByUserId(user.getId())
                    .orElse(createInitialState(user.getId(), user.getPhoneNumber()));
            state.setState("ONBOARDING");
            state.setCurrentStep("START");
            stateRepository.save(state);
            sendGoalPrompt(user.getPhoneNumber());
        } else if ("image".equalsIgnoreCase(message.getType()) && message.getImage() != null) {
            handleMealImageUpload(user, message);
        } else if ("audio".equalsIgnoreCase(message.getType()) && message.getAudio() != null) {
            handleVoiceNoteUpload(user, message);
        } else if ("PLAN".equalsIgnoreCase(text) || "TODAY".equalsIgnoreCase(text)) {
            dailyPlanService.generateDailyPlan(user, java.time.LocalDate.now());
        } else if ("WATER_MENU".equals(actionId) || "WATER".equalsIgnoreCase(text)) {
            sendWaterTracker(user);
        } else if ("WATER_ADD_250".equals(actionId)) {
            logWaterIntake(user, 250);
        } else if ("WATER_ADD_500".equals(actionId)) {
            logWaterIntake(user, 500);
        } else if ("WATER_RESET".equals(actionId)) {
            resetWaterIntake(user);
        } else if ("SWAP_MENU".equals(actionId)) {
            dailyPlanService.sendSwapMenu(user);
        } else if ("SWAP_BREAKFAST".equals(actionId)) {
            dailyPlanService.swapMealSlot(user, "BREAKFAST");
        } else if ("SWAP_LUNCH".equals(actionId)) {
            dailyPlanService.swapMealSlot(user, "LUNCH");
        } else if ("SWAP_DINNER".equals(actionId)) {
            dailyPlanService.swapMealSlot(user, "DINNER");
        } else if ("SWAP_SNACK".equals(actionId)) {
            dailyPlanService.swapMealSlot(user, "SNACK");
        } else if ("GET_GROCERIES".equals(actionId) || "GROCERIES".equalsIgnoreCase(text)) {
            dailyPlanService.sendShoppingList(user);
        } else if ("MARK_DONE".equals(actionId)) {
            logMealCompletion(user);
        } else if (text != null && text.trim().toLowerCase().startsWith("recipe ")) {
            String mealDish = text.substring(7).trim();
            apiClient.sendTextMessage(user.getPhoneNumber(), "👩‍🍳 *Dr. Aanya's Healthy Kitchen*:\nPreparing healthy clinical recipe for *" + mealDish + "*...");
            String recipe = geminiNutritionistService.generateRecipe(user, mealDish);
            apiClient.sendTextMessage(user.getPhoneNumber(), recipe);
        } else if (isFoodIntakeMessage(text)) {
            handleFoodIntakeLog(user, text);
        } else if (text != null && !text.isBlank()) {
            log.info("Routing free-form nutrition inquiry to AI Clinical Service for user: {}", user.getPhoneNumber());
            
            // Dynamic Care Memory Extraction: Check if user shared personal health struggles, cravings, or mood
            updateUserCareMemory(user, text);

            String aiAnswer = geminiNutritionistService.askNutritionist(user, text);
            apiClient.sendTextMessage(user.getPhoneNumber(), aiAnswer);
        } else {
            apiClient.sendTextMessage(user.getPhoneNumber(), 
                    "Hello " + (user.getName() != null ? user.getName() : "") + "! Reply *PLAN* for today's meals, *WATER* to log hydration, send a *food photo* 📸 to scan calories, or ask any health question!");
        }
    }

    private void updateUserCareMemory(User user, String text) {
        String lower = text.toLowerCase();
        boolean updated = false;

        if (lower.contains("bloat") || lower.contains("gas") || lower.contains("stomach pain") || lower.contains("acidity")) {
            user.setClinicalNotes("Recent digestion issue: reported " + (lower.contains("acidity") ? "acidity" : "bloating/gas"));
            user.setLastMood("Digestion sensitive");
            updated = true;
        } else if (lower.contains("crav") || lower.contains("sweet") || lower.contains("sugar") || lower.contains("hungry")) {
            user.setClinicalNotes("Experienced sweet/snack cravings: " + text.substring(0, Math.min(60, text.length())));
            updated = true;
        } else if (lower.contains("tired") || lower.contains("exhaust") || lower.contains("low energy") || lower.contains("weak")) {
            user.setLastMood("Low energy / fatigued");
            updated = true;
        } else if (lower.contains("great") || lower.contains("energetic") || lower.contains("fresh") || lower.contains("active")) {
            user.setLastMood("Energetic / Feeling good");
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
            log.info("Updated longitudinal care notes for user {}: notes='{}', mood='{}'", 
                    user.getPhoneNumber(), user.getClinicalNotes(), user.getLastMood());
        }
    }

    private void handleMealImageUpload(User user, MetaWebhookPayload.Message message) {
        String mediaId = message.getImage().getId();
        String mimeType = message.getImage().getMime_type();
        String caption = message.getImage().getCaption();

        log.info("Processing meal photo for user {}. Media ID: {}", user.getPhoneNumber(), mediaId);
        apiClient.sendTextMessage(user.getPhoneNumber(), "📸 *Dr. Aanya is analyzing your meal photo...* Analyzing portion sizes & nutritional macros...");

        byte[] imageBytes = apiClient.downloadMedia(mediaId);
        if (imageBytes == null || imageBytes.length == 0) {
            apiClient.sendTextMessage(user.getPhoneNumber(), "⚠️ Couldn't retrieve the image from WhatsApp. Please ensure image permissions are enabled and try again.");
            return;
        }

        String analysis = geminiNutritionistService.analyzeMealImage(user, imageBytes, mimeType, caption);
        apiClient.sendTextMessage(user.getPhoneNumber(), analysis);
    }

    private void handleVoiceNoteUpload(User user, MetaWebhookPayload.Message message) {
        String mediaId = message.getAudio().getId();
        String mimeType = message.getAudio().getMime_type();

        log.info("Processing voice note for user {}. Media ID: {}, Mime: {}", user.getPhoneNumber(), mediaId, mimeType);
        apiClient.sendTextMessage(user.getPhoneNumber(), "🎙️ *Dr. Aanya is listening to your voice note...* One moment please!");

        byte[] audioBytes = apiClient.downloadMedia(mediaId);
        if (audioBytes == null || audioBytes.length == 0) {
            apiClient.sendTextMessage(user.getPhoneNumber(), "⚠️ Couldn't retrieve the voice note from WhatsApp. Please send it again or type your question.");
            return;
        }

        String voiceResponse = geminiNutritionistService.analyzeVoiceNote(user, audioBytes, mimeType);
        apiClient.sendTextMessage(user.getPhoneNumber(), voiceResponse);
    }

    private void sendWaterTracker(User user) {
        LocalDate today = LocalDate.now();
        var progress = progressRepository.findByUserIdAndLogDate(user.getId(), today)
                .orElseGet(() -> com.yourapp.nutrition.entity.DailyProgress.builder()
                        .userId(user.getId()).logDate(today).waterIntakeMl(0).mealsCompleted(0).build());

        int current = progress.getWaterIntakeMl() != null ? progress.getWaterIntakeMl() : 0;
        int target = 3000;
        int blocks = 10;
        int filled = Math.min(blocks, (int) Math.round(((double) current / target) * blocks));
        String bar = "█".repeat(filled) + "░".repeat(Math.max(0, blocks - filled));

        String body = String.format("💧 *DAILY HYDRATION TRACKER*\n\n" +
                "Progress: [%s] *%d / %d ml* (%d%%)\n\n" +
                "Proper hydration regulates blood pressure, controls insulin sensitivity, and optimizes metabolic rate.\n\n" +
                "Tap below to log your water intake:",
                bar, current, target, (int) (((double) current / target) * 100));

        apiClient.sendButtonMessage(user.getPhoneNumber(), body, List.of(
                ButtonOption.builder().id("WATER_ADD_250").title("💧 +250 ml (1 Cup)").build(),
                ButtonOption.builder().id("WATER_ADD_500").title("🥤 +500 ml (Bottle)").build(),
                ButtonOption.builder().id("WATER_RESET").title("🔄 Reset").build()
        ));
    }

    private void logWaterIntake(User user, int amountMl) {
        LocalDate today = LocalDate.now();
        var progress = progressRepository.findByUserIdAndLogDate(user.getId(), today)
                .orElseGet(() -> com.yourapp.nutrition.entity.DailyProgress.builder()
                        .userId(user.getId()).logDate(today).waterIntakeMl(0).mealsCompleted(0).build());

        int newTotal = (progress.getWaterIntakeMl() != null ? progress.getWaterIntakeMl() : 0) + amountMl;
        progress.setWaterIntakeMl(newTotal);
        progressRepository.save(progress);

        sendWaterTracker(user);
    }

    private void resetWaterIntake(User user) {
        LocalDate today = LocalDate.now();
        var progress = progressRepository.findByUserIdAndLogDate(user.getId(), today)
                .orElseGet(() -> com.yourapp.nutrition.entity.DailyProgress.builder()
                        .userId(user.getId()).logDate(today).waterIntakeMl(0).mealsCompleted(0).build());

        progress.setWaterIntakeMl(0);
        progressRepository.save(progress);

        sendWaterTracker(user);
    }

    private void logMealCompletion(User user) {
        LocalDate today = LocalDate.now();
        var progress = progressRepository.findByUserIdAndLogDate(user.getId(), today)
                .orElseGet(() -> com.yourapp.nutrition.entity.DailyProgress.builder()
                        .userId(user.getId()).logDate(today).waterIntakeMl(0).mealsCompleted(0).build());

        int completed = (progress.getMealsCompleted() != null ? progress.getMealsCompleted() : 0) + 1;
        progress.setMealsCompleted(completed);
        progressRepository.save(progress);

        apiClient.sendTextMessage(user.getPhoneNumber(), 
                "🎉 *Meal Logged!* You've completed " + completed + " meals today.\n" +
                "Consistency is key to transforming your metabolism and clinical health! 🔥\n\n" +
                "💡 Reply *WATER* to log hydration or send a *food photo* 📸 to scan calories!");
    }

    private boolean isFoodIntakeMessage(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().trim();
        return lower.startsWith("i ate ") || lower.startsWith("i had ") || lower.startsWith("ate ") ||
                lower.startsWith("had ") || lower.contains("thinnanu") || lower.contains("khaya");
    }

    private void handleFoodIntakeLog(User user, String text) {
        log.info("Logging food intake for user {}: '{}'", user.getPhoneNumber(), text);
        int[] macros = geminiNutritionistService.estimateNutritionalContent(text);
        int calories = macros[0];
        int protein = macros[1];

        LocalDate today = LocalDate.now();
        var progress = progressRepository.findByUserIdAndLogDate(user.getId(), today)
                .orElseGet(() -> com.yourapp.nutrition.entity.DailyProgress.builder()
                        .userId(user.getId()).logDate(today).waterIntakeMl(0).loggedCalories(0).loggedProtein(0.0).mealsCompleted(0).build());

        int totalCals = (progress.getLoggedCalories() != null ? progress.getLoggedCalories() : 0) + calories;
        double totalProt = (progress.getLoggedProtein() != null ? progress.getLoggedProtein() : 0.0) + protein;
        int completed = (progress.getMealsCompleted() != null ? progress.getMealsCompleted() : 0) + 1;

        progress.setLoggedCalories(totalCals);
        progress.setLoggedProtein(totalProt);
        progress.setMealsCompleted(completed);
        progressRepository.save(progress);

        int targetCals = (user.getWeight() != null ? (int)(user.getWeight() * 28) : 2000);
        int blocks = 10;
        int filled = Math.min(blocks, (int) Math.round(((double) totalCals / targetCals) * blocks));
        String bar = "█".repeat(filled) + "░".repeat(Math.max(0, blocks - filled));

        String response = String.format(
                "📝 *Logged to Today's Nutrition Tracker!*\n\n" +
                "🍽️ *Intake:* %s\n" +
                "🔥 *Added:* +%d kcal  |  💪 *Protein:* +%d g\n\n" +
                "📊 *Today's Total:* [%s] *%d / %d kcal* (%.0f%%)\n" +
                "🍗 *Total Protein:* %.0f g\n\n" +
                "💡 *Tip:* Reply *WATER* to check hydration, or send a *food photo* 📸 anytime!",
                text, calories, protein, bar, totalCals, targetCals, ((double) totalCals / targetCals) * 100, totalProt
        );

        apiClient.sendTextMessage(user.getPhoneNumber(), response);
    }

    private void sendGoalPrompt(String phoneNumber) {
        apiClient.sendButtonMessage(phoneNumber,
                "👋 Welcome to *Nutrition Engine*!\n\nLet's build your personalized nutrition plan. What is your primary fitness goal?",
                List.of(
                        ButtonOption.builder().id("GOAL_FAT_LOSS").title("Fat Loss 🔥").build(),
                        ButtonOption.builder().id("GOAL_MAINTENANCE").title("Maintain ⚖️").build(),
                        ButtonOption.builder().id("GOAL_MUSCLE_GAIN").title("Muscle Gain 💪").build()
                )
        );
    }

    private String extractButtonId(MetaWebhookPayload.Message message) {
        if ("interactive".equalsIgnoreCase(message.getType()) && message.getInteractive() != null) {
            if (message.getInteractive().getButtonReply() != null) {
                return message.getInteractive().getButtonReply().getId();
            }
        }
        return null;
    }

    private String extractInteractiveId(MetaWebhookPayload.Message message) {
        if ("interactive".equalsIgnoreCase(message.getType()) && message.getInteractive() != null) {
            if (message.getInteractive().getListReply() != null) {
                return message.getInteractive().getListReply().getId();
            } else if (message.getInteractive().getButtonReply() != null) {
                return message.getInteractive().getButtonReply().getId();
            }
        }
        return null;
    }

    private String extractText(MetaWebhookPayload.Message message) {
        if ("text".equalsIgnoreCase(message.getType()) && message.getText() != null) {
            return message.getText().getBody();
        }
        return null;
    }

    private User createUser(String phoneNumber, String name) {
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .name(name != null ? name : "User")
                .active(true)
                .build();
        return userRepository.save(user);
    }

    private ConversationState createInitialState(Long userId, String phoneNumber) {
        ConversationState state = ConversationState.builder()
                .userId(userId)
                .phoneNumber(phoneNumber)
                .state("ONBOARDING")
                .currentStep("START")
                .build();
        return stateRepository.save(state);
    }
}
