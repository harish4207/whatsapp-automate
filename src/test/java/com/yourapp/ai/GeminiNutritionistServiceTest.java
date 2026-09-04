package com.yourapp.ai;

import com.yourapp.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiNutritionistServiceTest {

    @Test
    @DisplayName("Should return mock response gracefully when API key is unconfigured")
    void testMockResponseWhenDisabled() {
        GeminiNutritionistService service = new GeminiNutritionistService(
                "", // empty primary key
                "", // empty secondary key
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent",
                "", // empty grok key
                "https://api.x.ai/v1/chat/completions"
        );

        User user = User.builder()
                .name("Harish")
                .goal("FAT_LOSS")
                .healthCondition("DIABETES")
                .build();

        String answer = service.askNutritionist(user, "Can I eat biryani?");

        assertNotNull(answer);
        assertTrue(answer.contains("Dr. Aanya"));
        assertTrue(answer.contains("FAT_LOSS"));
    }
}
