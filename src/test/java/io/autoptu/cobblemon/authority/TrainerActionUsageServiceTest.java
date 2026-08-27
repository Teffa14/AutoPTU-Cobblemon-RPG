package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class TrainerActionUsageServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void serverCanonicalFeatureOwnershipIsRequired() {
        TrainerActionUsageService service = service();
        CanonicalPlayerState player = player(Set.of("owned-feature"));
        TrainerActionUsageDecision decision = service.reserveFeatureUse(
                player,
                new CanonicalTrainerActionRule("not-owned", TrainerActionFrequency.DAILY, 1),
                "op",
                null,
                3
        );
        assertEquals(TrainerActionUsageDecision.Status.FEATURE_NOT_OWNED, decision.status());
    }

    @Test
    void atWillDoesNotCreateDurableUseButStillObservesWorldDay() {
        FileTrainerActionUsageLedger ledger = new FileTrainerActionUsageLedger(tempDirectory);
        TrainerActionUsageService service = new TrainerActionUsageService(
                ledger,
                Clock.fixed(Instant.ofEpochMilli(4_000L), ZoneOffset.UTC)
        );
        TrainerActionUsageDecision decision = service.reserveFeatureUse(
                player(Set.of("always-ready")),
                new CanonicalTrainerActionRule("always-ready", TrainerActionFrequency.AT_WILL, 1),
                "op-at-will",
                null,
                20
        );
        assertEquals(TrainerActionUsageDecision.Status.ALLOWED_AT_WILL, decision.status());
        assertTrue(decision.reservation().isEmpty());
        assertEquals(20L, ledger.highestObservedOverworldDay());
    }

    @Test
    void turnAndRoundFrequencyStayBattleCoreOwned() {
        TrainerActionUsageService service = service();
        CanonicalPlayerState player = player(Set.of("turn-action", "round-action"));
        assertEquals(
                TrainerActionUsageDecision.Status.BATTLE_CORE_OWNED,
                service.reserveFeatureUse(
                        player,
                        new CanonicalTrainerActionRule("turn-action", TrainerActionFrequency.TURN, 1),
                        "turn-op",
                        null,
                        1
                ).status()
        );
        assertEquals(
                TrainerActionUsageDecision.Status.BATTLE_CORE_OWNED,
                service.reserveFeatureUse(
                        player,
                        new CanonicalTrainerActionRule("round-action", TrainerActionFrequency.ROUND, 1),
                        "round-op",
                        null,
                        1
                ).status()
        );
    }

    private TrainerActionUsageService service() {
        return new TrainerActionUsageService(
                new FileTrainerActionUsageLedger(tempDirectory),
                Clock.fixed(Instant.ofEpochMilli(2_000L), ZoneOffset.UTC)
        );
    }

    private static CanonicalPlayerState player(Set<String> features) {
        return new CanonicalPlayerState(
                "player-a",
                Set.of(),
                Map.of(),
                Set.of(),
                features,
                1L
        );
    }
}
