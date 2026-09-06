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
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent",
                "", // empty grok key
                "https://api.x.ai/v1/chat/completions",
                "", // empty nvidia key
                "https://integrate.api.nvidia.com/v1/chat/completions",
                "deepseek-ai/deepseek-v4-pro-0813"
        );

        User user = User.builder()
                .name("Harish")
                .goal("FAT_LOSS")
                .healthCondition("DIABETES")
                .build();

        String answer = service.askNutritionist(user, "Can I eat biryani?");

        assertNotNull(answer);
        assertTrue(answer.contains("Coach Mohan"));
        assertTrue(answer.contains("FAT_LOSS"));
    }

    @Test
    @DisplayName("Verify NVIDIA NIM key live connection")
    void testNvidiaNimLive() {
        GeminiNutritionistService service = new GeminiNutritionistService(
                "",
                "",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent",
                "",
                "https://api.x.ai/v1/chat/completions",
                "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",
                "https://integrate.api.nvidia.com/v1/chat/completions",
                "meta/llama-3.2-11b-vision-instruct"
        );

        int[] macros = service.estimateNutritionalContent("2 idlis with sambar");
        System.out.println("NVIDIA NIM TEST RESULT: Calories=" + macros[0] + ", Protein=" + macros[1]);
        assertTrue(macros[0] > 0, "Calories should be greater than 0");
    }
}
