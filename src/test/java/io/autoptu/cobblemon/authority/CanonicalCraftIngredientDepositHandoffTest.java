package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalCraftIngredientDepositHandoffTest {
    @TempDir
    Path tempDir;

    @Test
    void retryingSameHandoffCannotDuplicateCanonicalMaterial() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDir);
        CanonicalCraftIngredientDepositService service =
                new CanonicalCraftIngredientDepositService(repository, new WorldTaskCatalogue());

        CanonicalCraftIngredientDepositService.DepositResult first =
                service.depositHandoff("handoff-1", "player-1", "minecraft:wheat", 1);
        CanonicalCraftIngredientDepositService.DepositResult retry =
                service.depositHandoff("handoff-1", "player-1", "minecraft:wheat", 1);

        assertEquals(CanonicalCraftIngredientDepositService.DepositResult.Outcome.APPLIED, first.outcome());
        assertEquals(CanonicalCraftIngredientDepositService.DepositResult.Outcome.ALREADY_APPLIED, retry.outcome());
        assertTrue(retry.applied());
        assertEquals(1, repository.findReservableItems("player-1", "minecraft:wheat").stream()
                .mapToInt(CanonicalItemInstance::quantity)
                .sum());
    }

    @Test
    void distinctHandoffsRemainIndependentlyConsumableCanonicalStacks() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDir);
        CanonicalCraftIngredientDepositService service =
                new CanonicalCraftIngredientDepositService(repository, new WorldTaskCatalogue());

        service.depositHandoff("handoff-1", "player-1", "minecraft:wheat", 1);
        service.depositHandoff("handoff-2", "player-1", "minecraft:wheat", 1);

        assertEquals(2, repository.findReservableItems("player-1", "minecraft:wheat").stream()
                .mapToInt(CanonicalItemInstance::quantity)
                .sum());
    }
}
