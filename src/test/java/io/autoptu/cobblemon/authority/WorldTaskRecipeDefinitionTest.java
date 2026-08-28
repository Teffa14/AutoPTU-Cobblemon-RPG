package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldTaskRecipeDefinitionTest {
    @Test
    void cataloguePublishesServerOwnedIngredientsWorkstationAndEveryQualityOutput() {
        WorldTaskRecipeDefinition recipe = new WorldTaskCatalogue()
                .findRecipe("precision_poketech_parts")
                .orElseThrow();

        assertEquals(WorldTaskCatalogue.GENERAL_CRAFTING_WORKSTATION, recipe.workstationId());
        assertEquals(List.of(
                new WorldTaskRecipeDefinition.IngredientRequirement("minecraft:copper_ingot", 2),
                new WorldTaskRecipeDefinition.IngredientRequirement("minecraft:redstone", 2),
                new WorldTaskRecipeDefinition.IngredientRequirement("minecraft:iron_nugget", 4)
        ), recipe.ingredients());
        assertEquals(
                new WorldTaskRecipeDefinition.CraftOutput("ouros:precision_poketech_parts", 1),
                recipe.outputFor(WorldTaskRecipeDefinition.CraftQuality.STANDARD));
        assertEquals(3, recipe.outputsByQuality().size());
    }

    @Test
    void recipeRejectsMissingQualityOutputAndInvalidQuantities() {
        WorldTaskDefinition task = task();
        Map<WorldTaskRecipeDefinition.CraftQuality, WorldTaskRecipeDefinition.CraftOutput> incomplete =
                new EnumMap<>(WorldTaskRecipeDefinition.CraftQuality.class);
        incomplete.put(
                WorldTaskRecipeDefinition.CraftQuality.STANDARD,
                new WorldTaskRecipeDefinition.CraftOutput("ouros:test", 1));

        assertThrows(IllegalArgumentException.class, () -> new WorldTaskRecipeDefinition(
                task,
                "ouros:test_station",
                List.of(new WorldTaskRecipeDefinition.IngredientRequirement("minecraft:stone", 1)),
                incomplete));
        assertThrows(IllegalArgumentException.class,
                () -> new WorldTaskRecipeDefinition.IngredientRequirement("minecraft:stone", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new WorldTaskRecipeDefinition.CraftOutput("ouros:test", 0));
    }

    @Test
    void recipeCollectionsAreDefensivelyCopied() {
        WorldTaskRecipeDefinition recipe = new WorldTaskCatalogue().findRecipe("field_ration").orElseThrow();

        assertThrows(UnsupportedOperationException.class, () -> recipe.ingredients().add(
                new WorldTaskRecipeDefinition.IngredientRequirement("minecraft:dirt", 1)));
        assertThrows(UnsupportedOperationException.class, () -> recipe.outputsByQuality().put(
                WorldTaskRecipeDefinition.CraftQuality.STANDARD,
                new WorldTaskRecipeDefinition.CraftOutput("ouros:spoof", 99)));
    }

    private static WorldTaskDefinition task() {
        TreeMap<Integer, WorldTaskDefinition.QualityDistribution> curve = new TreeMap<>();
        curve.put(0, new WorldTaskDefinition.QualityDistribution(100, 0, 0));
        return new WorldTaskDefinition("test", "Test", "Survival", 0, curve);
    }
}
