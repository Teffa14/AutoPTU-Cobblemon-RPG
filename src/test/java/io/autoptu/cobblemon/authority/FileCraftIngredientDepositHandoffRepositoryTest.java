package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCraftIngredientDepositHandoffRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsPendingHandoffAcrossRepositoryRestartAndAdvancesByExpectedPhase() {
        CraftIngredientDepositHandoff prepared = new CraftIngredientDepositHandoff(
                "handoff-1", "player-1", "minecraft:wheat", 2, 5, 1,
                CraftIngredientDepositHandoff.Phase.PREPARED);

        FileCraftIngredientDepositHandoffRepository first =
                new FileCraftIngredientDepositHandoffRepository(tempDir);
        assertTrue(first.createIfAbsent(prepared));
        assertFalse(first.createIfAbsent(prepared));

        FileCraftIngredientDepositHandoffRepository restarted =
                new FileCraftIngredientDepositHandoffRepository(tempDir);
        List<CraftIngredientDepositHandoff> pending = restarted.findPendingForPlayer("player-1");
        assertEquals(List.of(prepared), pending);

        CraftIngredientDepositHandoff withdrawn = prepared.withPhase(CraftIngredientDepositHandoff.Phase.WITHDRAWN);
        assertTrue(restarted.replaceIfPhase(
                prepared.handoffId(), CraftIngredientDepositHandoff.Phase.PREPARED, withdrawn));
        assertFalse(restarted.replaceIfPhase(
                prepared.handoffId(), CraftIngredientDepositHandoff.Phase.PREPARED,
                prepared.withPhase(CraftIngredientDepositHandoff.Phase.ABORTED)));

        CraftIngredientDepositHandoff committed = withdrawn.withPhase(CraftIngredientDepositHandoff.Phase.COMMITTED);
        assertTrue(restarted.replaceIfPhase(
                prepared.handoffId(), CraftIngredientDepositHandoff.Phase.WITHDRAWN, committed));
        assertTrue(restarted.findPendingForPlayer("player-1").isEmpty());
        assertEquals(CraftIngredientDepositHandoff.Phase.COMMITTED,
                restarted.find("handoff-1").orElseThrow().phase());
    }
}
