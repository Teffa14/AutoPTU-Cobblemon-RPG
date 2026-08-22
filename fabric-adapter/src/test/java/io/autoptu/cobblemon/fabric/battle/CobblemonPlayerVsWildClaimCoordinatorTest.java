package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.BattleEncounterParticipantRequest;
import io.autoptu.cobblemon.authority.BattleParticipantKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobblemonPlayerVsWildClaimCoordinatorTest {
    private static final BattleArenaSnapshot ARENA = new BattleArenaSnapshot(
            "minecraft:overworld", 100, 64, 200, 1, 0, 0, 1
    );

    @Test
    void claimsOnlyAfterAuthenticatedContextAndCanonicalReservationSucceed() {
        CobblemonCanonicalEncounterIdentityRegistry identities = registeredIdentities();
        AtomicReference<String> resolvedActor = new AtomicReference<>();
        AtomicReference<List<BattleEncounterParticipantRequest>> reservedParticipants = new AtomicReference<>();

        CobblemonPlayerVsWildClaimCoordinator coordinator = new CobblemonPlayerVsWildClaimCoordinator(
                identities,
                actorId -> {
                    resolvedActor.set(actorId);
                    return Optional.of(new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                            "player-1", List.of("player-mon-1"), Map.of("potion", 1), ARENA
                    ));
                },
                (playerId, pokemonIds, consumables, arena, participants) -> {
                    assertEquals("player-1", playerId);
                    assertEquals(List.of("player-mon-1"), pokemonIds);
                    assertEquals(Map.of("potion", 1), consumables);
                    assertEquals(ARENA, arena);
                    reservedParticipants.set(participants);
                    return true;
                }
        );

        assertTrue(coordinator.tryClaim(signal()));
        assertEquals("external-player", resolvedActor.get());
        assertEquals(List.of(
                new BattleEncounterParticipantRequest(1, "player-1", BattleParticipantKind.PLAYER, List.of("player-mon-1")),
                new BattleEncounterParticipantRequest(2, "wild-1", BattleParticipantKind.WILD, List.of("wild-mon-1"))
        ), reservedParticipants.get());
    }

    @Test
    void refusesClaimWhenAuthenticatedPlayerContextIsUnavailable() {
        AtomicBoolean reservationCalled = new AtomicBoolean();
        CobblemonPlayerVsWildClaimCoordinator coordinator = new CobblemonPlayerVsWildClaimCoordinator(
                registeredIdentities(),
                actorId -> Optional.empty(),
                (playerId, pokemonIds, consumables, arena, participants) -> {
                    reservationCalled.set(true);
                    return true;
                }
        );

        assertFalse(coordinator.tryClaim(signal()));
        assertFalse(reservationCalled.get());
    }

    @Test
    void refusesClaimWhenAuthenticatedCanonicalRosterDoesNotMatchIdentityMapping() {
        AtomicBoolean reservationCalled = new AtomicBoolean();
        CobblemonPlayerVsWildClaimCoordinator coordinator = new CobblemonPlayerVsWildClaimCoordinator(
                registeredIdentities(),
                actorId -> Optional.of(new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                        "player-1", List.of("different-mon"), Map.of(), ARENA
                )),
                (playerId, pokemonIds, consumables, arena, participants) -> {
                    reservationCalled.set(true);
                    return true;
                }
        );

        assertFalse(coordinator.tryClaim(signal()));
        assertFalse(reservationCalled.get());
    }

    @Test
    void refusesClaimWhenCanonicalIdentityResolutionIsIncomplete() {
        CobblemonCanonicalEncounterIdentityRegistry identities = new CobblemonCanonicalEncounterIdentityRegistry();
        identities.register(
                CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                "external-player",
                "player-1",
                Map.of("external-player-mon", "player-mon-1")
        );
        AtomicBoolean reservationCalled = new AtomicBoolean();
        CobblemonPlayerVsWildClaimCoordinator coordinator = new CobblemonPlayerVsWildClaimCoordinator(
                identities,
                actorId -> Optional.of(new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                        "player-1", List.of("player-mon-1"), Map.of(), ARENA
                )),
                (playerId, pokemonIds, consumables, arena, participants) -> {
                    reservationCalled.set(true);
                    return true;
                }
        );

        assertFalse(coordinator.tryClaim(signal()));
        assertFalse(reservationCalled.get());
    }

    @Test
    void propagatesReservationDenialAsNoClaim() {
        CobblemonPlayerVsWildClaimCoordinator coordinator = new CobblemonPlayerVsWildClaimCoordinator(
                registeredIdentities(),
                actorId -> Optional.of(new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                        "player-1", List.of("player-mon-1"), Map.of(), ARENA
                )),
                (playerId, pokemonIds, consumables, arena, participants) -> false
        );

        assertFalse(coordinator.tryClaim(signal()));
    }

    @Test
    void rejectsUnsupportedTopologyBeforeAuthenticationOrReservation() {
        AtomicBoolean contextCalled = new AtomicBoolean();
        AtomicBoolean reservationCalled = new AtomicBoolean();
        CobblemonPlayerVsWildClaimCoordinator coordinator = new CobblemonPlayerVsWildClaimCoordinator(
                registeredIdentities(),
                actorId -> {
                    contextCalled.set(true);
                    return Optional.empty();
                },
                (playerId, pokemonIds, consumables, arena, participants) -> {
                    reservationCalled.set(true);
                    return true;
                }
        );
        CobblemonBattleStartInterceptor.BattleStartSignal wildVsWild = new CobblemonBattleStartInterceptor.BattleStartSignal(
                "battle-2",
                List.of(
                        new CobblemonBattleStartInterceptor.ParticipantIdentity(
                                1, CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                                "wild-a", List.of("wild-a-mon")
                        ),
                        new CobblemonBattleStartInterceptor.ParticipantIdentity(
                                2, CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                                "wild-b", List.of("wild-b-mon")
                        )
                )
        );

        assertFalse(coordinator.tryClaim(wildVsWild));
        assertFalse(contextCalled.get());
        assertFalse(reservationCalled.get());
    }

    private static CobblemonCanonicalEncounterIdentityRegistry registeredIdentities() {
        CobblemonCanonicalEncounterIdentityRegistry identities = new CobblemonCanonicalEncounterIdentityRegistry();
        identities.register(
                CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                "external-player",
                "player-1",
                Map.of("external-player-mon", "player-mon-1")
        );
        identities.register(
                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                "external-wild",
                "wild-1",
                Map.of("external-wild-mon", "wild-mon-1")
        );
        return identities;
    }

    private static CobblemonBattleStartInterceptor.BattleStartSignal signal() {
        return new CobblemonBattleStartInterceptor.BattleStartSignal(
                "battle-1",
                List.of(
                        new CobblemonBattleStartInterceptor.ParticipantIdentity(
                                1, CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                                "external-player", List.of("external-player-mon")
                        ),
                        new CobblemonBattleStartInterceptor.ParticipantIdentity(
                                2, CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                                "external-wild", List.of("external-wild-mon")
                        )
                )
        );
    }
}
