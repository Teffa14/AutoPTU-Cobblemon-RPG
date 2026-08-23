package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerOwnedWildEncounterIdentityBinderTest {
    @Test
    void bindsOnlyOpaqueExternalIdsToPreprovisionedCanonicalRoster() {
        CobblemonCanonicalEncounterIdentityRegistry identities = new CobblemonCanonicalEncounterIdentityRegistry();
        AtomicReference<String> requested = new AtomicReference<>();
        ServerOwnedWildEncounterIdentityBinder binder = new ServerOwnedWildEncounterIdentityBinder(
                identities,
                (battleId, side, actorId) -> {
                    requested.set(battleId + "|" + side + "|" + actorId);
                    return Optional.of(new ServerOwnedWildEncounterIdentityBinder.CanonicalWildRoster(
                            "wild-participant-7", List.of("wild-canonical-a", "wild-canonical-b")
                    ));
                }
        );
        CobblemonBattleStartInterceptor.ParticipantIdentity external = wild(
                List.of("external-wild-a", "external-wild-b")
        );

        assertTrue(binder.bind("battle-7", external));
        assertEquals("battle-7|2|external-wild", requested.get());
        assertEquals(
                Optional.of("wild-participant-7"),
                identities.resolveParticipantId(CobblemonBattleStartInterceptor.ParticipantKind.WILD, "external-wild")
        );
        assertEquals(
                List.of("wild-canonical-a", "wild-canonical-b"),
                identities.resolve(external).orElseThrow().combatantIds()
        );
    }

    @Test
    void failsClosedWhenCanonicalRosterWasNotProvisioned() {
        CobblemonCanonicalEncounterIdentityRegistry identities = new CobblemonCanonicalEncounterIdentityRegistry();
        ServerOwnedWildEncounterIdentityBinder binder = new ServerOwnedWildEncounterIdentityBinder(
                identities,
                (battleId, side, actorId) -> Optional.empty()
        );

        assertFalse(binder.bind("battle-7", wild(List.of("external-wild-a"))));
        assertEquals(0, identities.registeredParticipantCount());
    }

    @Test
    void rejectsRosterCardinalityMismatchInsteadOfInventingCombatants() {
        CobblemonCanonicalEncounterIdentityRegistry identities = new CobblemonCanonicalEncounterIdentityRegistry();
        ServerOwnedWildEncounterIdentityBinder binder = new ServerOwnedWildEncounterIdentityBinder(
                identities,
                (battleId, side, actorId) -> Optional.of(
                        new ServerOwnedWildEncounterIdentityBinder.CanonicalWildRoster(
                                "wild-participant-7", List.of("only-one-canonical")
                        )
                )
        );

        assertFalse(binder.bind("battle-7", wild(List.of("external-wild-a", "external-wild-b"))));
        assertEquals(0, identities.registeredParticipantCount());
    }

    @Test
    void rejectsNonWildParticipants() {
        CobblemonCanonicalEncounterIdentityRegistry identities = new CobblemonCanonicalEncounterIdentityRegistry();
        ServerOwnedWildEncounterIdentityBinder binder = new ServerOwnedWildEncounterIdentityBinder(
                identities,
                (battleId, side, actorId) -> Optional.of(
                        new ServerOwnedWildEncounterIdentityBinder.CanonicalWildRoster(
                                "wild-participant-7", List.of("wild-canonical-a")
                        )
                )
        );
        CobblemonBattleStartInterceptor.ParticipantIdentity player =
                new CobblemonBattleStartInterceptor.ParticipantIdentity(
                        2,
                        CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                        "external-player",
                        List.of("external-player-mon")
                );

        assertFalse(binder.bind("battle-7", player));
        assertEquals(0, identities.registeredParticipantCount());
    }

    private static CobblemonBattleStartInterceptor.ParticipantIdentity wild(List<String> pokemonIds) {
        return new CobblemonBattleStartInterceptor.ParticipantIdentity(
                2,
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "external-wild",
                pokemonIds
        );
    }
}
