package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistryBackedPresentationEntityGatewayTest {
    @Test
    void resolvesOnlyReservationScopedPresentationHandles() {
        PresentationEntityHandleRegistry<FakeEntity> registry = new PresentationEntityHandleRegistry<>();
        FakeEntity attacker = new FakeEntity("attacker");
        FakeEntity target = new FakeEntity("target");
        registry.register(" reservation-1 ", " entity-a ", attacker);
        registry.register("reservation-1", "entity-b", target);

        RecordingBackend backend = new RecordingBackend();
        RegistryBackedPresentationEntityGateway<FakeEntity> gateway =
                new RegistryBackedPresentationEntityGateway<>(registry, backend);

        gateway.animateMove("reservation-1", "entity-a", "entity-b", " Tackle ");
        gateway.projectDisplayedHealth("reservation-1", "entity-b", 22, 8);
        gateway.relocate(
                "reservation-1",
                "entity-a",
                new WorldBlockCoordinate("minecraft:overworld", 100, 64, 200),
                new WorldBlockCoordinate("minecraft:overworld", 101, 64, 200)
        );
        gateway.showCue(
                "reservation-1",
                "entity-b",
                new BattlePresentationCommand(
                        12,
                        0,
                        BattlePresentationCommand.Kind.TURN_START_CUE,
                        "pokemon-2",
                        Map.of("round", "2", "initiativeIndex", "1")
                )
        );

        assertEquals(List.of(
                "move:attacker:target:Tackle",
                "hp:target:22:8",
                "relocate:attacker:100,64,200->101,64,200",
                "cue:target:TURN_START_CUE:pokemon-2"
        ), backend.calls);
    }

    @Test
    void rejectsCrossReservationMissingAndDuplicateBindings() {
        PresentationEntityHandleRegistry<FakeEntity> registry = new PresentationEntityHandleRegistry<>();
        FakeEntity entity = new FakeEntity("entity");
        registry.register("reservation-1", "entity-a", entity);

        assertThrows(IllegalStateException.class,
                () -> registry.require("reservation-2", "entity-a"));
        assertThrows(IllegalStateException.class,
                () -> registry.register("reservation-1", "entity-b", entity));
        assertThrows(IllegalStateException.class,
                () -> registry.register("reservation-1", "entity-a", new FakeEntity("replacement")));

        registry.releaseReservation("reservation-1");
        assertEquals(0, registry.registeredCount("reservation-1"));
        assertThrows(IllegalStateException.class,
                () -> registry.require("reservation-1", "entity-a"));
    }

    @Test
    void gatewayFailsClosedBeforeCallingBackend() {
        PresentationEntityHandleRegistry<FakeEntity> registry = new PresentationEntityHandleRegistry<>();
        registry.register("reservation-1", "entity-a", new FakeEntity("entity-a"));
        RecordingBackend backend = new RecordingBackend();
        RegistryBackedPresentationEntityGateway<FakeEntity> gateway =
                new RegistryBackedPresentationEntityGateway<>(registry, backend);

        assertThrows(IllegalStateException.class,
                () -> gateway.animateMove("reservation-1", "entity-a", "entity-b", "Tackle"));
        assertThrows(IllegalArgumentException.class,
                () -> gateway.projectDisplayedHealth("reservation-1", "entity-a", -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> gateway.projectDisplayedHealth("reservation-1", "entity-a", 1, -1));
        assertThrows(NullPointerException.class,
                () -> gateway.showCue("reservation-1", "entity-a", null));
        assertEquals(List.of(), backend.calls);
    }

    private record FakeEntity(String id) {}

    private static final class RecordingBackend implements PresentationEntityPlatformBackend<FakeEntity> {
        private final List<String> calls = new ArrayList<>();

        @Override
        public void animateMove(FakeEntity attacker, FakeEntity target, String moveId) {
            calls.add("move:" + attacker.id() + ":" + target.id() + ":" + moveId);
        }

        @Override
        public void projectDisplayedHealth(FakeEntity entity, int targetHp, int damage) {
            calls.add("hp:" + entity.id() + ":" + targetHp + ":" + damage);
        }

        @Override
        public void relocate(FakeEntity entity, WorldBlockCoordinate origin, WorldBlockCoordinate destination) {
            calls.add("relocate:" + entity.id() + ":"
                    + origin.x() + "," + origin.y() + "," + origin.z() + "->"
                    + destination.x() + "," + destination.y() + "," + destination.z());
        }

        @Override
        public void showCue(FakeEntity entity, BattlePresentationCommand command) {
            calls.add("cue:" + entity.id() + ":" + command.kind() + ":" + command.subjectId());
        }
    }
}
