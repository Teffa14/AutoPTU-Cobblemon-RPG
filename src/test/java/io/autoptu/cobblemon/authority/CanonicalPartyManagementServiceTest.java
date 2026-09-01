package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalPartyManagementServiceTest {
    private final CanonicalPartyManagementService service = new CanonicalPartyManagementService();

    @Test
    void allowsMatchingServerResolvedLeadSelection() {
        CanonicalPartySummary party = party("minecraft-player:test", 7L);
        CanonicalPartyManagementService.Decision decision = service.canManage(
                new CanonicalPartyManagementService.Request(
                        party.playerId(), true, CanonicalPartyManagementService.Mutation.SET_LEAD,
                        2, "poke-b", 7L, party));

        assertTrue(decision.allowed());
    }

    @Test
    void allowsMatchingServerResolvedReorderSelection() {
        CanonicalPartySummary party = party("minecraft-player:test", 7L);
        CanonicalPartyManagementService.Decision decision = service.canManage(
                new CanonicalPartyManagementService.Request(
                        party.playerId(), true, CanonicalPartyManagementService.Mutation.REORDER,
                        1, "poke-a", 2, "poke-b", 7L, party));

        assertTrue(decision.allowed());
        assertEquals(1, decision.partySlot());
        assertEquals("poke-a", decision.pokemonId());
        assertEquals(2, decision.targetPartySlot());
        assertEquals("poke-b", decision.targetPokemonId());
    }

    @Test
    void rejectsStaleRevisionAndForgedPokemonIdentity() {
        CanonicalPartySummary party = party("minecraft-player:test", 7L);
        assertFalse(service.canManage(new CanonicalPartyManagementService.Request(
                party.playerId(), true, CanonicalPartyManagementService.Mutation.SET_LEAD,
                2, "poke-b", 6L, party)).allowed());
        assertFalse(service.canManage(new CanonicalPartyManagementService.Request(
                party.playerId(), true, CanonicalPartyManagementService.Mutation.SET_LEAD,
                2, "foreign-pokemon", 7L, party)).allowed());
    }

    @Test
    void rejectsReorderWhenTargetOrSourceIdentityChanged() {
        CanonicalPartySummary party = party("minecraft-player:test", 7L);
        assertFalse(service.canManage(new CanonicalPartyManagementService.Request(
                party.playerId(), true, CanonicalPartyManagementService.Mutation.REORDER,
                1, "foreign-source", 2, "poke-b", 7L, party)).allowed());
        assertFalse(service.canManage(new CanonicalPartyManagementService.Request(
                party.playerId(), true, CanonicalPartyManagementService.Mutation.REORDER,
                1, "poke-a", 2, "foreign-target", 7L, party)).allowed());
    }

    @Test
    void rejectsReorderToSameOrUnoccupiedSlot() {
        CanonicalPartySummary party = party("minecraft-player:test", 7L);
        assertFalse(service.canManage(new CanonicalPartyManagementService.Request(
                party.playerId(), true, CanonicalPartyManagementService.Mutation.REORDER,
                1, "poke-a", 1, "poke-a", 7L, party)).allowed());
        assertFalse(service.canManage(new CanonicalPartyManagementService.Request(
                party.playerId(), true, CanonicalPartyManagementService.Mutation.REORDER,
                1, "poke-a", 3, "poke-c", 7L, party)).allowed());
    }

    @Test
    void rejectsMissingTrainerAndForeignPartyOwner() {
        CanonicalPartySummary party = party("minecraft-player:test", 7L);
        assertFalse(service.canManage(new CanonicalPartyManagementService.Request(
                party.playerId(), false, CanonicalPartyManagementService.Mutation.SET_LEAD,
                2, "poke-b", 7L, party)).allowed());
        assertFalse(service.canManage(new CanonicalPartyManagementService.Request(
                "minecraft-player:other", true, CanonicalPartyManagementService.Mutation.SET_LEAD,
                2, "poke-b", 7L, party)).allowed());
    }

    private static CanonicalPartySummary party(String playerId, long revision) {
        return new CanonicalPartySummary(playerId, List.of(
                new CanonicalPartySummary.Member(1, "poke-a", "bulbasaur", 5, 20, 20, List.of(), 1L),
                new CanonicalPartySummary.Member(2, "poke-b", "charmander", 5, 18, 20, List.of(), 1L)
        ), revision);
    }
}
