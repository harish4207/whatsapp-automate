package com.yourapp.nutrition.service;

import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * ScheduledBroadcastService proactively pushes personalized daily meal plans
 * to all active WhatsApp subscribers every morning at 7:30 AM IST.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledBroadcastService {

    private final UserRepository userRepository;
    private final DailyPlanService dailyPlanService;

    /**
     * Executes every morning at 07:30:00 AM Indian Standard Time (IST).
     */
    @Scheduled(cron = "0 30 7 * * *", zone = "Asia/Kolkata")
    public void executeMorningBroadcast() {
        log.info("Starting automated morning nutrition plan broadcast (7:30 AM IST)...");
        triggerMorningBroadcast();
    }

    /**
     * Public method to dispatch the daily plan to all active users.
     * Can be invoked by scheduler or manual admin trigger.
     */
    public int triggerMorningBroadcast() {
        List<User> activeUsers = userRepository.findByActiveTrue();
        log.info("Found {} active subscribers for daily broadcast.", activeUsers.size());

        int count = 0;
        LocalDate today = LocalDate.now();

        for (User user : activeUsers) {
            // Only send to users who have completed onboarding (age and height are set)
            if (user.getAge() == null || user.getHeight() == null || user.getWeight() == null) {
                continue;
            }

            try {
                log.info("Dispatching daily plan to subscriber: {}", user.getPhoneNumber());
                dailyPlanService.generateDailyPlan(user, today);
                count++;

                // Rate limiting pause (50ms) to respect Meta Cloud API message per second (MPS) limits
                Thread.sleep(50);
            } catch (Exception e) {
                log.error("Failed to deliver morning plan to subscriber {}: {}", user.getPhoneNumber(), e.getMessage());
            }
        }

        log.info("Morning broadcast completed successfully. Total delivered: {}", count);
        return count;
    }

    /**
     * Proactive Craving Shield: Executes every afternoon at 04:30 PM IST.
     * Checks in right when tea-time sugar and fried snack cravings hit.
     */
    @Scheduled(cron = "0 30 16 * * *", zone = "Asia/Kolkata")
    public void executeAfternoonCravingShield() {
        log.info("Starting proactive afternoon craving check-in (4:30 PM IST)...");
        List<User> activeUsers = userRepository.findByActiveTrue();
        for (User user : activeUsers) {
            if (user.getAge() == null || user.getWeight() == null) continue;
            try {
                String name = user.getName() != null && !user.getName().isBlank() ? user.getName() : "Friend";
                String msg = String.format(
                        "☕ *Hi %s, it's 4:30 PM tea time!* 🌿\n\n" +
                        "This is usually when energy dips and cravings for biscuits or fried snacks sneak in.\n\n" +
                        "💡 *Dr. Mohan's Quick Shield:*\n" +
                        "• Swap biscuits for roasted makhana, boiled chana, or a handful of almonds.\n" +
                        "• Drink a glass of water first — mild dehydration often masquerades as sugar cravings!\n\n" +
                        "How is your energy feeling right now? Reply *WATER* to check hydration or send a photo 📸 of your snack!",
                        name
                );
                com.yourapp.whatsapp.client.WhatsAppApiClient client = dailyPlanService.getApiClient();
                if (client != null) {
                    client.sendTextMessage(user.getPhoneNumber(), msg);
                }
                Thread.sleep(50);
            } catch (Exception e) {
                log.error("Failed afternoon check-in for {}: {}", user.getPhoneNumber(), e.getMessage());
            }
        }
    }

    /**
     * Evening Reflection & Sleep Wind-Down: Executes every night at 09:30 PM IST.
     */
    @Scheduled(cron = "0 30 21 * * *", zone = "Asia/Kolkata")
    public void executeEveningCareCheckin() {
        log.info("Starting proactive evening wind-down check-in (9:30 PM IST)...");
        List<User> activeUsers = userRepository.findByActiveTrue();
        for (User user : activeUsers) {
            if (user.getAge() == null || user.getWeight() == null) continue;
            try {
                String name = user.getName() != null && !user.getName().isBlank() ? user.getName() : "Friend";
                String msg = String.format(
                        "🌙 *Good evening %s!* ✨\n\n" +
                        "Another great day invested in your health and longevity. Proud of your consistency!\n\n" +
                        "🛌 *Tonight's Recovery Note:*\n" +
                        "• Avoid screens 30 mins before sleep to support deep metabolic melatonin.\n" +
                        "• Keep dinner light and finish with warm water or chamomile/jeera tea.\n\n" +
                        "Rest well and recharge. Dr. Mohan will have your fresh nutrition plan ready at 7:30 AM tomorrow!",
                        name
                );
                com.yourapp.whatsapp.client.WhatsAppApiClient client = dailyPlanService.getApiClient();
                if (client != null) {
                    client.sendTextMessage(user.getPhoneNumber(), msg);
                }
                Thread.sleep(50);
            } catch (Exception e) {
                log.error("Failed evening check-in for {}: {}", user.getPhoneNumber(), e.getMessage());
            }
        }
    }

    /**
     * Anti-Sleep Self-Ping Heartbeat: Executes every 10 minutes.
     * Prevents free cloud hosts (such as Render) from entering idle sleep mode.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes = 600,000 ms
    public void executeKeepAliveHeartbeat() {
        try {
            String baseUrl = dailyPlanService.getPublicBaseUrl();
            if (baseUrl != null && !baseUrl.isBlank() && !baseUrl.contains("localhost")) {
                String targetUrl = baseUrl + "/api/cards/health";
                org.springframework.web.client.RestClient.create()
                        .get()
                        .uri(targetUrl)
                        .retrieve()
                        .toBodilessEntity();
                log.info("📡 [KEEP-ALIVE] Dispatched heartbeat ping to self: {}", targetUrl);
            }
        } catch (Exception e) {
            log.debug("[KEEP-ALIVE] Heartbeat ping completed: {}", e.getMessage());
        }
    }
}
