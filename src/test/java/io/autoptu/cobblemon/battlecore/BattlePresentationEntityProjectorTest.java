package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattlePresentationEntityProjectorTest {
    private final BattlePresentationEntityProjector projector = new BattlePresentationEntityProjector();

    @Test
    void bindsHpAndRelocationToTheExactRegisteredPresentationEntity() {
        BattleAuthoritySnapshot snapshot = snapshot();
        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(
                snapshot, Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b"));

        List<EntityBoundBattleHealthProjection> health = projector.bindHealth(
                new BattleHealthProjectionBatch("battle-entity", List.of(
                        new BattleHealthProjection(10, 1, "pokemon-2", 24, 36))),
                bindings);
        assertEquals("pokemon-2", health.getFirst().combatantId());
        assertEquals("entity-b", health.getFirst().presentationEntityId());
        assertEquals(36, health.getFirst().targetHp());

        BattleArenaSnapshot arena = snapshot.arena();
        List<EntityBoundBattleWorldRelocation> relocations = projector.bindRelocations(
                new BattleWorldRelocationBatch("battle-entity", arena, List.of(
                        new BattleWorldRelocation(
                                11, 0, "pokemon-1",
                                new WorldBlockCoordinate("minecraft:overworld", 100, 64, 200),
                                new WorldBlockCoordinate("minecraft:overworld", 101, 64, 200)))),
                bindings);
        assertEquals("pokemon-1", relocations.getFirst().combatantId());
        assertEquals("entity-a", relocations.getFirst().presentationEntityId());
        assertEquals(101, relocations.getFirst().destination().x());
    }

    @Test
    void rejectsCrossReservationAndUnboundCombatantRedirection() {
        BattlePresentationEntityBindings bindings = BattlePresentationEntityBindings.bind(
                snapshot(), Map.of("pokemon-1", "entity-a", "pokemon-2", "entity-b"));

        assertThrows(IllegalArgumentException.class, () -> projector.bindHealth(
                new BattleHealthProjectionBatch("other-battle", List.of(
                        new BattleHealthProjection(1, 0, "pokemon-1", 1, 59))), bindings));

        assertThrows(IllegalArgumentException.class, () -> projector.bindHealth(
                new BattleHealthProjectionBatch("battle-entity", List.of(
                        new BattleHealthProjection(1, 0, "pokemon-injected", 1, 59))), bindings));
    }

    @Test
    void preservesExistingCompatibilityBoundariesInsteadOfAddingRuleAuthority() {
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY).support());
    }

    private static BattleAuthoritySnapshot snapshot() {
        BattleTrainerSnapshot trainer = new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 1);
        BattlePokemonSnapshot first = new BattlePokemonSnapshot(
                "pokemon-1", "player-1", "cobblemon:charizard", 40, Set.of("Sky"), null, 2);
        BattlePokemonSnapshot second = new BattlePokemonSnapshot(
                "pokemon-2", "player-1", "cobblemon:blastoise", 40, Set.of("Swim"), null, 2);
        BattleArenaSnapshot arena = new BattleArenaSnapshot(
                "minecraft:overworld", 100, 64, 200, 1, 0, 0, 1);
        return new BattleAuthoritySnapshot(
                "battle-entity", "player-1", trainer, List.of(first, second), List.of(), 1234L, arena);
    }
}
