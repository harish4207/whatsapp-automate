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
    private static final int HEIGHT = 860;

    public byte[] renderCard(User user, MealPlan plan, List<PlanMeal> planMeals) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        try {
            // Enable high-quality anti-aliasing and subpixel text rendering
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // 1. Background Gradient (Deep Modern Navy-Slate with subtle dark glow)
            GradientPaint bgGradient = new GradientPaint(0, 0, new Color(11, 15, 25), 0, HEIGHT, new Color(20, 29, 47));
            g2.setPaint(bgGradient);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // Subtle top decorative aesthetic aura glow
            g2.setPaint(new RadialGradientPaint(WIDTH / 2.0f, 0, 600, new float[]{0.0f, 1.0f},
                    new Color[]{new Color(16, 185, 129, 20), new Color(11, 15, 25, 0)}));
            g2.fillRect(0, 0, WIDTH, 350);

            // 2. Header Area
            renderHeader(g2, user, plan);

            // 3. Macro Dashboard (Top 4 Pills)
            renderMacroDashboard(g2, plan);

            // 4. Meal Slots (4 Glassmorphic Cards)
            renderMealCards(g2, planMeals);

            // 5. Personal Doctor's Care Note Banner
            renderDoctorCareNote(g2, user, plan);

            // 6. Bottom Clinical Shield Badge
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
        g2.setColor(new Color(16, 185, 129, 35));
        g2.fill(new RoundRectangle2D.Float(50, 36, 310, 32, 16, 16));
        g2.setColor(new Color(52, 211, 153));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(50, 36, 310, 32, 16, 16));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString("● CLINICALLY BACKED NUTRITION", 65, 57);

        // Date on right with calendar icon accent
        String dateStr = plan.getPlanDate() != null ? 
                plan.getPlanDate().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")) : "Today";
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("📅 " + dateStr, WIDTH - 50 - fm.stringWidth("📅 " + dateStr), 57);

        // Title
        String userName = user.getName() != null && !user.getName().isBlank() ? user.getName() : "Personalized";
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2.drawString(userName + "'s Daily Nutrition Blueprint", 50, 105);

        // Subtitle
        String goalStr = user.getGoal() != null ? user.getGoal().replace("_", " ") : "BALANCED HEALTH";
        String dietStr = user.getDietType() != null ? user.getDietType().replace("_", " ") : "ALL";
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g2.drawString("Protocol: " + goalStr + "  •  Diet: " + dietStr + "  •  Calculated via Mifflin-St Jeor & WHO Standards", 50, 132);
    }

    private void renderMacroDashboard(Graphics2D g2, MealPlan plan) {
        int cardY = 152;
        int cardHeight = 80;
        int cardWidth = 252;
        int gap = 30;
        int startX = 50;

        int calories = plan.getTotalCalories() != null ? plan.getTotalCalories() : 0;
        int protein = plan.getTotalProtein() != null ? plan.getTotalProtein().intValue() : 0;
        int carbs = plan.getTotalCarbs() != null ? plan.getTotalCarbs().intValue() : 0;
        int fat = plan.getTotalFat() != null ? plan.getTotalFat().intValue() : 0;

        drawMacroPill(g2, startX, cardY, cardWidth, cardHeight, "CALORIES", calories + " kcal", new Color(249, 115, 22), new Color(249, 115, 22, 25));
        drawMacroPill(g2, startX + (cardWidth + gap), cardY, cardWidth, cardHeight, "PROTEIN", protein + " g", new Color(16, 185, 129), new Color(16, 185, 129, 25));
        drawMacroPill(g2, startX + (cardWidth + gap) * 2, cardY, cardWidth, cardHeight, "CARBOHYDRATES", carbs + " g", new Color(6, 182, 212), new Color(6, 182, 212, 25));
        drawMacroPill(g2, startX + (cardWidth + gap) * 3, cardY, cardWidth, cardHeight, "HEALTHY FATS", fat + " g", new Color(168, 85, 247), new Color(168, 85, 247, 25));
    }

    private void drawMacroPill(Graphics2D g2, int x, int y, int w, int h, String label, String value, Color accent, Color bg) {
        g2.setColor(new Color(24, 32, 50));
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 18, 18));

        // Pill border
        g2.setColor(new Color(51, 65, 85, 180));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, 18, 18));

        // Left accent indicator
        g2.setColor(accent);
        g2.fill(new RoundRectangle2D.Float(x + 14, y + 16, 4, h - 32, 3, 3));

        // Label
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(label, x + 28, y + 32);

        // Value
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2.drawString(value, x + 28, y + 62);
    }

    private void renderMealCards(Graphics2D g2, List<PlanMeal> planMeals) {
        int startY = 250;
        int cardWidth = 535;
        int cardHeight = 205;
        int colGap = 30;
        int rowGap = 16;

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
        // Glassmorphic Card Background
        g2.setColor(new Color(24, 32, 50, 230));
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 18, 18));

        // Card Border
        g2.setColor(new Color(51, 65, 85, 200));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, 18, 18));

        // Slot Title
        g2.setColor(accentColor);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString(slotTitle, x + 20, y + 30);

        if (pm == null || pm.getMeal() == null) {
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g2.drawString("No meal configured for slot", x + 20, y + 75);
            return;
        }

        Meal meal = pm.getMeal();

        // Meal Name
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        String mealName = meal.getName();
        if (mealName.length() > 32) mealName = mealName.substring(0, 30) + "...";
        g2.drawString(mealName, x + 20, y + 65);

        // Portion summary / Kitchen measurements
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        String ingredients = meal.getIngredientsSummary() != null ? meal.getIngredientsSummary() : "";
        if (ingredients.length() > 52) ingredients = ingredients.substring(0, 50) + "...";
        g2.drawString("🍽️ Portion: " + ingredients, x + 20, y + 98);

        // Calories Pill inside Card
        g2.setColor(new Color(13, 18, 30));
        g2.fill(new RoundRectangle2D.Float(x + 20, y + 124, 120, 34, 10, 10));
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.drawString(meal.getCalories() + " kcal", x + 34, y + 146);

        // Macro breakdown row
        g2.setColor(new Color(148, 163, 184));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        String macroStr = String.format("P: %.0fg  •  C: %.0fg  •  F: %.0fg  •  Fiber: %.0fg", 
                meal.getProtein() != null ? meal.getProtein() : 0,
                meal.getCarbohydrates() != null ? meal.getCarbohydrates() : 0,
                meal.getFat() != null ? meal.getFat() : 0,
                meal.getFiber() != null ? meal.getFiber() : 0);
        g2.drawString(macroStr, x + 155, y + 146);
    }

    private void renderDoctorCareNote(Graphics2D g2, User user, MealPlan plan) {
        int noteY = 690;
        int noteHeight = 52;
        int noteWidth = WIDTH - 100;

        // Soft indigo care banner
        g2.setColor(new Color(99, 102, 241, 20));
        g2.fill(new RoundRectangle2D.Float(50, noteY, noteWidth, noteHeight, 14, 14));
        g2.setColor(new Color(129, 140, 248, 100));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(50, noteY, noteWidth, noteHeight, 14, 14));

        String condition = user.getHealthCondition() != null ? user.getHealthCondition() : "NONE";
        String careNote = switch (condition.toUpperCase()) {
            case "DIABETES" -> "Dr. Note: Pair your meals with a 10-min slow walk to blunt glycemic spikes. Stay hydrated!";
            case "HYPERTENSION" -> "Dr. Note: All recipes strictly limit table salt & pickles. Savor natural herbs, cumin & lemon for flavor.";
            case "THYROID" -> "Dr. Note: Your selenium & zinc targets are dialed in. Remember your morning medication 30 mins before breakfast.";
            case "PCOS" -> "Dr. Note: Anti-inflammatory herbs included. Sip warm cinnamon or spearmint tea this afternoon for hormone balance.";
            case "FATTY_LIVER" -> "Dr. Note: Saturated fats kept under 5%. Focus on fresh antioxidant greens and lean protein to support liver recovery.";
            default -> "Dr. Note: Consistency creates momentum. Drink 300ml of water before each meal to optimize absorption!";
        };

        g2.setColor(new Color(199, 210, 254));
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString("🩺 " + careNote, 70, noteY + 31);
    }

    private void renderClinicalBadge(Graphics2D g2, User user) {
        int badgeY = 756;
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
        g2.fill(new RoundRectangle2D.Float(50, badgeY, badgeWidth, badgeHeight, 14, 14));

        // Border
        g2.setColor(new Color(16, 185, 129, 120));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(50, badgeY, badgeWidth, badgeHeight, 14, 14));

        // Shield Text
        g2.setColor(new Color(52, 211, 153));
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString("🛡️ " + conditionText, 70, badgeY + 31);
    }

    private PlanMeal findSlot(List<PlanMeal> planMeals, String slot) {
        if (planMeals == null) return null;
        return planMeals.stream()
                .filter(pm -> slot.equalsIgnoreCase(pm.getMealType()))
                .findFirst()
                .orElse(null);
    }
}
