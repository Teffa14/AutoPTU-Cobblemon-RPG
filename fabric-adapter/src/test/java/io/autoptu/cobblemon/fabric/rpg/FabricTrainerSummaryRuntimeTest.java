package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalTrainerSummaryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FabricTrainerSummaryRuntimeTest {
    @Test
    void formatsCanonicalSkillsAsDedicatedStableReadSurface() {
        CanonicalTrainerSummaryService.Summary summary = new CanonicalTrainerSummaryService.Summary(
                "minecraft-player:test",
                List.of("Researcher"),
                List.of(
                        new CanonicalTrainerSummaryService.Skill("Command", 2),
                        new CanonicalTrainerSummaryService.Skill("Survival", 4)),
                List.of("Focused Training"),
                List.of("Tracker"),
                2,
                1,
                14,
                "cedar-team",
                9L);

        assertEquals(List.of(
                "AutoPTU Trainer skills",
                "Command: 2",
                "Survival: 4",
                "Revision: 9"), FabricTrainerSummaryRuntime.formatSkillLines(summary));
    }

    @Test
    void reportsMissingCanonicalSkillsWithoutInventingPtuRanks() {
        CanonicalTrainerSummaryService.Summary summary = new CanonicalTrainerSummaryService.Summary(
                "minecraft-player:test",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                0,
                null,
                "",
                3L);

        assertEquals(List.of(
                "AutoPTU Trainer skills",
                "No canonical Trainer skills are available.",
                "Revision: 3"), FabricTrainerSummaryRuntime.formatSkillLines(summary));
    }

    @Test
    void formatsCanonicalClassesAsDedicatedStableReadSurface() {
        CanonicalTrainerSummaryService.Summary summary = new CanonicalTrainerSummaryService.Summary(
                "minecraft-player:test",
                List.of("Researcher", "Survivalist"),
                List.of(),
                List.of("Focused Training"),
                List.of(),
                2,
                1,
                14,
                "cedar-team",
                12L);

        assertEquals(List.of(
                "AutoPTU Trainer classes",
                "Researcher",
                "Survivalist",
                "Revision: 12"), FabricTrainerSummaryRuntime.formatClassLines(summary));
    }

    @Test
    void reportsMissingCanonicalClassesWithoutDerivingPtuClassState() {
        CanonicalTrainerSummaryService.Summary summary = new CanonicalTrainerSummaryService.Summary(
                "minecraft-player:test",
                List.of(),
                List.of(new CanonicalTrainerSummaryService.Skill("Survival", 4)),
                List.of("Survivalist"),
                List.of(),
                0,
                0,
                null,
                "",
                4L);

        assertEquals(List.of(
                "AutoPTU Trainer classes",
                "No canonical Trainer classes are available.",
                "Revision: 4"), FabricTrainerSummaryRuntime.formatClassLines(summary));
    }

    @Test
    void formatsCanonicalFeaturesAsDedicatedStableReadSurface() {
        CanonicalTrainerSummaryService.Summary summary = new CanonicalTrainerSummaryService.Summary(
                "minecraft-player:test",
                List.of("Researcher"),
                List.of(new CanonicalTrainerSummaryService.Skill("Command", 2)),
                List.of("Focused Training", "Let Me Help You With That"),
                List.of("Tracker"),
                2,
                1,
                14,
                "cedar-team",
                15L);

        assertEquals(List.of(
                "AutoPTU Trainer features",
                "Focused Training",
                "Let Me Help You With That",
                "Revision: 15"), FabricTrainerSummaryRuntime.formatFeatureLines(summary));
    }

    @Test
    void reportsMissingCanonicalFeaturesWithoutDerivingPtuFeatureState() {
        CanonicalTrainerSummaryService.Summary summary = new CanonicalTrainerSummaryService.Summary(
                "minecraft-player:test",
                List.of("Survivalist"),
                List.of(new CanonicalTrainerSummaryService.Skill("Survival", 4)),
                List.of(),
                List.of("Tracker"),
                0,
                0,
                null,
                "",
                5L);

        assertEquals(List.of(
                "AutoPTU Trainer features",
                "No canonical Trainer features are available.",
                "Revision: 5"), FabricTrainerSummaryRuntime.formatFeatureLines(summary));
    }
}
