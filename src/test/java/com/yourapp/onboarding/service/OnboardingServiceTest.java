package com.yourapp.onboarding.service;

import com.yourapp.onboarding.entity.ConversationState;
import com.yourapp.onboarding.repository.ConversationStateRepository;
import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import com.yourapp.whatsapp.client.WhatsAppApiClient;
import com.yourapp.whatsapp.dto.MetaWebhookPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
class OnboardingServiceTest {

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationStateRepository stateRepository;

    @MockitoBean
    private WhatsAppApiClient apiClient;

    private final String testPhone = "919988776655";

    @BeforeEach
    void setUp() {
        stateRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Step 1: First message should create user, set state to AWAITING_GOAL, and send goal buttons")
    void testFirstMessage_SendsGoalPrompt() {
        MetaWebhookPayload.Message message = new MetaWebhookPayload.Message();
        message.setType("text");
        var text = new MetaWebhookPayload.Text();
        text.setBody("Hi");
        message.setText(text);

        onboardingService.handleIncomingMessage(testPhone, message, "Suresh");

        Optional<User> userOpt = userRepository.findByPhoneNumber(testPhone);
        assertTrue(userOpt.isPresent());
        assertEquals("Suresh", userOpt.get().getName());

        Optional<ConversationState> stateOpt = stateRepository.findByPhoneNumber(testPhone);
        assertTrue(stateOpt.isPresent());
        assertEquals("AWAITING_GOAL", stateOpt.get().getCurrentStep());

        verify(apiClient).sendButtonMessage(eq(testPhone), any(), any());
    }

    @Test
    @DisplayName("Step 2: Selecting Goal button should advance state to AWAITING_SEX and save goal")
    void testGoalSelection_AdvancesToSex() {
        // Setup initial user & state
        User user = userRepository.save(User.builder().phoneNumber(testPhone).name("Suresh").active(true).build());
        stateRepository.save(ConversationState.builder().userId(user.getId()).phoneNumber(testPhone).state("ONBOARDING").currentStep("AWAITING_GOAL").build());

        MetaWebhookPayload.Message message = new MetaWebhookPayload.Message();
        message.setType("interactive");
        var interactive = new MetaWebhookPayload.Interactive();
        var buttonReply = new MetaWebhookPayload.ButtonReply();
        buttonReply.setId("GOAL_FAT_LOSS");
        buttonReply.setTitle("Fat Loss 🔥");
        interactive.setButtonReply(buttonReply);
        message.setInteractive(interactive);

        onboardingService.handleIncomingMessage(testPhone, message, "Suresh");

        User updatedUser = userRepository.findByPhoneNumber(testPhone).orElseThrow();
        assertEquals("FAT_LOSS", updatedUser.getGoal());

        ConversationState updatedState = stateRepository.findByPhoneNumber(testPhone).orElseThrow();
        assertEquals("AWAITING_SEX", updatedState.getCurrentStep());
    }

    @Test
    @DisplayName("Step 3: Replying with Vitals ('28, 178cm, 75kg') should parse numbers and advance to AWAITING_DIET")
    void testVitalsParsing_Success() {
        User user = userRepository.save(User.builder().phoneNumber(testPhone).name("Suresh").goal("FAT_LOSS").sex("MALE").active(true).build());
        stateRepository.save(ConversationState.builder().userId(user.getId()).phoneNumber(testPhone).state("ONBOARDING").currentStep("AWAITING_VITALS").build());

        MetaWebhookPayload.Message message = new MetaWebhookPayload.Message();
        message.setType("text");
        var text = new MetaWebhookPayload.Text();
        text.setBody("28, 178cm, 75kg");
        message.setText(text);

        onboardingService.handleIncomingMessage(testPhone, message, "Suresh");

        User updatedUser = userRepository.findByPhoneNumber(testPhone).orElseThrow();
        assertEquals(28, updatedUser.getAge());
        assertEquals(178.0, updatedUser.getHeight());
        assertEquals(75.0, updatedUser.getWeight());

        ConversationState updatedState = stateRepository.findByPhoneNumber(testPhone).orElseThrow();
        assertEquals("AWAITING_DIET", updatedState.getCurrentStep());
    }

    @Test
    @DisplayName("Step 4: Completing all steps transitions state to ACTIVE")
    void testFinalStep_TransitionsToActive() {
        User user = userRepository.save(User.builder()
                .phoneNumber(testPhone)
                .name("Suresh")
                .goal("FAT_LOSS")
                .sex("MALE")
                .age(28)
                .height(178.0)
                .weight(75.0)
                .dietType("VEG")
                .cuisine("NORTH_INDIAN")
                .active(true)
                .build());

        stateRepository.save(ConversationState.builder()
                .userId(user.getId())
                .phoneNumber(testPhone)
                .state("ONBOARDING")
                .currentStep("AWAITING_CONDITIONS")
                .build());

        MetaWebhookPayload.Message message = new MetaWebhookPayload.Message();
        message.setType("interactive");
        var interactive = new MetaWebhookPayload.Interactive();
        var buttonReply = new MetaWebhookPayload.ButtonReply();
        buttonReply.setId("COND_NONE");
        buttonReply.setTitle("None / Healthy ✅");
        interactive.setButtonReply(buttonReply);
        message.setInteractive(interactive);

        onboardingService.handleIncomingMessage(testPhone, message, "Suresh");

        ConversationState step7State = stateRepository.findByPhoneNumber(testPhone).orElseThrow();
        assertEquals("AWAITING_ENERGY_SLEEP", step7State.getCurrentStep());

        // Step 7: Select Energy
        MetaWebhookPayload.Message msgEnergy = new MetaWebhookPayload.Message();
        msgEnergy.setType("interactive");
        var interactiveEnergy = new MetaWebhookPayload.Interactive();
        var btnEnergy = new MetaWebhookPayload.ButtonReply();
        btnEnergy.setId("ENERGY_HIGH");
        interactiveEnergy.setButtonReply(btnEnergy);
        msgEnergy.setInteractive(interactiveEnergy);

        onboardingService.handleIncomingMessage(testPhone, msgEnergy, "Suresh");

        ConversationState step8State = stateRepository.findByPhoneNumber(testPhone).orElseThrow();
        assertEquals("AWAITING_GUT_HEALTH", step8State.getCurrentStep());

        // Step 8: Select Gut Health
        MetaWebhookPayload.Message msgGut = new MetaWebhookPayload.Message();
        msgGut.setType("interactive");
        var interactiveGut = new MetaWebhookPayload.Interactive();
        var btnGut = new MetaWebhookPayload.ButtonReply();
        btnGut.setId("GUT_SMOOTH");
        interactiveGut.setButtonReply(btnGut);
        msgGut.setInteractive(interactiveGut);

        onboardingService.handleIncomingMessage(testPhone, msgGut, "Suresh");

        ConversationState finalState = stateRepository.findByPhoneNumber(testPhone).orElseThrow();
        assertEquals("ACTIVE", finalState.getState());
        assertEquals("COMPLETE", finalState.getCurrentStep());
    }

    @Test
    @DisplayName("Active User: Greeting ('Hi') should deliver Feature Showcase Card")
    void testActiveUserGreeting_DeliversShowcase() {
        User user = userRepository.save(User.builder().phoneNumber(testPhone).name("Suresh").active(true).build());
        stateRepository.save(ConversationState.builder().userId(user.getId()).phoneNumber(testPhone).state("ACTIVE").currentStep("COMPLETE").build());

        MetaWebhookPayload.Message message = new MetaWebhookPayload.Message();
        message.setType("text");
        var text = new MetaWebhookPayload.Text();
        text.setBody("Hello Coach Mohan!");
        message.setText(text);

        onboardingService.handleIncomingMessage(testPhone, message, "Suresh");

        verify(apiClient).sendButtonMessage(eq(testPhone), any(), any());
    }
}
