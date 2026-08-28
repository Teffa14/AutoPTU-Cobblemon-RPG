package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldCampSetupServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void physicalCampOutcomePersistsAcrossRepositoryRestartWithoutReroll() {
        WorldTaskDefinition task = new WorldTaskCatalogue()
                .find(WorldTaskCatalogue.FIELD_CAMP_SETUP)
                .orElseThrow();
        String campId = "minecraft:overworld:10:64:-4";
        String attemptId = "field-camp:" + campId;
        AtomicInteger firstRolls = new AtomicInteger();
        FieldCampSetupService firstService = new FieldCampSetupService(
                new FileFieldCampSetupAttemptRepository(tempDir),
                new WorldTaskCompetenceService(),
                () -> {
                    firstRolls.incrementAndGet();
                    return 91;
                }
        );

        FieldCampSetupService.SetupResult established = firstService.establish(
                attemptId,
                campId,
                trainer("player-1", 4),
                task
        );

        assertEquals(FieldCampSetupService.Status.COMMITTED, established.status());
        assertEquals(FieldCampSetupAttempt.Quality.EXCELLENT, established.attempt().quality());
        assertEquals(4, established.attempt().canonicalSkillRank());
        assertEquals(1, firstRolls.get());

        AtomicInteger recoveryRolls = new AtomicInteger();
        FieldCampSetupService restartedService = new FieldCampSetupService(
                new FileFieldCampSetupAttemptRepository(tempDir),
                new WorldTaskCompetenceService(),
                () -> {
                    recoveryRolls.incrementAndGet();
                    return 1;
                }
        );
        FieldCampSetupService.SetupResult recovered = restartedService.establish(
                attemptId,
                campId,
                trainer("player-2", 0),
                task
        );

        assertEquals(FieldCampSetupService.Status.ALREADY_ESTABLISHED, recovered.status());
        assertEquals(FieldCampSetupAttempt.Quality.EXCELLENT, recovered.attempt().quality());
        assertEquals("player-1", recovered.attempt().establishedByPlayerId());
        assertEquals(4, recovered.attempt().canonicalSkillRank());
        assertEquals(0, recoveryRolls.get());
    }

    @Test
    void serverOwnedAttemptIdentityFailsClosedIfReboundToAnotherCamp() {
        WorldTaskDefinition task = new WorldTaskCatalogue()
                .find(WorldTaskCatalogue.FIELD_CAMP_SETUP)
                .orElseThrow();
        FieldCampSetupService service = new FieldCampSetupService(
                new FileFieldCampSetupAttemptRepository(tempDir),
                new WorldTaskCompetenceService(),
                () -> 50
        );
        String attemptId = "field-camp:minecraft:overworld:1:64:1";
        assertTrue(service.establish(
                attemptId,
                "minecraft:overworld:1:64:1",
                trainer("player-1", 2),
                task
        ).established());

        FieldCampSetupService.SetupResult conflict = service.establish(
                attemptId,
                "minecraft:overworld:2:64:2",
                trainer("player-1", 2),
                task
        );

        assertEquals(FieldCampSetupService.Status.ATTEMPT_CONFLICT, conflict.status());
    }

    private static CanonicalPlayerState trainer(String playerId, int survivalRank) {
        return new CanonicalPlayerState(
                playerId,
                Set.of(),
                Map.of("Survival", survivalRank),
                Set.of(),
                Set.of(),
                0,
                0,
                0,
                playerId,
                0
        );
    }
}
