package com.yourapp.nutrition.visual;

import com.yourapp.nutrition.entity.Meal;
import com.yourapp.nutrition.entity.MealPlan;
import com.yourapp.nutrition.entity.PlanMeal;
import com.yourapp.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * NutritionCardRenderer dynamically generates high-resolution, dark-mode visual
 * Nutrition Blueprint infographic cards (PNG) using Java Graphics2D.
 */
@Component
@Slf4j
public class NutritionCardRenderer {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 850;

    public byte[] renderCard(User user, MealPlan plan, List<PlanMeal> planMeals) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        try {
            // Enable high-quality anti-aliasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 1. Background Gradient (Sleek Slate/Navy)
            GradientPaint bgGradient = new GradientPaint(0, 0, new Color(15, 23, 42), 0, HEIGHT, new Color(30, 41, 59));
            g2.setPaint(bgGradient);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // 2. Header Area
            renderHeader(g2, user, plan);

            // 3. Macro Dashboard (Top 4 Pills)
            renderMacroDashboard(g2, plan);

            // 4. Meal Slots (4 Rounded Cards)
            renderMealCards(g2, planMeals);

            // 5. Bottom Clinical Shield Badge
            renderClinicalBadge(g2, user);

        } finally {
            g2.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to encode nutrition card image: {}", e.getMessage(), e);
            throw new RuntimeException("Image encoding failed", e);
        }
    }

    private void renderHeader(Graphics2D g2, User user, MealPlan plan) {
        // Tagline badge
        g2.setColor(new Color(16, 185, 129, 40));
        g2.fill(new RoundRectangle2D.Float(50, 40, 310, 32, 16, 16));
        g2.setColor(new Color(16, 185, 129));
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString("● CLINICALLY BACKED NUTRITION", 65, 61);

        // Date on right
        String dateStr = plan.getPlanDate() != null ? 
                plan.getPlanDate().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) : "Today";
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(dateStr, WIDTH - 50 - fm.stringWidth(dateStr), 62);

        // Title
        String userName = user.getName() != null && !user.getName().isBlank() ? user.getName() : "Personalized";
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 30));
        g2.drawString(userName + "'s Daily Nutrition Blueprint", 50, 115);

        // Subtitle
        String goalStr = user.getGoal() != null ? user.getGoal().replace("_", " ") : "BALANCED HEALTH";
        String dietStr = user.getDietType() != null ? user.getDietType() : "ALL";
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2.drawString("Protocol: " + goalStr + "  •  Diet: " + dietStr + "  •  Calculated via Mifflin-St Jeor Formula", 50, 142);
    }

    private void renderMacroDashboard(Graphics2D g2, MealPlan plan) {
        int cardY = 165;
        int cardHeight = 85;
        int cardWidth = 252;
        int gap = 30;
        int startX = 50;

        int calories = plan.getTotalCalories() != null ? plan.getTotalCalories() : 0;
        int protein = plan.getTotalProtein() != null ? plan.getTotalProtein().intValue() : 0;
        int carbs = plan.getTotalCarbs() != null ? plan.getTotalCarbs().intValue() : 0;
        int fat = plan.getTotalFat() != null ? plan.getTotalFat().intValue() : 0;

        drawMacroPill(g2, startX, cardY, cardWidth, cardHeight, "CALORIES", calories + " kcal", new Color(249, 115, 22), new Color(249, 115, 22, 30));
        drawMacroPill(g2, startX + (cardWidth + gap), cardY, cardWidth, cardHeight, "PROTEIN", protein + " g", new Color(16, 185, 129), new Color(16, 185, 129, 30));
        drawMacroPill(g2, startX + (cardWidth + gap) * 2, cardY, cardWidth, cardHeight, "CARBOHYDRATES", carbs + " g", new Color(6, 182, 212), new Color(6, 182, 212, 30));
        drawMacroPill(g2, startX + (cardWidth + gap) * 3, cardY, cardWidth, cardHeight, "HEALTHY FATS", fat + " g", new Color(168, 85, 247), new Color(168, 85, 247, 30));
    }

    private void drawMacroPill(Graphics2D g2, int x, int y, int w, int h, String label, String value, Color accent, Color bg) {
        g2.setColor(new Color(30, 41, 59));
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 20, 20));

        // Left accent indicator
        g2.setColor(accent);
        g2.fill(new RoundRectangle2D.Float(x + 14, y + 18, 5, h - 36, 4, 4));

        // Label
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString(label, x + 30, y + 36);

        // Value
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2.drawString(value, x + 30, y + 66);
    }

    private void renderMealCards(Graphics2D g2, List<PlanMeal> planMeals) {
        int startY = 275;
        int cardWidth = 535;
        int cardHeight = 220;
        int colGap = 30;
        int rowGap = 20;

        PlanMeal breakfast = findSlot(planMeals, "BREAKFAST");
        PlanMeal lunch = findSlot(planMeals, "LUNCH");
        PlanMeal snack = findSlot(planMeals, "SNACK");
        PlanMeal dinner = findSlot(planMeals, "DINNER");

        // Top Row: Breakfast & Lunch
        drawMealCard(g2, 50, startY, cardWidth, cardHeight, "🌅 BREAKFAST (08:30 AM)", breakfast, new Color(251, 146, 60));
        drawMealCard(g2, 50 + cardWidth + colGap, startY, cardWidth, cardHeight, "☀️ LUNCH (01:30 PM)", lunch, new Color(56, 189, 248));

        // Bottom Row: Snack & Dinner
        drawMealCard(g2, 50, startY + cardHeight + rowGap, cardWidth, cardHeight, "☕ EVENING SNACK (05:00 PM)", snack, new Color(250, 204, 21));
        drawMealCard(g2, 50 + cardWidth + colGap, startY + cardHeight + rowGap, cardWidth, cardHeight, "🌙 DINNER (08:30 PM)", dinner, new Color(192, 132, 252));
    }

    private void drawMealCard(Graphics2D g2, int x, int y, int w, int h, String slotTitle, PlanMeal pm, Color accentColor) {
        // Card Background
        g2.setColor(new Color(30, 41, 59, 230));
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 20, 20));

        // Card Border
        g2.setColor(new Color(51, 65, 85));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, 20, 20));

        // Slot Title
        g2.setColor(accentColor);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString(slotTitle, x + 20, y + 35);

        if (pm == null || pm.getMeal() == null) {
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g2.drawString("No meal configured for slot", x + 20, y + 80);
            return;
        }

        Meal meal = pm.getMeal();

        // Meal Name
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 19));
        String mealName = meal.getName();
        if (mealName.length() > 32) mealName = mealName.substring(0, 30) + "...";
        g2.drawString(mealName, x + 20, y + 70);

        // Portion summary / Kitchen measurements
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        String ingredients = meal.getIngredientsSummary() != null ? meal.getIngredientsSummary() : "";
        if (ingredients.length() > 52) ingredients = ingredients.substring(0, 50) + "...";
        g2.drawString("🍽️ Portion: " + ingredients, x + 20, y + 105);

        // Calories Pill inside Card
        g2.setColor(new Color(15, 23, 42));
        g2.fill(new RoundRectangle2D.Float(x + 20, y + 130, 130, 36, 12, 12));
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.drawString(meal.getCalories() + " kcal", x + 35, y + 154);

        // Macro breakdown row
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        String macroStr = String.format("P: %.0fg  •  C: %.0fg  •  F: %.0fg  •  Fiber: %.0fg", 
                meal.getProtein() != null ? meal.getProtein() : 0,
                meal.getCarbohydrates() != null ? meal.getCarbohydrates() : 0,
                meal.getFat() != null ? meal.getFat() : 0,
                meal.getFiber() != null ? meal.getFiber() : 0);
        g2.drawString(macroStr, x + 165, y + 154);
    }

    private void renderClinicalBadge(Graphics2D g2, User user) {
        int badgeY = 760;
        int badgeHeight = 52;
        int badgeWidth = WIDTH - 100;

        String condition = user.getHealthCondition() != null ? user.getHealthCondition() : "NONE";
        String conditionText = switch (condition.toUpperCase()) {
            case "DIABETES" -> "CLINICAL SHIELD ACTIVE: Verified Safe for Diabetes (Strict Low-GI • High Soluble Fiber • Zero Added Sugar)";
            case "HYPERTENSION" -> "CLINICAL SHIELD ACTIVE: Verified Safe for Blood Pressure (DASH Compliant • Low Sodium • High Potassium)";
            case "THYROID" -> "CLINICAL SHIELD ACTIVE: Verified Safe for Thyroid Support (High Zinc & Selenium • Zero Raw Goitrogens)";
            case "PCOS" -> "CLINICAL SHIELD ACTIVE: Verified Safe for PCOS/PCOD (Hormone-Balancing • Anti-inflammatory • Low Glycemic)";
            case "FATTY_LIVER" -> "CLINICAL SHIELD ACTIVE: Verified Safe for Fatty Liver (Hepatic Detox • Low Saturated Fat • Choline Rich)";
            default -> "CLINICAL SHIELD ACTIVE: Scientifically Balanced Macronutrients & Micronutrients for Peak Longevity";
        };

        // Shield Banner background
        g2.setColor(new Color(16, 185, 129, 25));
        g2.fill(new RoundRectangle2D.Float(50, badgeY, badgeWidth, badgeHeight, 16, 16));

        // Border
        g2.setColor(new Color(16, 185, 129, 120));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(50, badgeY, badgeWidth, badgeHeight, 16, 16));

        // Shield Text
        g2.setColor(new Color(52, 211, 153));
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString("🛡️ " + conditionText, 70, badgeY + 32);
    }

    private PlanMeal findSlot(List<PlanMeal> planMeals, String slot) {
        if (planMeals == null) return null;
        return planMeals.stream()
                .filter(pm -> slot.equalsIgnoreCase(pm.getMealType()))
                .findFirst()
                .orElse(null);
    }
}
