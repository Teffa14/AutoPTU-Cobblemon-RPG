package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleChoiceMenuServiceTest {
    @Test
    void presentsOnlyChoicesFromTheAuthoritativeSnapshot() {
        BattleCoreLegalChoice.Shift shift = new BattleCoreLegalChoice.Shift(
                "mon-1", new BattleGridCoordinate(3, 4), "shift|mon-1|3,4");
        BattleCoreLegalChoice.Move move = new BattleCoreLegalChoice.Move(
                "mon-1", "tackle", BattleClientActionRequest.Target.Mode.COMBATANT,
                "mon-2", new BattleGridCoordinate(8, 9), "standard",
                "move|mon-1|tackle|combatant|mon-2|8,9|standard");
        BattleChoiceMenuService service = new BattleChoiceMenuService(
                (reservationId, actorId) -> new BattleCoreLegalChoiceSet(
                        reservationId, actorId, List.of(shift, move)),
                (reservationId, choice) -> { throw new AssertionError("display must not execute"); });

        List<BattleChoiceMenuService.Entry> entries = service.choices("battle-1", "mon-1");

        assertEquals(List.of(
                new BattleChoiceMenuService.Entry("shift|mon-1|3,4", "Shift to 3,4"),
                new BattleChoiceMenuService.Entry(
                        "move|mon-1|tackle|combatant|mon-2|8,9|standard",
                        "tackle -> combatant mon-2")
        ), entries);
    }

    @Test
    void refetchesAndExecutesTheExactCurrentChoiceForStableKey() {
        BattleCoreLegalChoice.Move current = new BattleCoreLegalChoice.Move(
                "mon-1", "ember", BattleClientActionRequest.Target.Mode.TILE,
                null, new BattleGridCoordinate(5, 6), "standard", "ember-tile-5-6");
        AtomicInteger sourceCalls = new AtomicInteger();
        AtomicReference<BattleCoreLegalChoice> executed = new AtomicReference<>();
        BattleChoiceMenuService service = new BattleChoiceMenuService(
                (reservationId, actorId) -> {
                    sourceCalls.incrementAndGet();
                    return new BattleCoreLegalChoiceSet(reservationId, actorId, List.of(current));
                },
                (reservationId, choice) -> executed.set(choice));

        BattleChoiceMenuService.Entry selected = service.choose("battle-1", "mon-1", "ember-tile-5-6");

        assertEquals(1, sourceCalls.get());
        assertEquals("ember-tile-5-6", selected.choiceId());
        assertSame(current, executed.get());
    }

    @Test
    void staleChoiceIdNeverReachesExecutor() {
        AtomicInteger executions = new AtomicInteger();
        BattleChoiceMenuService service = new BattleChoiceMenuService(
                (reservationId, actorId) -> new BattleCoreLegalChoiceSet(reservationId, actorId, List.of()),
                (reservationId, choice) -> executions.incrementAndGet());

        assertThrows(IllegalArgumentException.class,
                () -> service.choose("battle-1", "mon-1", "old-choice"));
        assertEquals(0, executions.get());
    }

    @Test
    void crossBattleOrCrossActorSnapshotFailsClosed() {
        AtomicInteger executions = new AtomicInteger();
        BattleCoreLegalChoice.Shift other = new BattleCoreLegalChoice.Shift(
                "mon-2", new BattleGridCoordinate(1, 2), "other");
        BattleChoiceMenuService service = new BattleChoiceMenuService(
                (reservationId, actorId) -> new BattleCoreLegalChoiceSet("battle-2", "mon-2", List.of(other)),
                (reservationId, choice) -> executions.incrementAndGet());

        assertThrows(IllegalStateException.class,
                () -> service.choose("battle-1", "mon-1", "other"));
        assertEquals(0, executions.get());
    }
}
