package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalCareStatusService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FabricCareStatusRuntimeTest {
    @Test
    void formatsPersistedCareValuesWithoutRecoveryInference() {
        CanonicalCareStatusService.Summary summary = new CanonicalCareStatusService.Summary(
                "minecraft-player:test",
                List.of(
                        new CanonicalCareStatusService.Member(1, "poke-a", "bulbasaur", 7, 29,
                                List.of("burned", "poisoned"), 2, 4L),
                        new CanonicalCareStatusService.Member(2, "poke-b", "squirtle", null, null,
                                List.of(), null, 1L)
                ),
                6L
        );

        assertEquals(List.of(
                "AutoPTU care status",
                "Slot 1 bulbasaur | HP 7/29 | statuses burned, poisoned | injuries 2 | Pokemon revision 4",
                "Slot 2 squirtle | HP unavailable | statuses none | injuries unavailable | Pokemon revision 1",
                "Party revision: 6",
                "Recovery eligibility/effects: authoritative PTU contract required"
        ), FabricCareStatusRuntime.formatLines(summary));
    }

    @Test
    void formatsEmptyCanonicalPartyWithoutInventingCareState() {
        CanonicalCareStatusService.Summary summary = new CanonicalCareStatusService.Summary(
                "minecraft-player:test", List.of(), 3L
        );

        assertEquals(List.of(
                "AutoPTU care status",
                "Party: empty",
                "Party revision: 3",
                "Recovery eligibility/effects: authoritative PTU contract required"
        ), FabricCareStatusRuntime.formatLines(summary));
    }
}
