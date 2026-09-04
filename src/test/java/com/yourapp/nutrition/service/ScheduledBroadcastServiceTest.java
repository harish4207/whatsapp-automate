package com.yourapp.nutrition.service;

import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import com.yourapp.whatsapp.client.WhatsAppApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ScheduledBroadcastServiceTest {

    @Autowired
    private ScheduledBroadcastService broadcastService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private WhatsAppApiClient apiClient;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // 1. Fully onboarded active user
        userRepository.save(User.builder()
                .phoneNumber("919988776655")
                .name("Arun")
                .age(30)
                .sex("MALE")
                .height(175.0)
                .weight(72.0)
                .goal("FAT_LOSS")
                .dietType("VEG")
                .cuisine("SOUTH_INDIAN")
                .healthCondition("DIABETES")
                .active(true)
                .build());

        // 2. Incomplete onboarding user (should be skipped)
        userRepository.save(User.builder()
                .phoneNumber("918877665544")
                .name("Incomplete")
                .active(true)
                .build());
    }

    @Test
    @DisplayName("Should deliver morning broadcast to fully onboarded active users only")
    void testMorningBroadcast() {
        int deliveredCount = broadcastService.triggerMorningBroadcast();

        assertEquals(1, deliveredCount, "Should only deliver to 1 fully onboarded active subscriber");
        verify(apiClient, atLeastOnce()).sendButtonMessage(eq("919988776655"), any(), any());
    }
}
