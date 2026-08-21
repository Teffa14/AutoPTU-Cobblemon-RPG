package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleEntityBoundPresentationDispatcherTest {
    @Test
    void dispatchesEveryBoundOutputInAuthoritativeOrder() {
        BattlePresentationCommand moveCommand = new BattlePresentationCommand(
                10, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, "pokemon-1",
                Map.of("targetId", "pokemon-2", "moveId", "tackle"));
        EntityBoundMoveAnimation move = new EntityBoundMoveAnimation(moveCommand, "entity-1", "entity-2");
        EntityBoundBattleHealthProjection health = new EntityBoundBattleHealthProjection(
                10, 1, "pokemon-2", "entity-2", 7, 23);
        EntityBoundBattleWorldRelocation relocation = new EntityBoundBattleWorldRelocation(
                11, 0, "pokemon-1", "entity-1",
                new WorldBlockCoordinate("overworld", 0, 64, 0),
                new WorldBlockCoordinate("overworld", 1, 64, 0));
        EntityBoundBattlePresentationCommand cue = new EntityBoundBattlePresentationCommand(
                new BattlePresentationCommand(12, 0, BattlePresentationCommand.Kind.TURN_START_CUE,
                        "pokemon-2", Map.of("round", "2", "initiativeIndex", "1")),
                "entity-2");

        BattleEntityBoundPresentationStream stream = new BattleEntityBoundPresentationStream(
                "reservation-1", List.of(move, health, relocation, cue));
        RecordingConsumer consumer = new RecordingConsumer();

        BattleEntityBoundPresentationDispatcher.dispatch(stream, consumer);

        assertEquals(List.of(
                "move:reservation-1:entity-1:entity-2:tackle",
                "hp:reservation-1:entity-2:23",
                "relocate:reservation-1:entity-1:1,64,0",
                "cue:reservation-1:entity-2:TURN_START_CUE"
        ), consumer.calls);
    }

    @Test
    void rejectsMissingStreamOrConsumer() {
        BattleEntityBoundPresentationStream stream = new BattleEntityBoundPresentationStream("reservation-1", List.of());
        RecordingConsumer consumer = new RecordingConsumer();
        assertThrows(NullPointerException.class, () -> BattleEntityBoundPresentationDispatcher.dispatch(null, consumer));
        assertThrows(NullPointerException.class, () -> BattleEntityBoundPresentationDispatcher.dispatch(stream, null));
    }

    private static final class RecordingConsumer implements BattleEntityBoundPresentationConsumer {
        private final List<String> calls = new ArrayList<>();

        @Override
        public void animateMove(String reservationId, EntityBoundMoveAnimation animation) {
            calls.add("move:" + reservationId + ":" + animation.attackerPresentationEntityId()
                    + ":" + animation.targetPresentationEntityId() + ":" + animation.moveId());
        }

        @Override
        public void projectHealth(String reservationId, EntityBoundBattleHealthProjection health) {
            calls.add("hp:" + reservationId + ":" + health.presentationEntityId() + ":" + health.targetHp());
        }

        @Override
        public void relocateEntity(String reservationId, EntityBoundBattleWorldRelocation relocation) {
            WorldBlockCoordinate destination = relocation.destination();
            calls.add("relocate:" + reservationId + ":" + relocation.presentationEntityId() + ":"
                    + destination.x() + "," + destination.y() + "," + destination.z());
        }

        @Override
        public void showCombatantCue(String reservationId, EntityBoundBattlePresentationCommand cue) {
            calls.add("cue:" + reservationId + ":" + cue.presentationEntityId() + ":" + cue.command().kind());
        }
    }
}
