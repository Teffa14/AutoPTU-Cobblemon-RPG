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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricCraftingWorkstationRuntimeTest {
    @TempDir
    Path tempDirectory;

    @Test
    void selectorShowsUnderstoodRecipesAndMarksCanonicalMaterialReadiness() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDirectory);
        CanonicalPlayerState player = trainer("player-1", Map.of("Survival", 2));
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "wheat", player.playerId(), "minecraft:wheat", 2, 0)));
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "berries", player.playerId(), "minecraft:sweet_berries", 2, 0)));

        var options = FabricCraftingWorkstationRuntime.recipeOptions(
                player,
                player.playerId(),
                new WorldTaskCraftMaterialAssessmentService(items)
        );

        assertEquals(1, options.size());
        assertEquals("field_ration", options.getFirst().recipe().taskId());
        assertTrue(options.getFirst().materials().ready());
    }

    @Test
    void selectorKeepsKnownRecipeVisibleButDoesNotMarkItCraftableWithoutCanonicalMaterials() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDirectory);
        CanonicalPlayerState player = trainer("player-2", Map.of("Survival", 2));
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "wheat", player.playerId(), "minecraft:wheat", 2, 0)));

        var options = FabricCraftingWorkstationRuntime.recipeOptions(
                player,
                player.playerId(),
                new WorldTaskCraftMaterialAssessmentService(items)
        );

        assertEquals(1, options.size());
        assertFalse(options.getFirst().materials().ready());
        assertEquals("2x minecraft:sweet_berries", FabricCraftingWorkstationRuntime.missingMaterials(
                options.getFirst().materials()));
    }

    @Test
    void selectorDoesNotExposeRecipesAboveTheTrainersKnowledgeRank() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDirectory);
        CanonicalPlayerState player = trainer("player-3", Map.of());

        var options = FabricCraftingWorkstationRuntime.recipeOptions(
                player,
                player.playerId(),
                new WorldTaskCraftMaterialAssessmentService(items)
        );

        assertEquals(1, options.size());
        assertEquals("field_ration", options.getFirst().recipe().taskId());
        assertTrue(options.stream().noneMatch(option -> "occult_lure".equals(option.recipe().taskId())));
        assertTrue(options.stream().noneMatch(option -> "precision_poketech_parts".equals(option.recipe().taskId())));
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
