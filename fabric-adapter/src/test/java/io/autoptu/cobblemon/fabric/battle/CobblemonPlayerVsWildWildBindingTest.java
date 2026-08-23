package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.BattleEncounterParticipantRequest;
import io.autoptu.cobblemon.authority.BattleParticipantKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobblemonPlayerVsWildWildBindingTest {
    private static final BattleArenaSnapshot ARENA = new BattleArenaSnapshot(
            "minecraft:overworld", 100, 64, 200, 1, 0, 0, 1
    );

    @Test
    void bindsWildRosterBeforeCanonicalResolutionAndReservation() {
        CobblemonCanonicalEncounterIdentityRegistry identities = playerOnlyIdentity();
        ServerOwnedWildEncounterIdentityBinder wildBinder = new ServerOwnedWildEncounterIdentityBinder(
                identities,
                (battleId, side, actorId) -> Optional.of(
                        new ServerOwnedWildEncounterIdentityBinder.CanonicalWildRoster(
                                "wild-1", List.of("wild-mon-1")
                        )
                )
        );
        AtomicBoolean reserved = new AtomicBoolean();
        CobblemonPlayerVsWildClaimCoordinator coordinator = new CobblemonPlayerVsWildClaimCoordinator(
                identities,
                null,
                wildBinder::bind,
                actorId -> Optional.of(new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                        "player-1", List.of("player-mon-1"), Map.of(), ARENA
                )),
                (playerId, pokemonIds, consumables, arena, participants) -> {
                    assertEquals(List.of(
                            new BattleEncounterParticipantRequest(
                                    1, "player-1", BattleParticipantKind.PLAYER, List.of("player-mon-1")
                            ),
                            new BattleEncounterParticipantRequest(
                                    2, "wild-1", BattleParticipantKind.WILD, List.of("wild-mon-1")
                            )
                    ), participants);
                    reserved.set(true);
                    return true;
                }
        );

        assertTrue(coordinator.tryClaim(signal()));
        assertTrue(reserved.get());
    }

    @Test
    void failsClosedBeforeReservationWhenWildRosterIsNotProvisioned() {
        CobblemonCanonicalEncounterIdentityRegistry identities = playerOnlyIdentity();
        AtomicBoolean contextRead = new AtomicBoolean();
        AtomicBoolean reserved = new AtomicBoolean();
        CobblemonPlayerVsWildClaimCoordinator coordinator = new CobblemonPlayerVsWildClaimCoordinator(
                identities,
                null,
                (battleId, externalWild) -> false,
                actorId -> {
                    contextRead.set(true);
                    return Optional.empty();
                },
                (playerId, pokemonIds, consumables, arena, participants) -> {
                    reserved.set(true);
                    return true;
                }
        );

        assertFalse(coordinator.tryClaim(signal()));
        assertFalse(contextRead.get());
        assertFalse(reserved.get());
    }

    private static CobblemonCanonicalEncounterIdentityRegistry playerOnlyIdentity() {
        CobblemonCanonicalEncounterIdentityRegistry identities = new CobblemonCanonicalEncounterIdentityRegistry();
        identities.register(
                CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                "external-player",
                "player-1",
                Map.of("external-player-mon", "player-mon-1")
        );
        return identities;
    }

    private static CobblemonBattleStartInterceptor.BattleStartSignal signal() {
        return new CobblemonBattleStartInterceptor.BattleStartSignal(
                "battle-1",
                List.of(
                        new CobblemonBattleStartInterceptor.ParticipantIdentity(
                                1,
                                CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                                "external-player",
                                List.of("external-player-mon")
                        ),
                        new CobblemonBattleStartInterceptor.ParticipantIdentity(
                                2,
                                CobblemonBattleStartInterceptor.ParticipantKind.WILD,
                                "external-wild",
                                List.of("external-wild-mon")
                        )
                )
        );
    }
}
