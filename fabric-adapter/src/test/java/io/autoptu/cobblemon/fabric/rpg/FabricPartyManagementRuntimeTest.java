package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPartySummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FabricPartyManagementRuntimeTest {
    @Test
    void hudLabelProjectsOnlyCanonicalPartySummary() {
        CanonicalPartySummary summary = new CanonicalPartySummary(
                "player:test",
                List.of(
                        new CanonicalPartySummary.Member(
                                1, "pokemon-1", "pokemon:bulbasaur", 5, 20, 20, List.of(), 2),
                        new CanonicalPartySummary.Member(
                                2, "pokemon-2", "pokemon:charmander", 6, 12, 24, List.of("burned"), 4),
                        new CanonicalPartySummary.Member(
                                3, "pokemon-3", "pokemon:squirtle", 4, null, null, List.of(), 1)
                ),
                7
        );

        assertEquals(
                "Party ★ Bulbasaur 20/20 | Charmander 12/24 | Squirtle",
                FabricPartyManagementRuntime.hudLabel(summary)
        );
    }

    @Test
    void hudLabelFailsClosedForMissingPartyProjection() {
        assertEquals("Party unavailable", FabricPartyManagementRuntime.hudLabel(null));
        assertEquals(
                "Party unavailable",
                FabricPartyManagementRuntime.hudLabel(new CanonicalPartySummary("player:test", List.of(), 0))
        );
    }
}
