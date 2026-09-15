package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleChoiceVisualPlanTest {
    @Test
    void separatesMovementAndAttackAnchorsWithoutInventingSelfOrFieldTiles() {
        BattleGridCoordinate shift = new BattleGridCoordinate(2, 3);
        BattleGridCoordinate target = new BattleGridCoordinate(5, 4);
        BattleCoreLegalChoiceSet choices = choices(List.of(
                new BattleCoreLegalChoice.Shift("actor", shift, "shift-1"),
                move("ember", BattleClientActionRequest.Target.Mode.COMBATANT, "enemy", target, "move-1"),
                move("focus", BattleClientActionRequest.Target.Mode.SELF, null, new BattleGridCoordinate(99, 99), "move-2"),
                move("rain", BattleClientActionRequest.Target.Mode.FIELD, null, new BattleGridCoordinate(88, 88), "move-3")
        ));

        BattleChoiceVisualPlan plan = BattleChoiceVisualPlan.from(choices, null);

        assertEquals(Set.of(shift), plan.shiftDestinations());
        assertEquals(Set.of(target), plan.attackTargets());
        assertEquals(new BattleChoiceVisualPlan.GridWindow(1, 6, 2, 5), plan.gridWindow());
        assertNull(plan.highlight());
    }

    @Test
    void highlightsOnlyAnExactAuthoritativeChoice() {
        BattleGridCoordinate target = new BattleGridCoordinate(4, 6);
        BattleCoreLegalChoiceSet choices = choices(List.of(
                new BattleCoreLegalChoice.Shift("actor", new BattleGridCoordinate(2, 2), "shift-1"),
                move("ember", BattleClientActionRequest.Target.Mode.TILE, null, target, "move-1")
        ));

        BattleChoiceVisualPlan plan = BattleChoiceVisualPlan.from(choices, "move-1");

        assertEquals(BattleChoiceVisualPlan.HighlightKind.ATTACK, plan.highlight().kind());
        assertEquals(target, plan.highlight().anchor());
        assertEquals("ember", plan.highlight().moveId());
        assertThrows(IllegalArgumentException.class, () -> BattleChoiceVisualPlan.from(choices, "stale-choice"));
    }

    @Test
    void boundsLargeAuthoritativeSpacesAroundTheSelectedCell() {
        BattleCoreLegalChoiceSet choices = choices(List.of(
                new BattleCoreLegalChoice.Shift("actor", new BattleGridCoordinate(-100, -100), "far-a"),
                new BattleCoreLegalChoice.Shift("actor", new BattleGridCoordinate(100, 100), "focus")
        ));

        BattleChoiceVisualPlan plan = BattleChoiceVisualPlan.from(choices, "focus");

        assertEquals(12, plan.gridWindow().width());
        assertEquals(12, plan.gridWindow().height());
        assertEquals(100, plan.gridWindow().minX() + 5);
        assertEquals(100, plan.gridWindow().minY() + 5);
    }

    @Test
    void executedHighlightSurvivesTheNextChoiceSetForPresentationOnly() {
        BattleCoreLegalChoice.Shift executed = new BattleCoreLegalChoice.Shift(
                "actor", new BattleGridCoordinate(3, 4), "shift-1");
        BattleChoiceVisualPlan next = BattleChoiceVisualPlan.from(
                choices(List.of(move("ember", BattleClientActionRequest.Target.Mode.FIELD, null,
                        new BattleGridCoordinate(0, 0), "move-2"))), null);

        BattleChoiceVisualPlan receipt = next.withExecutedHighlight(executed);

        assertEquals(BattleChoiceVisualPlan.HighlightKind.MOVEMENT, receipt.highlight().kind());
        assertEquals(new BattleGridCoordinate(3, 4), receipt.highlight().anchor());
        assertEquals(new BattleChoiceVisualPlan.GridWindow(2, 4, 3, 5), receipt.gridWindow());
    }

    private static BattleCoreLegalChoiceSet choices(List<BattleCoreLegalChoice> choices) {
        return new BattleCoreLegalChoiceSet("reservation", "actor", choices);
    }

    private static BattleCoreLegalChoice.Move move(
            String moveId,
            BattleClientActionRequest.Target.Mode mode,
            String targetId,
            BattleGridCoordinate anchor,
            String key
    ) {
        return new BattleCoreLegalChoice.Move("actor", moveId, mode, targetId, anchor, "STANDARD", key);
    }
}
