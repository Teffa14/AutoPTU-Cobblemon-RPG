package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleHealthProjectionProjectorTest {
    private final BattleHealthProjectionProjector projector = new BattleHealthProjectionProjector();

    @Test
    void bindsAuthoritativeHpProjectionToFrozenReservationAndRoster() {
        BattleAuthoritySnapshot snapshot = snapshot();
        BattlePresentationBatch presentation = new BattlePresentationBatch("battle-health", List.of(
                new BattlePresentationCommand(
                        10, 0, BattlePresentationCommand.Kind.MOVE_ANIMATION, "pokemon-1",
                        Map.of("targetId", "pokemon-2", "moveId", "water pulse")),
                new BattlePresentationCommand(
                        10, 1, BattlePresentationCommand.Kind.HP_PROJECTION, "pokemon-2",
                        Map.of("damage", "24", "targetHp", "36"))));

        BattleHealthProjectionBatch result = projector.project(snapshot, presentation);

        assertEquals("battle-health", result.reservationId());
        assertEquals(1, result.healthUpdates().size());
        BattleHealthProjection hp = result.healthUpdates().getFirst();
        assertEquals(10, hp.sequence());
        assertEquals(1, hp.ordinal());
        assertEquals("pokemon-2", hp.combatantId());
        assertEquals(24, hp.damage());
        assertEquals(36, hp.targetHp());
    }

    @Test
    void rejectsCrossReservationAndInjectedCombatantHpProjection() {
        BattleAuthoritySnapshot snapshot = snapshot();
        BattlePresentationCommand hp = new BattlePresentationCommand(
                1, 0, BattlePresentationCommand.Kind.HP_PROJECTION, "pokemon-2",
                Map.of("damage", "5", "targetHp", "55"));

        assertThrows(IllegalArgumentException.class, () -> projector.project(
                snapshot, new BattlePresentationBatch("other-battle", List.of(hp))));

        BattlePresentationCommand injected = new BattlePresentationCommand(
                1, 0, BattlePresentationCommand.Kind.HP_PROJECTION, "pokemon-injected",
                Map.of("damage", "5", "targetHp", "55"));
        assertThrows(IllegalArgumentException.class, () -> projector.project(
                snapshot, new BattlePresentationBatch("battle-health", List.of(injected))));
    }

    @Test
    void rejectsMalformedHealthPayloadInsteadOfInventingDisplayState() {
        BattleAuthoritySnapshot snapshot = snapshot();
        BattlePresentationCommand missingHp = new BattlePresentationCommand(
                1, 0, BattlePresentationCommand.Kind.HP_PROJECTION, "pokemon-2",
                Map.of("damage", "5"));
        BattlePresentationCommand negativeDamage = new BattlePresentationCommand(
                2, 0, BattlePresentationCommand.Kind.HP_PROJECTION, "pokemon-2",
                Map.of("damage", "-1", "targetHp", "55"));

        assertThrows(IllegalArgumentException.class, () -> projector.project(
                snapshot, new BattlePresentationBatch("battle-health", List.of(missingHp))));
        assertThrows(IllegalArgumentException.class, () -> projector.project(
                snapshot, new BattlePresentationBatch("battle-health", List.of(negativeDamage))));
    }

    @Test
    void remainsBoundToPartialAuthoritativeDamageCapability() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.DAMAGE_RESULT_PLAYBACK);
        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);

        assertFalse(requirement.hasBlockingDependency());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
    }

    private static BattleAuthoritySnapshot snapshot() {
        BattleTrainerSnapshot trainer = new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 1);
        BattlePokemonSnapshot first = new BattlePokemonSnapshot(
                "pokemon-1", "player-1", "cobblemon:charizard", 40, Set.of("Sky"), null, 2);
        BattlePokemonSnapshot second = new BattlePokemonSnapshot(
                "pokemon-2", "player-1", "cobblemon:blastoise", 40, Set.of("Swim"), null, 2);
        return new BattleAuthoritySnapshot(
                "battle-health", "player-1", trainer, List.of(first, second), List.of(), 1234L);
    }
}
