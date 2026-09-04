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
}
