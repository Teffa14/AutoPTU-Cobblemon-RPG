package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhaseFamilyPlaybackOrderCompatibilityTest {
    @Test
    void presentationPreservesAuthoritativeStatusAbilityPerkEventOrder() {
        BattlePlaybackBatch playback = new BattlePlaybackBatch(
                "reservation-phase-order",
                List.of(
                        ruleEffect(100, "status", "flinch", "status_phase"),
                        ruleEffect(101, "ability", "lancer", "ability_phase"),
                        ruleEffect(102, "perk", "example-perk", "perk_phase")
                )
        );

        BattlePresentationBatch presentation = new BattlePresentationProjector().project(playback);

        assertEquals(List.of(100L, 101L, 102L), presentation.commands().stream()
                .map(BattlePresentationCommand::sequence)
                .toList());
        assertEquals(List.of("status", "ability", "perk"), presentation.commands().stream()
                .map(command -> command.data().get("sourceKind"))
                .toList());
        assertEquals(List.of("status_phase", "ability_phase", "perk_phase"), presentation.commands().stream()
                .map(command -> command.data().get("effect"))
                .toList());
    }

    private static BattleEventPlaybackEnvelope ruleEffect(
            long sequence,
            String sourceKind,
            String sourceName,
            String effect
    ) {
        return new BattleEventPlaybackEnvelope(
                sequence,
                "rule_effect",
                "rule_effect|" + sourceKind + "|" + sourceName + "|pokemon-a|||" + effect + "|0.0|30",
                Map.of()
        );
    }
}
