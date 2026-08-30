package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalTrainerSummaryServiceTest {
    @Test
    void projectsOnlyCanonicalTrainerStateInStableOrder() {
        CanonicalPlayerState state = new CanonicalPlayerState(
                "minecraft-player:test",
                Set.of("Ace Trainer", "Researcher"),
                Map.of("Survival", 3, "Command", 2),
                Set.of("Mount", "Tracker"),
                Set.of("Focused Training", "Agility Training"),
                2,
                1,
                14,
                "cedar-team",
                9L);
        CanonicalStateRepository repository = playerId -> playerId.equals(state.playerId())
                ? Optional.of(state)
                : Optional.empty();

        CanonicalTrainerSummaryService.Summary summary = new CanonicalTrainerSummaryService(repository)
                .find(state.playerId())
                .orElseThrow();

        assertEquals(state.playerId(), summary.playerId());
        assertEquals(java.util.List.of("Ace Trainer", "Researcher"), summary.trainerClasses());
        assertEquals(java.util.List.of(
                new CanonicalTrainerSummaryService.Skill("Command", 2),
                new CanonicalTrainerSummaryService.Skill("Survival", 3)), summary.skills());
        assertEquals(java.util.List.of("Agility Training", "Focused Training"), summary.trainerFeatures());
        assertEquals(java.util.List.of("Mount", "Tracker"), summary.availablePokemonCapabilities());
        assertEquals(2, summary.actionPoints());
        assertEquals(1, summary.initiativeModifier());
        assertEquals(14, summary.explicitInitiativeSpeed());
        assertEquals("cedar-team", summary.teamId());
        assertEquals(9L, summary.revision());
    }

    @Test
    void unknownOrBlankPlayerFailsClosed() {
        CanonicalStateRepository repository = playerId -> Optional.empty();
        CanonicalTrainerSummaryService service = new CanonicalTrainerSummaryService(repository);

        assertTrue(service.find("missing").isEmpty());
        assertTrue(service.find(" ").isEmpty());
        assertTrue(service.find(null).isEmpty());
    }
}
