package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalCraftIngredientDepositServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void depositsOnlyAuthoredIngredientsAndAccumulatesInOneCanonicalStack() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDir);
        CanonicalCraftIngredientDepositService service =
                new CanonicalCraftIngredientDepositService(repository, new WorldTaskCatalogue());

        assertTrue(service.supports("minecraft:wheat"));
        assertFalse(service.supports("minecraft:diamond"));

        CanonicalCraftIngredientDepositService.DepositResult first =
                service.deposit("player-1", "minecraft:wheat", 2);
        CanonicalCraftIngredientDepositService.DepositResult second =
                service.deposit("player-1", "minecraft:wheat", 3);
        CanonicalCraftIngredientDepositService.DepositResult unsupported =
                service.deposit("player-1", "minecraft:diamond", 1);

        assertTrue(first.applied());
        assertEquals(2, first.canonicalQuantity());
        assertTrue(second.applied());
        assertEquals(5, second.canonicalQuantity());
        assertEquals(CanonicalCraftIngredientDepositService.DepositResult.Outcome.UNSUPPORTED, unsupported.outcome());

        String itemId = CanonicalCraftIngredientDepositService.stableStackId("player-1", "minecraft:wheat");
        CanonicalItemInstance stored = repository.findItem(itemId).orElseThrow();
        assertEquals("player-1", stored.ownerPlayerId());
        assertEquals("minecraft:wheat", stored.templateId());
        assertEquals(5, stored.quantity());
        assertEquals(1L, stored.revision());
    }

    @Test
    void rejectsInvalidQuantitiesWithoutCreatingCanonicalItems() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDir);
        CanonicalCraftIngredientDepositService service =
                new CanonicalCraftIngredientDepositService(repository, new WorldTaskCatalogue());

        CanonicalCraftIngredientDepositService.DepositResult result =
                service.deposit("player-1", "minecraft:wheat", 0);

        assertEquals(CanonicalCraftIngredientDepositService.DepositResult.Outcome.INVALID, result.outcome());
        assertTrue(repository.findItem(
                CanonicalCraftIngredientDepositService.stableStackId("player-1", "minecraft:wheat")).isEmpty());
    }
}
