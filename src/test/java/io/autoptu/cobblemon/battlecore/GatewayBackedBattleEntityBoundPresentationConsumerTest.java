package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayBackedBattleEntityBoundPresentationConsumerTest {
    @Test
    void forwardsOnlyAlreadyBoundAuthoritativePresentationValues() {
        RecordingGateway gateway = new RecordingGateway();
        GatewayBackedBattleEntityBoundPresentationConsumer consumer =
                new GatewayBackedBattleEntityBoundPresentationConsumer(gateway);

        BattlePresentationCommand moveCommand = new BattlePresentationCommand(
                10, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, "pokemon-1",
                Map.of("targetId", "pokemon-2", "moveId", "Tackle"));
        consumer.animateMove(" reservation-1 ",
                new EntityBoundMoveAnimation(moveCommand, "entity-a", "entity-b"));

        consumer.projectHealth("reservation-1",
                new EntityBoundBattleHealthProjection(10, 1, "pokemon-2", "entity-b", 8, 22));

        WorldBlockCoordinate origin = new WorldBlockCoordinate("minecraft:overworld", 100, 64, 200);
        WorldBlockCoordinate destination = new WorldBlockCoordinate("minecraft:overworld", 101, 64, 200);
        consumer.relocateEntity("reservation-1",
                new EntityBoundBattleWorldRelocation(
                        11, 0, "pokemon-1", "entity-a", origin, destination));

        BattlePresentationCommand cueCommand = new BattlePresentationCommand(
                12, 0, BattlePresentationCommand.Kind.TURN_START_CUE, "pokemon-2",
                Map.of("round", "2", "initiativeIndex", "1"));
        consumer.showCombatantCue("reservation-1",
                new EntityBoundBattlePresentationCommand(cueCommand, "entity-b"));

        assertEquals(List.of(
                "move:reservation-1:entity-a:entity-b:Tackle",
                "hp:reservation-1:entity-b:22:8",
                "relocate:reservation-1:entity-a:100,64,200->101,64,200",
                "cue:reservation-1:entity-b:TURN_START_CUE:pokemon-2"
        ), gateway.calls);
    }

    @Test
    void rejectsMissingGatewayReservationOrBoundOutput() {
        assertThrows(NullPointerException.class,
                () -> new GatewayBackedBattleEntityBoundPresentationConsumer(null));

        GatewayBackedBattleEntityBoundPresentationConsumer consumer =
                new GatewayBackedBattleEntityBoundPresentationConsumer(new RecordingGateway());
        EntityBoundBattleHealthProjection health =
                new EntityBoundBattleHealthProjection(1, 0, "pokemon-1", "entity-a", 0, 20);

        assertThrows(IllegalArgumentException.class, () -> consumer.projectHealth(" ", health));
        assertThrows(NullPointerException.class, () -> consumer.projectHealth("reservation-1", null));
        assertThrows(NullPointerException.class, () -> consumer.animateMove("reservation-1", null));
        assertThrows(NullPointerException.class, () -> consumer.relocateEntity("reservation-1", null));
        assertThrows(NullPointerException.class, () -> consumer.showCombatantCue("reservation-1", null));
    }

    private static final class RecordingGateway implements PresentationEntityGateway {
        private final List<String> calls = new ArrayList<>();

        @Override
        public void animateMove(
                String reservationId,
                String attackerPresentationEntityId,
                String targetPresentationEntityId,
                String moveId) {
            calls.add("move:" + reservationId + ":" + attackerPresentationEntityId + ":"
                    + targetPresentationEntityId + ":" + moveId);
        }

        @Override
        public void projectDisplayedHealth(
                String reservationId,
                String presentationEntityId,
                int targetHp,
                int damage) {
            calls.add("hp:" + reservationId + ":" + presentationEntityId + ":" + targetHp + ":" + damage);
        }

        @Override
        public void relocate(
                String reservationId,
                String presentationEntityId,
                WorldBlockCoordinate origin,
                WorldBlockCoordinate destination) {
            calls.add("relocate:" + reservationId + ":" + presentationEntityId + ":"
                    + origin.x() + "," + origin.y() + "," + origin.z() + "->"
                    + destination.x() + "," + destination.y() + "," + destination.z());
        }

        @Override
        public void showCue(
                String reservationId,
                String presentationEntityId,
                BattlePresentationCommand command) {
            calls.add("cue:" + reservationId + ":" + presentationEntityId + ":"
                    + command.kind() + ":" + command.subjectId());
        }
    }
}
