package com.yourapp.ai;

import com.yourapp.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * GeminiNutritionistService provides personalized, clinically-aware conversational AI
 * guidance powered by Google Gemini 3.5 Flash-Lite.
 */
@Service
@Slf4j
public class GeminiNutritionistService {

    private final RestClient restClient;
    private final String apiKey;
    private final String secondaryApiKey;
    private final String apiUrl;
    private final String grokApiKey;
    private final String grokApiUrl;
    private final String nvidiaApiKey;
    private final String nvidiaApiUrl;
    private final String nvidiaModel;
    private final boolean isEnabled;

    public GeminiNutritionistService(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.api.key.secondary:}") String secondaryApiKey,
            @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent}") String apiUrl,
            @Value("${grok.api.key:}") String grokApiKey,
            @Value("${grok.api.url:https://api.x.ai/v1/chat/completions}") String grokApiUrl,
            @Value("${nvidia.nim.api.key:}") String nvidiaApiKey,
            @Value("${nvidia.nim.api.url:https://integrate.api.nvidia.com/v1/chat/completions}") String nvidiaApiUrl,
            @Value("${nvidia.nim.model:deepseek-ai/deepseek-v4-pro-0813}") String nvidiaModel) {

        this.apiKey = apiKey;
        this.secondaryApiKey = secondaryApiKey;
        this.apiUrl = apiUrl;
        this.grokApiKey = grokApiKey;
        this.grokApiUrl = grokApiUrl;
        this.nvidiaApiKey = nvidiaApiKey;
        this.nvidiaApiUrl = nvidiaApiUrl;
        this.nvidiaModel = nvidiaModel;
        this.isEnabled = (apiKey != null && !apiKey.trim().isEmpty()) 
                || (secondaryApiKey != null && !secondaryApiKey.trim().isEmpty())
                || (nvidiaApiKey != null && !nvidiaApiKey.trim().isEmpty());
        this.restClient = RestClient.builder().build();

        if (isEnabled) {
            log.info("GeminiNutritionistService initialized with Primary + Secondary Gemini Failover & NVIDIA NIM (DeepSeek-v4-pro) backup.");
        } else {
            log.warn("No AI API keys configured. Running GeminiNutritionistService in mock mode.");
        }
    }

    /**
     * Answers any free-form user query with full awareness of their clinical profile and targets.
     */
    public String askNutritionist(User user, String userQuery) {
        if (!isEnabled) {
            return getMockResponse(user, userQuery);
        }

        String systemInstruction = buildSystemInstruction(user);
        Map<String, Object> requestPayload = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", systemInstruction + "\n\nUser Question: " + userQuery)
                                )
                        )
                )
        );

        // 1. Try Primary + Secondary Gemini Flash
        String answer = executeWithFailover(requestPayload);
        if (answer != null) {
            return answer;
        }

        // 2. High-Performance NVIDIA NIM Failover (DeepSeek-v4-pro)
        if (nvidiaApiKey != null && !nvidiaApiKey.isBlank()) {
            log.info("Gemini unavailable. Failing over to NVIDIA NIM ({}) for user {}...", nvidiaModel, user.getPhoneNumber());
            String nvidiaAnswer = callNvidiaNim(systemInstruction, userQuery);
            if (nvidiaAnswer != null) {
                return nvidiaAnswer;
            }
        }

        // 3. Optional Grok text fallback if Grok API key is configured
        if (grokApiKey != null && !grokApiKey.isBlank()) {
            String grokAnswer = callGrok(systemInstruction, userQuery);
            if (grokAnswer != null) {
                return grokAnswer;
            }
        }

        return "🩺 *Dr. Aanya*: I'm receiving very high traffic right now, but I'm right here with you! Please text *PLAN* to view today's meals, or try your question again in a moment.";
    }

    /**
     * Multimodal Image Analysis: Takes a photo of the user's food plate, identifies dishes,
     * estimates calories and macronutrients, and verifies clinical safety.
     */
    public String analyzeMealImage(User user, byte[] imageBytes, String mimeType, String userCaption) {
        if (!isEnabled || imageBytes == null || imageBytes.length == 0) {
            return "📸 *Meal Analysis*:\n\nLooks delicious! Make sure to prioritize lean protein and greens aligned with your *" +
                    (user.getGoal() != null ? user.getGoal() : "Health") + "* plan.";
        }

        try {
            String base64Data = java.util.Base64.getEncoder().encodeToString(imageBytes);
            String actualMime = (mimeType != null && !mimeType.isBlank()) ? mimeType : "image/jpeg";

            String prompt = """
                You are Dr. Aanya, an expert Indian Clinical Nutritionist analyzing this meal photo sent by your patient.
                
                Patient Clinical Profile:
                - Goal: %s
                - Health Condition: %s
                - Dietary Preference: %s
                - Daily Calorie Target: ~%d kcal
                
                Please provide a structured, engaging analysis for WhatsApp:
                1. 🍽️ *Identified Items & Portion Estimate*: Name the dishes and approximate quantity.
                2. 📊 *Estimated Nutrition*:
                   - 🔥 Calories: ~X kcal
                   - 💪 Protein: ~Xg | 🌾 Carbs: ~Xg | 🥑 Fats: ~Xg
                3. 🛡️ *Clinical Safety Verdict*:
                   - State whether this is recommended, neutral, or needs caution for their %s condition (e.g. oil/salt/glycemic index).
                4. 💡 *Actionable Pro-Tip*: One simple tweak to make this plate even healthier.
                
                Keep your total response under 150 words, friendly, and formatted cleanly with WhatsApp bolding and emojis.
                User note: %s
                """.formatted(
                    user.getGoal() != null ? user.getGoal() : "Health & Vitality",
                    user.getHealthCondition() != null ? user.getHealthCondition() : "None",
                    user.getDietType() != null ? user.getDietType() : "General",
                    user.getWeight() != null ? (int)(user.getWeight() * 28) : 2000,
                    user.getHealthCondition() != null ? user.getHealthCondition() : "General Wellness",
                    userCaption != null && !userCaption.isBlank() ? userCaption : "What is on my plate?"
            );

            Map<String, Object> requestPayload = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(
                                            Map.of("text", prompt),
                                            Map.of("inline_data", Map.of(
                                                    "mime_type", actualMime,
                                                    "data", base64Data
                                            ))
                                    )
                            )
                    )
            );

            String answer = executeWithFailover(requestPayload);
            if (answer != null) {
                return answer;
            }

            return "📸 *Meal Scanner*: Plate analyzed! Prioritize 1/2 plate fresh veggies and 1/4 plate protein aligned with your health goal. Reply *PLAN* anytime!";

        } catch (Exception e) {
            log.error("Failed to analyze meal image with Gemini: {}", e.getMessage(), e);
            return "📸 *Meal Scanner*: I received your food image! Due to a temporary AI connection timeout, please note that healthy Indian meals with 1/2 plate vegetables, 1/4 plate protein, and 1/4 complex carbs fit best into your plan. Reply *PLAN* anytime!";
        }
    }

    /**
     * Multimodal Audio Analysis: Processes voice notes (WhatsApp OGG/AAC/MP3),
     * transcribes the user's spoken question in Telugu/Hindi/English, and provides
     * clinical nutritionist answers.
     */
    public String analyzeVoiceNote(User user, byte[] audioBytes, String mimeType) {
        if (!isEnabled || audioBytes == null || audioBytes.length == 0) {
            return "🎙️ *Voice Note Received*: I heard your voice message! Text *PLAN* anytime for today's meals.";
        }

        try {
            String base64Data = java.util.Base64.getEncoder().encodeToString(audioBytes);
            // WhatsApp voice notes are typically "audio/ogg; codecs=opus" or "audio/ogg"
            String actualMime = (mimeType != null && mimeType.contains("ogg")) ? "audio/ogg" : (mimeType != null ? mimeType : "audio/ogg");

            String systemInstruction = buildSystemInstruction(user);
            String prompt = """
                The user has sent a WhatsApp voice message.
                
                Please:
                1. Listen to what the user said in the audio clip (it could be in English, Telugu, Hindi, or Hinglish).
                2. Briefly quote or summarize what they asked: "🎙️ *I heard you ask:* \"...\""
                3. Provide your expert clinical nutrition response, keeping your advice personalized to their health condition (%s).
                4. If the user asked in Telugu, respond in Telugu. If in Hindi, respond in Hindi. If in English, respond in English.
                5. Keep your total response under 150 words.
                """.formatted(user.getHealthCondition() != null ? user.getHealthCondition() : "General Fitness");

            Map<String, Object> requestPayload = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(
                                            Map.of("text", systemInstruction + "\n\n" + prompt),
                                            Map.of("inline_data", Map.of(
                                                    "mime_type", actualMime,
                                                    "data", base64Data
                                            ))
                                    )
                            )
                    )
            );

            String answer = executeWithFailover(requestPayload);
            if (answer != null) {
                return answer;
            }

            return "🎙️ *Dr. Aanya*: I listened to your voice message! Keep your meals balanced and rich in fiber. Text *PLAN* anytime for today's meals.";

        } catch (Exception e) {
            log.error("Failed to analyze voice note with Gemini: {}", e.getMessage(), e);
            return "🎙️ *Voice Note*: I received your audio note, but had trouble deciphering the sound file. Could you please send it again or type your question?";
        }
    }

    /**
     * Generates a quick 4-step condition-safe home cooking recipe.
     */
    public String generateRecipe(User user, String mealName) {
        String prompt = "Give a simple 4-step healthy Indian cooking recipe for '" + mealName +
                "' suitable for a patient with " + (user.getHealthCondition() != null ? user.getHealthCondition() : "general fitness") +
                ". Keep oil and salt minimal. Max 120 words with steps numbered 1 to 4.";
        return askNutritionist(user, prompt);
    }

    /**
     * Estimates calories and protein from spoken or typed food intake (e.g. "I ate 2 dosas and sambar").
     * Returns: int[] {calories, protein}
     */
    public int[] estimateNutritionalContent(String foodText) {
        String prompt = "You are a nutritional database. For this Indian food item: '" + foodText +
                "', reply ONLY with two numbers separated by a comma: estimated_calories,estimated_protein_in_grams. " +
                "Example response format: 350,12. Do not write any other words or letters.";
        try {
            Map<String, Object> requestPayload = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", prompt))
                            )
                    )
            );
            String raw = executeWithFailover(requestPayload);
            if (raw != null) {
                String clean = raw.replaceAll("[^0-9,.]", "").trim();
                String[] parts = clean.split(",");
                if (parts.length >= 2) {
                    int cals = (int) Math.round(Double.parseDouble(parts[0].trim()));
                    int prot = (int) Math.round(Double.parseDouble(parts[1].trim()));
                    return new int[]{cals, prot};
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse nutrition numbers: {}", e.getMessage());
        }
        return new int[]{300, 10}; // Safe default estimation
    }

    private String buildSystemInstruction(User user) {
        String name = user.getName() != null ? user.getName() : "Friend";
        String goal = user.getGoal() != null ? user.getGoal().replace("_", " ") : "GENERAL HEALTH";
        String condition = user.getHealthCondition() != null ? user.getHealthCondition() : "NONE";
        String diet = user.getDietType() != null ? user.getDietType().replace("_", " ") : "ANY";
        String cuisine = user.getCuisine() != null ? user.getCuisine().replace("_", " ") : "INDIAN";
        String memoryNotes = user.getClinicalNotes() != null ? user.getClinicalNotes() : "None yet";
        String lastMood = user.getLastMood() != null ? user.getLastMood() : "Normal";
        int streak = user.getStreakDays() != null ? user.getStreakDays() : 1;

        return """
            You are Dr. Aanya, a dedicated, deeply compassionate, and clinically certified personal Nutritionist & Dietitian.
            You are NOT a detached, cold bot or an generic AI search engine. You speak as a caring, attentive personal doctor who knows the patient personally.
            
            PATIENT PERSONAL CLINICAL PROFILE:
            - Name: %s
            - Age: %s | Sex: %s | Height: %s cm | Weight: %s kg
            - Primary Fitness Goal: %s
            - Dietary Preference: %s | Preferred Cuisine: %s
            - Medical Condition: %s
            - Ongoing Care Notes / Personal Struggles: %s
            - Last Reported Mood / Sensation: %s
            - Active Consistency Streak: %d days
            
            CLINICAL CONCERN & EMPATHY GUIDELINES:
            1. EMPATHY & EMOTIONAL REASSURANCE:
               - Greet them warmly and acknowledge their personal situation.
               - If they report having a cheat meal, binge eating, or snacking on sweets/fried food, NEVER scold or induce guilt. Validate that life happens, congratulate them on being honest, and gently advise how to balance their blood sugar/gut for the next meal (e.g. 15-min stroll, warm water with lemon/jeera, extra fiber).
            2. LONGITUDINAL MEMORY:
               - If they previously struggled with acidity, bloating, late-night sweet cravings, or work fatigue, proactively inquire about how their body is feeling today.
            3. CRITICAL RED FLAG PROTOCOL:
               - If the patient reports severe emergency symptoms (chest tightness, extreme hypoglycemia shakes < 60 mg/dL, dizziness/fainting, vomiting blood, breathlessness):
               - IMMEDIATELY state clearly with 🚨 that their health and safety comes first, instruct them to sit down, drink glucose/water if diabetic, and urgently contact their local doctor or emergency services.
            4. NON-NEGOTIABLE CLINICAL SAFETY RULES:
               - DIABETES: Strictly avoid simple sugars, jaggery, honey, maida, white potatoes, and high-GI fruit juices. Prioritize fiber-rich lentils, methi, millets, and portion balance.
               - HYPERTENSION: Keep sodium minimal. Advise rinsing or avoiding pickles/papads/chips. Promote potassium-rich greens & coconut water.
               - THYROID: Ensure selenium & zinc (eggs, pumpkin seeds, soaked almonds). Strictly avoid raw cruciferous vegetables (raw cabbage/broccoli) and unfermented soy.
               - PCOS: Emphasize anti-inflammatory meals, spearmint tea, cinnamon, and low glycemic index foods.
               - FATTY LIVER: Avoid saturated fats and refined sugars. Recommend choline, cruciferous veggies, and liver-friendly turmeric/black pepper.
            
            WHATSAPP TONE & READABILITY:
            - Maximum 140 words so it is effortless to read on a mobile screen.
            - Use friendly emojis (🩺, 💚, 🌿, ☀️, 💧) and clean WhatsApp bolding.
            - If user speaks in Telugu, Hindi, or Hinglish, answer warmly with natural phrasing in that language.
            """.formatted(
                name,
                user.getAge() != null ? user.getAge() : "Adult",
                user.getSex() != null ? user.getSex() : "Not specified",
                user.getHeight() != null ? user.getHeight() : 170.0,
                user.getWeight() != null ? user.getWeight() : 70.0,
                goal,
                diet,
                cuisine,
                condition,
                memoryNotes,
                lastMood,
                streak
        );
    }

    @SuppressWarnings("unchecked")
    private String extractResponseText(Map response) {
        if (response == null) return "I couldn't process your request. Please ask again!";

        try {
            List candidates = (List) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map firstCandidate = (Map) candidates.get(0);
                Map content = (Map) firstCandidate.get("content");
                if (content != null) {
                    List parts = (List) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        Map firstPart = (Map) parts.get(0);
                        String text = (String) firstPart.get("text");
                        if (text != null && !text.isBlank()) {
                            return text.trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing Gemini response structure: {}", e.getMessage());
        }

        return "I couldn't generate a nutritional answer at the moment. Please text *PLAN* to view your daily blueprint.";
    }

    private String executeWithFailover(Map<String, Object> requestPayload) {
        // Try Primary Key first
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String requestUrl = apiUrl + "?key=" + apiKey;
                Map response = restClient.post()
                        .uri(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestPayload)
                        .retrieve()
                        .body(Map.class);

                String text = extractResponseText(response);
                if (text != null && !text.isBlank()) return text;
            } catch (Exception e) {
                log.warn("Primary Gemini Key encountered error: {}. Failing over to secondary key...", e.getMessage());
            }
        }

        // Failover to Secondary Key
        if (secondaryApiKey != null && !secondaryApiKey.isBlank()) {
            try {
                String requestUrl = apiUrl + "?key=" + secondaryApiKey;
                Map response = restClient.post()
                        .uri(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestPayload)
                        .retrieve()
                        .body(Map.class);

                String text = extractResponseText(response);
                if (text != null && !text.isBlank()) return text;
            } catch (Exception e) {
                log.error("Secondary Gemini Key encountered error: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * NVIDIA NIM High-Performance LLM Failover.
     * Uses OpenAI-compatible chat completions with DeepSeek-v4-pro or Llama-3.2.
     */
    private String callNvidiaNim(String systemInstruction, String userQuery) {
        try {
            log.info("Dispatching query to NVIDIA NIM ({}) via OpenAI-compatible endpoint...", nvidiaModel);
            Map<String, Object> payload = Map.of(
                    "model", nvidiaModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemInstruction),
                            Map.of("role", "user", "content", userQuery)
                    ),
                    "temperature", 0.6,
                    "max_tokens", 500
            );

            Map response = restClient.post()
                    .uri(nvidiaApiUrl)
                    .header("Authorization", "Bearer " + nvidiaApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("choices")) {
                List choices = (List) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map msg = (Map) firstChoice.get("message");
                    if (msg != null && msg.containsKey("content")) {
                        String content = (String) msg.get("content");
                        if (content != null && !content.isBlank()) {
                            log.info("NVIDIA NIM successfully answered query for user!");
                            return content.trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("NVIDIA NIM API call failed: {}", e.getMessage());
        }
        return null;
    }

    private String callGrok(String systemInstruction, String userQuery) {
        try {
            log.info("Dispatching query to xAI Grok fallback API...");
            Map<String, Object> grokPayload = Map.of(
                    "model", "grok-beta",
                    "messages", List.of(
                            Map.of("role", "system", "content", systemInstruction),
                            Map.of("role", "user", "content", userQuery)
                    ),
                    "temperature", 0.7
            );

            Map response = restClient.post()
                    .uri(grokApiUrl)
                    .header("Authorization", "Bearer " + grokApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(grokPayload)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("choices")) {
                List choices = (List) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map msg = (Map) firstChoice.get("message");
                    if (msg != null && msg.containsKey("content")) {
                        return ((String) msg.get("content")).trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("xAI Grok API call failed: {}", e.getMessage());
        }
        return null;
    }

    private String getMockResponse(User user, String query) {
        return "🩺 *Dr. Aanya (AI Nutritionist)*:\n\n" +
                "That's a great question regarding: \"" + query + "\"!\n\n" +
                "• For your *" + (user.getGoal() != null ? user.getGoal() : "Health") + "* goal, prioritize balanced protein and fresh fiber.\n" +
                "• Health Shield: Keep your *" + (user.getHealthCondition() != null ? user.getHealthCondition() : "General Fitness") + "* safe with clean home-cooked portions.\n\n" +
                "💡 *Quick Tip*: Drink 500ml of water before your meals to optimize digestion!";
    }
}
