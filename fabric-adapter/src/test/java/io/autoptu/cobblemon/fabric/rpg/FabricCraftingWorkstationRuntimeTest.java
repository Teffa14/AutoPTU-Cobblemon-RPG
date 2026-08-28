package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalItemInstance;
import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.FileCanonicalItemReservationRepository;
import io.autoptu.cobblemon.authority.WorldTaskCraftMaterialAssessmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricCraftingWorkstationRuntimeTest {
    @TempDir
    Path tempDirectory;

    @Test
    void selectsOnlyAnUnderstoodRecipeWithServerOwnedCanonicalMaterials() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDirectory);
        CanonicalPlayerState player = trainer("player-1", Map.of("Survival", 2));
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "wheat", player.playerId(), "minecraft:wheat", 2, 0)));
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "berries", player.playerId(), "minecraft:sweet_berries", 2, 0)));

        var selected = FabricCraftingWorkstationRuntime.firstReadyRecipe(
                player,
                player.playerId(),
                new WorldTaskCraftMaterialAssessmentService(items)
        );

        assertTrue(selected.isPresent());
        assertEquals("field_ration", selected.orElseThrow().taskId());
    }

    @Test
    void refusesToSelectARecipeWhenCanonicalMaterialsAreIncomplete() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDirectory);
        CanonicalPlayerState player = trainer("player-2", Map.of("Survival", 2));
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "wheat", player.playerId(), "minecraft:wheat", 2, 0)));

        var selected = FabricCraftingWorkstationRuntime.firstReadyRecipe(
                player,
                player.playerId(),
                new WorldTaskCraftMaterialAssessmentService(items)
        );

        assertTrue(selected.isEmpty());
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
