package io.autoptu.cobblemon.authority;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import static io.autoptu.cobblemon.authority.WorldTaskRecipeDefinition.CraftQuality.EXCELLENT;
import static io.autoptu.cobblemon.authority.WorldTaskRecipeDefinition.CraftQuality.IMPROVISED;
import static io.autoptu.cobblemon.authority.WorldTaskRecipeDefinition.CraftQuality.STANDARD;

/** Server-owned initial catalogue for capability-sensitive Ouros world tasks and crafting contracts. */
public final class WorldTaskCatalogue {
    public static final String GENERAL_CRAFTING_WORKSTATION = "ouros:general_crafting_workstation";

    private final Map<String, WorldTaskRecipeDefinition> recipes;

    public WorldTaskCatalogue() {
        this.recipes = Map.of(
                "field_ration", recipe(
                        new WorldTaskDefinition(
                                "field_ration",
                                "Field Ration",
                                "Survival",
                                0,
                                curve(
                                        0, quality(70, 25, 5),
                                        2, quality(45, 40, 15),
                                        4, quality(20, 50, 30),
                                        6, quality(5, 50, 45)
                                )
                        ),
                        List.of(
                                ingredient("minecraft:wheat", 2),
                                ingredient("minecraft:sweet_berries", 2)
                        ),
                        output("ouros:field_ration_improvised", 1),
                        output("ouros:field_ration", 1),
                        output("ouros:field_ration_excellent", 1)
                ),
                "precision_poketech_parts", recipe(
                        new WorldTaskDefinition(
                                "precision_poketech_parts",
                                "Precision Poketech Parts",
                                "Technology Education",
                                1,
                                curve(
                                        0, quality(100, 0, 0),
                                        1, quality(65, 30, 5),
                                        3, quality(35, 45, 20),
                                        5, quality(15, 50, 35),
                                        7, quality(5, 45, 50)
                                )
                        ),
                        List.of(
                                ingredient("minecraft:copper_ingot", 2),
                                ingredient("minecraft:redstone", 2),
                                ingredient("minecraft:iron_nugget", 4)
                        ),
                        output("ouros:precision_poketech_parts_improvised", 1),
                        output("ouros:precision_poketech_parts", 1),
                        output("ouros:precision_poketech_parts_excellent", 1)
                ),
                "occult_lure", recipe(
                        new WorldTaskDefinition(
                                "occult_lure",
                                "Occult Lure",
                                "Occult Education",
                                1,
                                curve(
                                        0, quality(100, 0, 0),
                                        1, quality(70, 25, 5),
                                        3, quality(40, 45, 15),
                                        5, quality(20, 50, 30),
                                        7, quality(5, 50, 45)
                                )
                        ),
                        List.of(
                                ingredient("minecraft:string", 2),
                                ingredient("minecraft:amethyst_shard", 1),
                                ingredient("minecraft:glow_berries", 2)
                        ),
                        output("ouros:occult_lure_improvised", 1),
                        output("ouros:occult_lure", 1),
                        output("ouros:occult_lure_excellent", 1)
                )
        );
    }

    public Optional<WorldTaskDefinition> find(String taskId) {
        return findRecipe(taskId).map(WorldTaskRecipeDefinition::task);
    }

    public Optional<WorldTaskRecipeDefinition> findRecipe(String taskId) {
        if (taskId == null || taskId.isBlank()) return Optional.empty();
        return Optional.ofNullable(recipes.get(taskId.trim().toLowerCase()));
    }

    public List<WorldTaskDefinition> all() {
        return allRecipes().stream().map(WorldTaskRecipeDefinition::task).toList();
    }

    public List<WorldTaskRecipeDefinition> allRecipes() {
        return recipes.values().stream()
                .sorted(java.util.Comparator.comparing(WorldTaskRecipeDefinition::taskId))
                .toList();
    }

    private static WorldTaskRecipeDefinition recipe(
            WorldTaskDefinition task,
            List<WorldTaskRecipeDefinition.IngredientRequirement> ingredients,
            WorldTaskRecipeDefinition.CraftOutput improvised,
            WorldTaskRecipeDefinition.CraftOutput standard,
            WorldTaskRecipeDefinition.CraftOutput excellent
    ) {
        EnumMap<WorldTaskRecipeDefinition.CraftQuality, WorldTaskRecipeDefinition.CraftOutput> outputs =
                new EnumMap<>(WorldTaskRecipeDefinition.CraftQuality.class);
        outputs.put(IMPROVISED, improvised);
        outputs.put(STANDARD, standard);
        outputs.put(EXCELLENT, excellent);
        return new WorldTaskRecipeDefinition(task, GENERAL_CRAFTING_WORKSTATION, ingredients, outputs);
    }

    private static WorldTaskRecipeDefinition.IngredientRequirement ingredient(String templateId, int quantity) {
        return new WorldTaskRecipeDefinition.IngredientRequirement(templateId, quantity);
    }

    private static WorldTaskRecipeDefinition.CraftOutput output(String templateId, int quantity) {
        return new WorldTaskRecipeDefinition.CraftOutput(templateId, quantity);
    }

    private static WorldTaskDefinition.QualityDistribution quality(
            int improvised,
            int standard,
            int excellent
    ) {
        return new WorldTaskDefinition.QualityDistribution(improvised, standard, excellent);
    }

    private static NavigableMap<Integer, WorldTaskDefinition.QualityDistribution> curve(Object... values) {
        if (values.length == 0 || values.length % 2 != 0) {
            throw new IllegalArgumentException("curve requires rank/distribution pairs");
        }
        TreeMap<Integer, WorldTaskDefinition.QualityDistribution> curve = new TreeMap<>();
        for (int index = 0; index < values.length; index += 2) {
            curve.put(
                    (Integer) values[index],
                    (WorldTaskDefinition.QualityDistribution) values[index + 1]
            );
        }
        return curve;
    }
}
