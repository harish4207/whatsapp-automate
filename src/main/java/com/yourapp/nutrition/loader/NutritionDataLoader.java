package com.yourapp.nutrition.loader;

import com.yourapp.nutrition.entity.Condition;
import com.yourapp.nutrition.entity.Meal;
import com.yourapp.nutrition.entity.MealConditionRule;
import com.yourapp.nutrition.repository.ConditionRepository;
import com.yourapp.nutrition.repository.MealConditionRuleRepository;
import com.yourapp.nutrition.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * NutritionDataLoader pre-populates 32 clinical-grade South and North Indian meals
 * and medical contraindication rules for Diabetes, Hypertension, Thyroid, PCOS, and Fatty Liver.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NutritionDataLoader implements CommandLineRunner {

    private final MealRepository mealRepository;
    private final ConditionRepository conditionRepository;
    private final MealConditionRuleRepository conditionRuleRepository;

    @Override
    public void run(String... args) {
        if (mealRepository.count() >= 30) {
            log.info("Meal catalog already initialized with {} meals. Skipping seed.", mealRepository.count());
            return;
        }

        log.info("Seeding 32 clinical Indian meals and health condition rules...");

        // 1. Seed 6 Clinical Health Conditions
        Condition none = conditionRepository.findByNameIgnoreCase("NONE")
                .orElseGet(() -> conditionRepository.save(Condition.builder().name("NONE").description("General Fitness & Health").build()));
        Condition diabetes = conditionRepository.findByNameIgnoreCase("DIABETES")
                .orElseGet(() -> conditionRepository.save(Condition.builder().name("DIABETES").description("Type 2 Diabetes & Pre-diabetes (Strict Low GI)").build()));
        Condition hypertension = conditionRepository.findByNameIgnoreCase("HYPERTENSION")
                .orElseGet(() -> conditionRepository.save(Condition.builder().name("HYPERTENSION").description("High Blood Pressure (Low Sodium DASH diet)").build()));
        Condition thyroid = conditionRepository.findByNameIgnoreCase("THYROID")
                .orElseGet(() -> conditionRepository.save(Condition.builder().name("THYROID").description("Thyroid Support (Low Goitrogens, High Selenium)").build()));
        Condition pcos = conditionRepository.findByNameIgnoreCase("PCOS")
                .orElseGet(() -> conditionRepository.save(Condition.builder().name("PCOS").description("PCOS/PCOD (Anti-inflammatory, Hormone Balance)").build()));
        Condition fattyLiver = conditionRepository.findByNameIgnoreCase("FATTY_LIVER")
                .orElseGet(() -> conditionRepository.save(Condition.builder().name("FATTY_LIVER").description("Non-Alcoholic Fatty Liver (Low Sat Fat, Choline)").build()));

        // 2. Seed 32 Authentic Indian Meals
        List<Meal> mealList = new ArrayList<>();

        // --- BREAKFAST (8 Meals) ---
        mealList.add(Meal.builder()
                .name("Andhra Pesarattu & Allam Chutney")
                .mealType("BREAKFAST").dietType("VEG").cuisine("SOUTH_INDIAN")
                .calories(380).protein(22.0).carbohydrates(50.0).fat(9.0).fiber(9.0).prepTime(18)
                .ingredientsSummary("2 Medium Green Moong Dal Dosas (150g), 40g Fresh Ginger Chutney")
                .build());

        mealList.add(Meal.builder()
                .name("Ragi & Vegetable Idli with Lentil Sambar")
                .mealType("BREAKFAST").dietType("VEG").cuisine("SOUTH_INDIAN")
                .calories(360).protein(16.0).carbohydrates(58.0).fat(6.0).fiber(11.0).prepTime(20)
                .ingredientsSummary("3 Steamed Ragi Idlis (160g), 1 Cup Vegetable Sambar (180ml)")
                .build());

        mealList.add(Meal.builder()
                .name("South Indian Boiled Egg Podi Roast & Ragi Dosa")
                .mealType("BREAKFAST").dietType("EGGETARIAN").cuisine("SOUTH_INDIAN")
                .calories(410).protein(28.0).carbohydrates(38.0).fat(14.0).fiber(6.0).prepTime(15)
                .ingredientsSummary("3 Boiled Eggs (2 whites, 1 whole) with Idli Podi, 1 Thin Ragi Dosa")
                .build());

        mealList.add(Meal.builder()
                .name("Chicken Keema Dosa (Foxtail Millet based)")
                .mealType("BREAKFAST").dietType("NON_VEG").cuisine("SOUTH_INDIAN")
                .calories(440).protein(36.0).carbohydrates(40.0).fat(12.0).fiber(5.0).prepTime(20)
                .ingredientsSummary("Minced Chicken Breast (120g), 1 Millet Dosa, Tomato Onion Masala")
                .build());

        mealList.add(Meal.builder()
                .name("Paneer Bhurji with 2 Multigrain Phulkas")
                .mealType("BREAKFAST").dietType("VEG").cuisine("NORTH_INDIAN")
                .calories(450).protein(28.0).carbohydrates(42.0).fat(18.0).fiber(8.0).prepTime(15)
                .ingredientsSummary("Low-Fat Paneer (140g), 2 Wheat-Oat Phulkas, Onions, Green Chillies")
                .build());

        mealList.add(Meal.builder()
                .name("Moong Dal & Palak Chilla with Curd")
                .mealType("BREAKFAST").dietType("VEG").cuisine("NORTH_INDIAN")
                .calories(370).protein(24.0).carbohydrates(44.0).fat(10.0).fiber(9.0).prepTime(18)
                .ingredientsSummary("2 Yellow Moong & Spinach Crepes (140g), 100g Low-fat Curd")
                .build());

        mealList.add(Meal.builder()
                .name("Masala Egg Scramble (3 Whites, 1 Whole) & Toast")
                .mealType("BREAKFAST").dietType("EGGETARIAN").cuisine("NORTH_INDIAN")
                .calories(390).protein(29.0).carbohydrates(34.0).fat(13.0).fiber(6.0).prepTime(12)
                .ingredientsSummary("3 Egg Whites + 1 Whole Egg Scrambled, 2 Multigrain Toasts, Tomato")
                .build());

        mealList.add(Meal.builder()
                .name("High-Protein Soya Granule Bhurji & Methi Roti")
                .mealType("BREAKFAST").dietType("VEGAN").cuisine("NORTH_INDIAN")
                .calories(420).protein(32.0).carbohydrates(46.0).fat(11.0).fiber(10.0).prepTime(18)
                .ingredientsSummary("Boiled Soya Granules (120g), 2 Fresh Fenugreek Rotis, Spices")
                .build());

        // --- LUNCH (9 Meals) ---
        mealList.add(Meal.builder()
                .name("Hyderabadi Chicken Breast Curry & Brown Rice")
                .mealType("LUNCH").dietType("NON_VEG").cuisine("SOUTH_INDIAN")
                .calories(620).protein(48.0).carbohydrates(62.0).fat(16.0).fiber(8.0).prepTime(25)
                .ingredientsSummary("Skinless Chicken Breast (180g), 1 Cup Brown Rice (150g), Onion Gravy")
                .build());

        mealList.add(Meal.builder()
                .name("Andhra Tomato Pappu & Foxtail Millet")
                .mealType("LUNCH").dietType("VEG").cuisine("SOUTH_INDIAN")
                .calories(580).protein(30.0).carbohydrates(72.0).fat(14.0).fiber(13.0).prepTime(25)
                .ingredientsSummary("1.5 Cups Toor Dal with Tomatoes (200g), 1 Cup Cooked Millets (150g), Beans Poriyal")
                .build());

        mealList.add(Meal.builder()
                .name("Gongura Chicken with 2 Jowar Rotis & Salad")
                .mealType("LUNCH").dietType("NON_VEG").cuisine("SOUTH_INDIAN")
                .calories(590).protein(46.0).carbohydrates(54.0).fat(14.0).fiber(9.0).prepTime(25)
                .ingredientsSummary("Gongura Spiced Chicken (160g), 2 Sorghum (Jowar) Rotis, Cucumber Slices")
                .build());

        mealList.add(Meal.builder()
                .name("Kerala Fish Curry (Kudampuli) & Brown Rice")
                .mealType("LUNCH").dietType("NON_VEG").cuisine("SOUTH_INDIAN")
                .calories(550).protein(42.0).carbohydrates(58.0).fat(12.0).fiber(6.0).prepTime(22)
                .ingredientsSummary("Fish Fillet (180g) in Kokum sauce, 1 Cup Brown Rice, Cabbage salad")
                .build());

        mealList.add(Meal.builder()
                .name("Palak Paneer with 2 Multigrain Phulkas")
                .mealType("LUNCH").dietType("VEG").cuisine("NORTH_INDIAN")
                .calories(580).protein(32.0).carbohydrates(52.0).fat(22.0).fiber(10.0).prepTime(22)
                .ingredientsSummary("Fresh Spinach Puree, Low-fat Paneer (150g), 2 Multigrain Phulkas")
                .build());

        mealList.add(Meal.builder()
                .name("Tandoori Chicken Breast & Mint Sprout Salad")
                .mealType("LUNCH").dietType("NON_VEG").cuisine("NORTH_INDIAN")
                .calories(600).protein(54.0).carbohydrates(36.0).fat(16.0).fiber(8.0).prepTime(25)
                .ingredientsSummary("Marinated Chicken Breast (200g) Tikka, 1 Cup Boiled Moong Sprouts Salad")
                .build());

        mealList.add(Meal.builder()
                .name("Punjabi Rajma Masala & Brown Basmati Rice")
                .mealType("LUNCH").dietType("VEG").cuisine("NORTH_INDIAN")
                .calories(560).protein(24.0).carbohydrates(82.0).fat(12.0).fiber(14.0).prepTime(25)
                .ingredientsSummary("1.5 Cups Slow-cooked Kidney Beans (200g), 1 Cup Brown Basmati Rice, Onion salad")
                .build());

        mealList.add(Meal.builder()
                .name("Homestyle Egg Curry (3 Eggs) & 2 Jowar Rotis")
                .mealType("LUNCH").dietType("EGGETARIAN").cuisine("NORTH_INDIAN")
                .calories(540).protein(30.0).carbohydrates(52.0).fat(20.0).fiber(8.0).prepTime(20)
                .ingredientsSummary("3 Hard Boiled Eggs in Tomato Onion Gravy, 2 Sorghum Flatbreads")
                .build());

        mealList.add(Meal.builder()
                .name("Yellow Moong Dal Tadka, Bhindi & 2 Phulkas")
                .mealType("LUNCH").dietType("VEG").cuisine("NORTH_INDIAN")
                .calories(510).protein(22.0).carbohydrates(70.0).fat(12.0).fiber(12.0).prepTime(22)
                .ingredientsSummary("1.5 Cups Moong Dal Tadka (180g), 1 Cup Sautéed Okra (Bhindi), 2 Whole Wheat Rotis")
                .build());

        // --- SNACK (7 Meals) ---
        mealList.add(Meal.builder()
                .name("Roasted Makhana (Foxnuts) with Pepper")
                .mealType("SNACK").dietType("VEG").cuisine("NORTH_INDIAN")
                .calories(180).protein(6.0).carbohydrates(28.0).fat(4.5).fiber(5.0).prepTime(8)
                .ingredientsSummary("Roasted Foxnuts (45g) in 1 tsp Ghee, Crushed Black Pepper, Rock Salt")
                .build());

        mealList.add(Meal.builder()
                .name("South Indian Sprouted Green Gram Sundal")
                .mealType("SNACK").dietType("VEG").cuisine("SOUTH_INDIAN")
                .calories(210).protein(14.0).carbohydrates(30.0).fat(4.0).fiber(8.0).prepTime(10)
                .ingredientsSummary("Steamed Sprouted Moong (120g), Mustard Seed Tadka, 1 tbsp Fresh Coconut")
                .build());

        mealList.add(Meal.builder()
                .name("Boiled Peanut & Cucumber Kosambari")
                .mealType("SNACK").dietType("VEG").cuisine("SOUTH_INDIAN")
                .calories(230).protein(12.0).carbohydrates(18.0).fat(12.0).fiber(6.0).prepTime(8)
                .ingredientsSummary("Boiled Peanuts (60g), Diced Cucumber, Green Chilli, Lemon Juice")
                .build());

        mealList.add(Meal.builder()
                .name("2 Boiled Egg Whites Chaat with Lemon & Herbs")
                .mealType("SNACK").dietType("EGGETARIAN").cuisine("CONTINENTAL")
                .calories(120).protein(16.0).carbohydrates(4.0).fat(2.0).fiber(1.0).prepTime(5)
                .ingredientsSummary("2 Hard Boiled Egg Whites cubed, Chaat Masala, Lemon Juice, Coriander")
                .build());

        mealList.add(Meal.builder()
                .name("Roasted Chana & Almonds Trail Mix")
                .mealType("SNACK").dietType("VEG").cuisine("NORTH_INDIAN")
                .calories(220).protein(12.0).carbohydrates(22.0).fat(9.0).fiber(7.0).prepTime(2)
                .ingredientsSummary("Roasted Bengal Gram / Chana (40g), 8 Raw Almonds")
                .build());

        mealList.add(Meal.builder()
                .name("Curd & Chia Seed Bowl with Pomegranate")
                .mealType("SNACK").dietType("VEG").cuisine("SOUTH_INDIAN")
                .calories(190).protein(10.0).carbohydrates(22.0).fat(6.0).fiber(6.0).prepTime(5)
                .ingredientsSummary("Low-Fat Curd (150g), 1 tsp Chia Seeds, 2 tbsp Fresh Pomegranate Arils")
                .build());

        mealList.add(Meal.builder()
                .name("Spiced Buttermilk (Chaas) with Roasted Soya Nuts")
                .mealType("SNACK").dietType("VEG").cuisine("NORTH_INDIAN")
                .calories(170).protein(15.0).carbohydrates(12.0).fat(5.0).fiber(4.0).prepTime(5)
                .ingredientsSummary("1 Glass Spiced Buttermilk (200ml) with Jeera, 30g Crunchy Roasted Soya Beans")
                .build());

        // --- DINNER (8 Meals) ---
        mealList.add(Meal.builder()
                .name("Methi Chicken with 2 Millet Rotis & Koshimbir")
                .mealType("DINNER").dietType("NON_VEG").cuisine("NORTH_INDIAN")
                .calories(530).protein(44.0).carbohydrates(48.0).fat(14.0).fiber(8.0).prepTime(22)
                .ingredientsSummary("Chicken Breast in Fenugreek Leaves (160g), 2 Ragi/Jowar Rotis, Cucumber salad")
                .build());

        mealList.add(Meal.builder()
                .name("Palak Moong Dal Khichdi & Homemade Dahi")
                .mealType("DINNER").dietType("VEG").cuisine("NORTH_INDIAN")
                .calories(490).protein(22.0).carbohydrates(68.0).fat(11.0).fiber(11.0).prepTime(20)
                .ingredientsSummary("1.5 Cups Moong-Spinach Khichdi (220g), 100g Fresh Curd")
                .build());

        mealList.add(Meal.builder()
                .name("Tofu & Mixed Veggie Stir-Fry with Ragi Mudde")
                .mealType("DINNER").dietType("VEGAN").cuisine("SOUTH_INDIAN")
                .calories(480).protein(28.0).carbohydrates(54.0).fat(14.0).fiber(12.0).prepTime(20)
                .ingredientsSummary("Firm Tofu (150g), French Beans, Carrots, 1 Small Ragi Mudde Ball (100g)")
                .build());

        mealList.add(Meal.builder()
                .name("Grilled Fish Tikka & Steamed Beans Poriyal")
                .mealType("DINNER").dietType("NON_VEG").cuisine("SOUTH_INDIAN")
                .calories(460).protein(45.0).carbohydrates(24.0).fat(12.0).fiber(7.0).prepTime(20)
                .ingredientsSummary("Grilled Fish Fillet (180g), 1 Cup Green Beans Poriyal (120g)")
                .build());

        mealList.add(Meal.builder()
                .name("Paneer Tikka Salad with Mint Dip & 1 Phulka")
                .mealType("DINNER").dietType("VEG").cuisine("NORTH_INDIAN")
                .calories(510).protein(30.0).carbohydrates(38.0).fat(22.0).fiber(8.0).prepTime(18)
                .ingredientsSummary("Tandoori Paneer Cubes (140g), Bell Peppers, Onions, 1 Whole Wheat Phulka")
                .build());

        mealList.add(Meal.builder()
                .name("Soya Chunk & Capsicum Curry with 2 Jowar Rotis")
                .mealType("DINNER").dietType("VEG").cuisine("NORTH_INDIAN")
                .calories(490).protein(36.0).carbohydrates(50.0).fat(12.0).fiber(11.0).prepTime(20)
                .ingredientsSummary("Boiled Soya Chunks (100g), Capsicum Tomato Gravy, 2 Sorghum Rotis")
                .build());

        mealList.add(Meal.builder()
                .name("South Indian Egg White Rasam & Sweet Potato")
                .mealType("DINNER").dietType("EGGETARIAN").cuisine("SOUTH_INDIAN")
                .calories(420).protein(26.0).carbohydrates(54.0).fat(8.0).fiber(9.0).prepTime(18)
                .ingredientsSummary("Pepper Garlic Rasam with 3 Poached Egg Whites, 1 Boiled Sweet Potato (120g)")
                .build());

        mealList.add(Meal.builder()
                .name("Chicken Clear Pepper Soup & Steamed Sprouts")
                .mealType("DINNER").dietType("NON_VEG").cuisine("CONTINENTAL")
                .calories(410).protein(42.0).carbohydrates(30.0).fat(8.0).fiber(7.0).prepTime(15)
                .ingredientsSummary("Shredded Chicken Breast (160g) in Broth with Celery, Warm Moong Sprouts")
                .build());

        List<Meal> savedMeals = mealRepository.saveAll(mealList);
        log.info("Successfully seeded {} authentic Indian meals into catalog.", savedMeals.size());

        // 3. Seed Clinical Contraindication Rules
        seedClinicalContraindications(savedMeals, diabetes, hypertension, thyroid, pcos, fattyLiver);
    }

    private void seedClinicalContraindications(List<Meal> meals, Condition diabetes, Condition hypertension,
                                              Condition thyroid, Condition pcos, Condition fattyLiver) {
        List<MealConditionRule> rules = new ArrayList<>();

        for (Meal m : meals) {
            // High Carbohydrate meals (> 65g) or high GI are strictly avoided for DIABETES
            if (m.getCarbohydrates() != null && m.getCarbohydrates() > 65.0) {
                rules.add(MealConditionRule.builder()
                        .mealId(m.getId()).conditionId(diabetes.getId()).suitability("AVOID")
                        .notes("High carbohydrate load (>65g) contraindicated for glycemic control.")
                        .build());
            }

            // High Sodium / Salted chaat / Dried papad snacks are avoided for HYPERTENSION
            if (m.getName().toLowerCase().contains("chaat") || m.getName().toLowerCase().contains("pickle")) {
                rules.add(MealConditionRule.builder()
                        .mealId(m.getId()).conditionId(hypertension.getId()).suitability("AVOID")
                        .notes("High sodium seasoning contraindicated for blood pressure control.")
                        .build());
            }

            // Heavy unfermented Soya is avoided for THYROID (Goitrogenic risk)
            if (m.getName().toLowerCase().contains("soya")) {
                rules.add(MealConditionRule.builder()
                        .mealId(m.getId()).conditionId(thyroid.getId()).suitability("AVOID")
                        .notes("High isoflavone / unfermented soy can interfere with thyroid hormone synthesis.")
                        .build());
            }

            // High Saturated Fat meals (> 20g fat) are avoided for FATTY_LIVER & PCOS
            if (m.getFat() != null && m.getFat() > 20.0) {
                rules.add(MealConditionRule.builder()
                        .mealId(m.getId()).conditionId(fattyLiver.getId()).suitability("AVOID")
                        .notes("High saturated fat load (>20g) contraindicated for hepatic steatosis.")
                        .build());
                rules.add(MealConditionRule.builder()
                        .mealId(m.getId()).conditionId(pcos.getId()).suitability("AVOID")
                        .notes("High saturated dairy fat contraindicated for androgen regulation.")
                        .build());
            }
        }

        conditionRuleRepository.saveAll(rules);
        log.info("Successfully seeded {} clinical contraindication rules.", rules.size());
    }
}
