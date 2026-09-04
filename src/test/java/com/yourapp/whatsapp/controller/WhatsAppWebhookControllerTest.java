package com.yourapp.whatsapp.controller;

import com.yourapp.whatsapp.repository.MessageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class WhatsAppWebhookControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MessageLogRepository messageLogRepository;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
        messageLogRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /webhook with valid token should return hub.challenge and 200 OK")
    void testWebhookVerification_Success() throws Exception {
        mockMvc.perform(get("/webhook")
                .param("hub.mode", "subscribe")
                .param("hub.verify_token", "whatsapp_nutrition_secret_2026")
                .param("hub.challenge", "challenge_123456"))
                .andExpect(status().isOk())
                .andExpect(content().string("challenge_123456"));
    }

    @Test
    @DisplayName("GET /webhook with invalid token should return 403 Forbidden")
    void testWebhookVerification_InvalidToken() throws Exception {
        mockMvc.perform(get("/webhook")
                .param("hub.mode", "subscribe")
                .param("hub.verify_token", "wrong_token")
                .param("hub.challenge", "challenge_123456"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Verification token mismatch"));
    }

    @Test
    @DisplayName("POST /webhook with valid message payload should return 200 OK and persist message log")
    void testIncomingMessage_Success() throws Exception {
        String testMessageId = "wamid.TEST_ID_9999";
        String sampleJson = """
            {
              "object": "whatsapp_business_account",
              "entry": [
                {
                  "id": "BIZ_123",
                  "changes": [
                    {
                      "field": "messages",
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": {
                          "display_phone_number": "15551234567",
                          "phone_number_id": "10001"
                        },
                        "contacts": [
                          {
                            "profile": { "name": "Ravi" },
                            "wa_id": "919988776655"
                          }
                        ],
                        "messages": [
                          {
                            "from": "919988776655",
                            "id": "%s",
                            "timestamp": "1725355000",
                            "type": "text",
                            "text": { "body": "Hi, I want a meal plan" }
                          }
                        ]
                      }
                    }
                  ]
                }
              ]
            }
            """.formatted(testMessageId);

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleJson.getBytes()))
                .andExpect(status().isOk())
                .andExpect(content().string("EVENT_RECEIVED"));

        // Wait a few milliseconds for async worker to complete
        Thread.sleep(300);

        // Verify that the message was saved in message_logs table
        assertTrue(messageLogRepository.existsByMessageId(testMessageId));
    }

    @Test
    @DisplayName("POST /webhook duplicate message should be safely ignored (Idempotency)")
    void testIncomingMessage_Idempotency() throws Exception {
        String duplicateMsgId = "wamid.DUPLICATE_123";
        String sampleJson = """
            {
              "object": "whatsapp_business_account",
              "entry": [
                {
                  "id": "BIZ_123",
                  "changes": [
                    {
                      "field": "messages",
                      "value": {
                        "messaging_product": "whatsapp",
                        "messages": [
                          {
                            "from": "919988776655",
                            "id": "%s",
                            "timestamp": "1725355000",
                            "type": "text",
                            "text": { "body": "Hi again" }
                          }
                        ]
                      }
                    }
                  ]
                }
              ]
            }
            """.formatted(duplicateMsgId);

        // First delivery
        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleJson.getBytes()))
                .andExpect(status().isOk());

        Thread.sleep(200);
        assertEquals(1, messageLogRepository.count());

        // Second delivery of exact same message_id (Meta retry simulation)
        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sampleJson.getBytes()))
                .andExpect(status().isOk());

        Thread.sleep(200);
        // Count should STILL be 1, not 2
        assertEquals(1, messageLogRepository.count());
    }
}
