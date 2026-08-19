package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalTrainerFeatureSnapshotTest {
    @Test
    void freezesServerOwnedTrainerFeaturesIntoBattleSnapshot() {
        LinkedHashSet<String> mutable = new LinkedHashSet<>(Set.of("Defense Mastery", "Stat Mastery"));
        CanonicalPlayerState state = new CanonicalPlayerState(
                "trainer-1",
                Set.of("Ace Trainer"),
                Map.of("command", 4),
                Set.of("Tracker"),
                mutable,
                7
        );

        BattleTrainerSnapshot snapshot = BattleTrainerSnapshot.from(state);
        mutable.clear();

        assertEquals(Set.of("Defense Mastery", "Stat Mastery"), snapshot.trainerFeatures());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.trainerFeatures().add("Attack Link"));
    }

    @Test
    void legacyPlayerConstructorCarriesNoInventedTrainerFeatures() {
        CanonicalPlayerState legacy = new CanonicalPlayerState(
                "trainer-1",
                Set.of("Ace Trainer"),
                Map.of("command", 4),
                Set.of("Tracker"),
                7
        );

        assertTrue(legacy.trainerFeatures().isEmpty());
        assertTrue(BattleTrainerSnapshot.from(legacy).trainerFeatures().isEmpty());
    }

    @Test
    void rejectsBlankOrCaseDuplicateTrainerFeatureIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new CanonicalPlayerState(
                "trainer-1", Set.of(), Map.of(), Set.of(), Set.of(" "), 0));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalPlayerState(
                "trainer-1", Set.of(), Map.of(), Set.of(), Set.of("Defense Mastery", "defense mastery"), 0));
    }
}
