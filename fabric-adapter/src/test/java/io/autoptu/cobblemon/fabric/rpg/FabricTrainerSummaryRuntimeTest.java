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
}
