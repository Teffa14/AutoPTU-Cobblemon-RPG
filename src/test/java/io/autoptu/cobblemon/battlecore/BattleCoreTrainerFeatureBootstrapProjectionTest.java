package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import io.autoptu.cobblemon.authority.CanonicalMoveLoadout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCoreTrainerFeatureBootstrapProjectionTest {
    @Test
    void projectsFrozenTrainerFeatureIdentityWithoutExecutingPerkRules() {
        BattleAuthoritySnapshot battle = battle(Set.of("Defense Mastery", "Stat Mastery"));

        BattleCoreTrainerFeatureBootstrapProjection projection =
                BattleCoreTrainerFeatureBootstrapProjection.from(battle);

        assertEquals("battle-features", projection.reservationId());
        assertEquals("player-1", projection.trainer().trainerId());
        assertEquals(Set.of("Defense Mastery", "Stat Mastery"), projection.trainer().trainerFeatures());
        assertThrows(UnsupportedOperationException.class,
                () -> projection.trainer().trainerFeatures().add("Attack Link"));
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_TRAINER_FEATURE_BOOTSTRAP)
                .hasBlockingDependency());
    }

    @Test
    void legacyTrainerSnapshotCarriesNoInventedFeatures() {
        BattleAuthoritySnapshot battle = new BattleAuthoritySnapshot(
                "battle-features",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 3),
                List.of(pokemon()),
                List.of(),
                991L
        );

        assertEquals(Set.of(), BattleCoreTrainerFeatureBootstrapProjection.from(battle)
                .trainer().trainerFeatures());
    }

    private static BattleAuthoritySnapshot battle(Set<String> features) {
        return new BattleAuthoritySnapshot(
                "battle-features",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of("Ace Trainer"), Map.of("command", 4), features, 3),
                List.of(pokemon()),
                List.of(),
                991L
        );
    }

    private static BattlePokemonSnapshot pokemon() {
        return new BattlePokemonSnapshot(
                "pokemon-a",
                "player-1",
                "cobblemon:test",
                10,
                Set.of("Overland 5"),
                Set.of(),
                new CanonicalCombatStats(10, 10, 10, 10, 10),
                new CanonicalHealth(20, 20),
                new CanonicalMoveLoadout(List.of("Tackle")),
                null,
                1
        );
    }
}
