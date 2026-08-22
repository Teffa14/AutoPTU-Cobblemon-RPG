package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerRoundLifecycleAuthorityGuardTest {
    @Test
    void runtimeAssemblyCannotAcceptTrainerRoundMutationInputs() {
        Set<String> components = Arrays.stream(BattleRuntimeAssemblySeed.class.getRecordComponents())
                .map(RecordComponent::getName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        for (String forbidden : Set.of(
                "temporaryap", "temporaryapgrants", "temporaryapexpiry", "expiresround",
                "traineractionreset", "resettraineractions", "traineractionbudget",
                "roundstarttrainerhook", "trainerroundlifecycle", "currentround")) {
            assertFalse(components.contains(forbidden),
                    () -> "integration must not accept core-owned Trainer round input: " + forbidden);
        }
    }

    @Test
    void currentInspectionKeepsTrainerRoundLifecycleBoundedAndCoreOwned() {
        CurrentUpstreamCompatibilityInspection.Evidence lifecycle =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE);
        CurrentUpstreamCompatibilityInspection.Evidence actionEconomy =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE);
        CurrentUpstreamCompatibilityInspection.Evidence perks =
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS);

        assertTrue(lifecycle.contracts().contains("TemporaryApGrant"));
        assertTrue(lifecycle.contracts().contains("Trainer action reset at order 40"));
        assertTrue(lifecycle.limitation().contains("temporary AP grants"));
        assertTrue(lifecycle.limitation().contains("Trainer action reset"));

        assertTrue(actionEconomy.contracts().contains("TrainerRuntimeState"));
        assertTrue(actionEconomy.limitation().contains("temporary AP grants/expiry"));

        assertTrue(perks.contracts().contains("temporary AP grants"));
        assertTrue(perks.limitation().contains("choose AP grant expiry/source"));
    }
}
