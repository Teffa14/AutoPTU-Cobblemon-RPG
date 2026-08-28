package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldTaskCraftServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void committedCraftConsumesCanonicalIngredientsAndCreatesOneQualityOutput() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDirectory);
        FileWorldTaskCraftAttemptRepository attempts = new FileWorldTaskCraftAttemptRepository(tempDirectory);
        WorldTaskRecipeDefinition recipe = new WorldTaskCatalogue().find("field_ration").orElseThrow();
        seedIngredients(items, "player-1", recipe, 1);
        AtomicInteger rolls = new AtomicInteger();
        WorldTaskCraftService service = new WorldTaskCraftService(
                items, attempts, new WorldTaskCompetenceService(), () -> {
                    rolls.incrementAndGet();
                    return 100;
                });

        WorldTaskCraftService.CraftResult result = service.craft(
                "attempt-1", trainer("player-1", Map.of("Survival", 6)), recipe, 1);

        assertTrue(result.committed());
        assertEquals(WorldTaskRecipeDefinition.CraftQuality.EXCELLENT, result.attempt().quality());
        assertEquals(1, rolls.get());
        CanonicalItemInstance output = items.findItem("craft-output:attempt-1").orElseThrow();
        assertEquals(recipe.outputFor(WorldTaskRecipeDefinition.CraftQuality.EXCELLENT).itemTemplateId(), output.templateId());
        assertEquals(recipe.outputFor(WorldTaskRecipeDefinition.CraftQuality.EXCELLENT).quantity(), output.quantity());
        for (WorldTaskCraftAttempt.PlannedReservation reservation : result.attempt().ingredientReservations()) {
            CanonicalItemInstance ingredient = items.findItem(reservation.itemInstanceId()).orElseThrow();
            assertEquals(reservation.itemRevision() + 1, ingredient.revision());
            assertTrue(items.findReservation(reservation.reservationId()).isEmpty());
        }
    }

    @Test
    void retryAfterRepositoryRestartReusesOutcomeAndDoesNotDuplicateOutput() {
        FileCanonicalItemReservationRepository firstItems = new FileCanonicalItemReservationRepository(tempDirectory);
        FileWorldTaskCraftAttemptRepository firstAttempts = new FileWorldTaskCraftAttemptRepository(tempDirectory);
        WorldTaskRecipeDefinition recipe = new WorldTaskCatalogue().find("field_ration").orElseThrow();
        seedIngredients(firstItems, "player-2", recipe, 1);
        WorldTaskCraftService first = new WorldTaskCraftService(
                firstItems, firstAttempts, new WorldTaskCompetenceService(), () -> 85);
        WorldTaskCraftService.CraftResult initial = first.craft(
                "attempt-restart", trainer("player-2", Map.of("Survival", 4)), recipe, 1);
        assertTrue(initial.committed());
        CanonicalItemInstance outputBefore = firstItems.findItem("craft-output:attempt-restart").orElseThrow();

        FileCanonicalItemReservationRepository restartedItems = new FileCanonicalItemReservationRepository(tempDirectory);
        FileWorldTaskCraftAttemptRepository restartedAttempts = new FileWorldTaskCraftAttemptRepository(tempDirectory);
        WorldTaskCraftService restarted = new WorldTaskCraftService(
                restartedItems, restartedAttempts, new WorldTaskCompetenceService(),
                () -> { throw new AssertionError("committed attempt must never reroll"); });
        WorldTaskCraftService.CraftResult retry = restarted.craft(
                "attempt-restart", trainer("player-2", Map.of("Survival", 0)), recipe, 1);

        assertTrue(retry.committed());
        assertEquals(initial.attempt().rollPercent(), retry.attempt().rollPercent());
        assertEquals(initial.attempt().quality(), retry.attempt().quality());
        assertEquals(outputBefore, restartedItems.findItem("craft-output:attempt-restart").orElseThrow());
    }

    @Test
    void restartAfterPartialIngredientConsumptionRecoversWithoutReroll() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDirectory);
        FileWorldTaskCraftAttemptRepository attempts = new FileWorldTaskCraftAttemptRepository(tempDirectory);
        WorldTaskRecipeDefinition recipe = new WorldTaskCatalogue().find("field_ration").orElseThrow();
        seedIngredients(items, "player-3", recipe, 1);
        CanonicalPlayerState trainer = trainer("player-3", Map.of("Survival", 4));
        WorldTaskDefinition.QualityDistribution distribution = new WorldTaskCompetenceService()
                .assess(trainer, recipe.task()).distribution();
        List<WorldTaskCraftAttempt.PlannedReservation> plan = planFromSeededItems(
                items, "recover", "player-3", recipe);
        WorldTaskCraftAttempt planned = WorldTaskCraftAttempt.planned(
                "recover", "player-3", recipe.taskId(), 1, plan, distribution);
        assertTrue(attempts.createIfAbsent(planned));
        for (WorldTaskCraftAttempt.PlannedReservation reservation : plan) {
            assertTrue(items.tryReserveItem(reservation.asItemReservation("player-3")));
        }
        WorldTaskRecipeDefinition.CraftQuality frozenQuality = WorldTaskRecipeDefinition.CraftQuality.STANDARD;
        WorldTaskCraftAttempt resolved = planned.resolved(80, frozenQuality, recipe.outputFor(frozenQuality));
        assertTrue(attempts.replaceIfPhase("recover", WorldTaskCraftAttempt.Phase.PLANNED, resolved));
        assertTrue(items.consumeReservationRetainingLock(plan.get(0).reservationId(), "player-3"));

        FileCanonicalItemReservationRepository restartedItems = new FileCanonicalItemReservationRepository(tempDirectory);
        FileWorldTaskCraftAttemptRepository restartedAttempts = new FileWorldTaskCraftAttemptRepository(tempDirectory);
        WorldTaskCraftService restarted = new WorldTaskCraftService(
                restartedItems, restartedAttempts, new WorldTaskCompetenceService(),
                () -> { throw new AssertionError("resolved attempt must never reroll"); });
        WorldTaskCraftService.CraftResult recovered = restarted.craft("recover", trainer, recipe, 1);

        assertTrue(recovered.committed());
        assertEquals(80, recovered.attempt().rollPercent());
        assertEquals(frozenQuality, recovered.attempt().quality());
        assertTrue(restartedItems.findItem("craft-output:recover").isPresent());
        for (WorldTaskCraftAttempt.PlannedReservation reservation : plan) {
            assertFalse(restartedItems.findReservation(reservation.reservationId()).isPresent());
        }
    }

    @Test
    void insufficientCanonicalMaterialsNeverCreateAttemptOrRoll() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDirectory);
        FileWorldTaskCraftAttemptRepository attempts = new FileWorldTaskCraftAttemptRepository(tempDirectory);
        WorldTaskRecipeDefinition recipe = new WorldTaskCatalogue().find("field_ration").orElseThrow();
        AtomicInteger rolls = new AtomicInteger();
        WorldTaskCraftService service = new WorldTaskCraftService(
                items, attempts, new WorldTaskCompetenceService(), () -> {
                    rolls.incrementAndGet();
                    return 1;
                });

        WorldTaskCraftService.CraftResult result = service.craft(
                "no-materials", trainer("player-4", Map.of("Survival", 7)), recipe, 1);

        assertEquals(WorldTaskCraftService.Status.INSUFFICIENT_INGREDIENTS, result.status());
        assertEquals(0, rolls.get());
        assertTrue(attempts.find("no-materials").isEmpty());
    }

    private static void seedIngredients(
            FileCanonicalItemReservationRepository items,
            String playerId,
            WorldTaskRecipeDefinition recipe,
            int craftQuantity
    ) {
        int index = 0;
        for (WorldTaskRecipeDefinition.IngredientRequirement requirement : recipe.ingredients()) {
            int quantity = Math.multiplyExact(requirement.quantity(), craftQuantity);
            assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                    "ingredient-" + playerId + "-" + index,
                    playerId,
                    requirement.itemTemplateId(),
                    quantity,
                    0
            )));
            index++;
        }
    }

    private static List<WorldTaskCraftAttempt.PlannedReservation> planFromSeededItems(
            FileCanonicalItemReservationRepository items,
            String attemptId,
            String playerId,
            WorldTaskRecipeDefinition recipe
    ) {
        List<WorldTaskCraftAttempt.PlannedReservation> plan = new ArrayList<>();
        int index = 0;
        for (WorldTaskRecipeDefinition.IngredientRequirement requirement : recipe.ingredients()) {
            CanonicalItemInstance item = items.findReservableItems(playerId, requirement.itemTemplateId()).get(0);
            plan.add(new WorldTaskCraftAttempt.PlannedReservation(
                    "craft:" + attemptId + ":ingredient:" + index + ":part:0",
                    item.itemInstanceId(),
                    item.templateId(),
                    requirement.quantity(),
                    item.revision()
            ));
            index++;
        }
        return List.copyOf(plan);
    }

    private static CanonicalPlayerState trainer(String playerId, Map<String, Integer> skillRanks) {
        return new CanonicalPlayerState(
                playerId,
                Set.of(),
                skillRanks,
                Set.of(),
                Set.of(),
                0,
                0,
                0,
                playerId,
                0
        );
    }
}
