package io.autoptu.cobblemon.fabric.demo;

import io.autoptu.core.event.MoveResolvedEvent;
import io.autoptu.core.model.*;
import io.autoptu.core.random.PythonRandom;
import io.autoptu.core.rules.ActionBudget;
import io.autoptu.core.runtime.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayableBattleActionsTest {
    private static RuntimeCombatantState actor(String id, int x) {
        return new RuntimeCombatantState(id, MovementProfile.walking(new GridCoord(x, 1), 3), 60, 60, new ActionBudget());
    }

    @Test void distantTargetOnlyOffersLongRangeAttack() {
        var player = actor("player", 1);
        var enemy = actor("enemy", 5);
        var runtime = new BattleRuntimeState(new MovementGrid(7, 4, Set.of(), Map.of()), List.of(player, enemy));
        var moves = PlayableBattleTestRuntime.legalDemoMoves(runtime, player, enemy);
        assertEquals(List.of("demo-strike"), moves.stream().map(m -> m.moveId()).toList());
    }

    @Test void eachOfferedAttackResolvesItsOwnIdAndConsumesBudget() {
        for (String id : List.of("demo-strike", "demo-burst", "demo-arc")) {
            var player = actor("player", 2);
            var enemy = actor("enemy", 3);
            var runtime = new BattleRuntimeState(new MovementGrid(7, 4, Set.of(), Map.of()), List.of(player, enemy));
            var choice = PlayableBattleTestRuntime.legalDemoMoves(runtime, player, enemy).stream()
                    .filter(m -> m.moveId().equals(id)).findFirst().orElseThrow();
            var applied = BattleRuntime.applyAuthoritativeMove(runtime, choice,
                    PlayableBattleTestRuntime.demoMove(id), "Medium", "Medium", Set.of(), "Player",
                    new PythonRandom(20260823), PlayableBattleTestRuntime.demoMoveInput(id));
            var event = applied.events().stream().filter(MoveResolvedEvent.class::isInstance)
                    .map(MoveResolvedEvent.class::cast).findFirst().orElseThrow();
            assertEquals(id, event.moveId());
            assertEquals(event.targetHp(), enemy.hp());
            assertTrue(PlayableBattleTestRuntime.legalDemoMoves(runtime, player, enemy).isEmpty());
            player.actionBudget().resetConsumedActions();
            assertEquals(3, PlayableBattleTestRuntime.legalDemoMoves(runtime, player, enemy).size());
        }
    }
}
