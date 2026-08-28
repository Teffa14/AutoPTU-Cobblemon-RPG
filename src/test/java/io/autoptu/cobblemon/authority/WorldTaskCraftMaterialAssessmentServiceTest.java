package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldTaskCraftMaterialAssessmentServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void aggregatesMultipleOwnedStacksAndScalesByCraftQuantity() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDirectory);
        WorldTaskRecipeDefinition recipe = new WorldTaskCatalogue().findRecipe("field_ration").orElseThrow();
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "wheat-a", "player-1", "minecraft:wheat", 2, 0)));
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "wheat-b", "player-1", "minecraft:wheat", 3, 0)));
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "berry-a", "player-1", "minecraft:sweet_berries", 4, 0)));

        WorldTaskCraftMaterialAssessmentService.Assessment assessment =
                new WorldTaskCraftMaterialAssessmentService(items).assess("player-1", recipe, 2);

        assertTrue(assessment.ready());
        assertEquals(4, assessment.ingredients().get(0).required());
        assertEquals(4, assessment.ingredients().get(0).available());
        assertEquals(0, assessment.ingredients().get(0).missing());
    }

    @Test
    void excludesOtherOwnersAndReservedStacks() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDirectory);
        WorldTaskRecipeDefinition recipe = new WorldTaskCatalogue().findRecipe("field_ration").orElseThrow();
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "wheat-own", "player-2", "minecraft:wheat", 2, 0)));
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "wheat-other", "player-3", "minecraft:wheat", 99, 0)));
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "berries-reserved", "player-2", "minecraft:sweet_berries", 2, 0)));
        assertTrue(items.tryReserveItem(new ItemReservation(
                "reservation-1", "player-2", "berries-reserved", "minecraft:sweet_berries", 1, 0)));

        WorldTaskCraftMaterialAssessmentService.Assessment assessment =
                new WorldTaskCraftMaterialAssessmentService(items).assess("player-2", recipe, 1);

        assertFalse(assessment.ready());
        WorldTaskCraftMaterialAssessmentService.IngredientAvailability berries = assessment.ingredients().stream()
                .filter(value -> value.itemTemplateId().equals("minecraft:sweet_berries"))
                .findFirst().orElseThrow();
        assertEquals(2, berries.required());
        assertEquals(0, berries.available());
        assertEquals(2, berries.missing());
    }

    @Test
    void reportsOnlyTheMissingRemainder() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDirectory);
        WorldTaskRecipeDefinition recipe = new WorldTaskCatalogue().findRecipe("field_ration").orElseThrow();
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "wheat", "player-4", "minecraft:wheat", 2, 0)));
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "berries", "player-4", "minecraft:sweet_berries", 1, 0)));

        WorldTaskCraftMaterialAssessmentService.Assessment assessment =
                new WorldTaskCraftMaterialAssessmentService(items).assess("player-4", recipe, 1);

        assertFalse(assessment.ready());
        WorldTaskCraftMaterialAssessmentService.IngredientAvailability berries = assessment.ingredients().stream()
                .filter(value -> value.itemTemplateId().equals("minecraft:sweet_berries"))
                .findFirst().orElseThrow();
        assertEquals(1, berries.available());
        assertEquals(1, berries.missing());
    }
}
