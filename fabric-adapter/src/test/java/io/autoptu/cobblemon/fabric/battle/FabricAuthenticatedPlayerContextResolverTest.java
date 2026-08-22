package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricAuthenticatedPlayerContextResolverTest {
    private static final UUID PLAYER_UUID = UUID.fromString("b6b5fca3-4fc5-4fc8-b61e-d0a9057e01a4");
    private static final BattleArenaSnapshot ARENA = new BattleArenaSnapshot(
            "minecraft:overworld", 20, 64, 30, 1, 0, 0, 1
    );

    @Test
    void resolvesCanonicalContextOnlyForOnlineAuthenticatedUuid() {
        AtomicReference<UUID> lookedUp = new AtomicReference<>();
        AtomicReference<UUID> canonicalLookup = new AtomicReference<>();
        CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext expected = context();

        FabricAuthenticatedPlayerContextResolver resolver = new FabricAuthenticatedPlayerContextResolver(
                uuid -> {
                    lookedUp.set(uuid);
                    return true;
                },
                uuid -> {
                    canonicalLookup.set(uuid);
                    return Optional.of(expected);
                }
        );

        assertEquals(Optional.of(expected), resolver.resolve(PLAYER_UUID.toString()));
        assertEquals(PLAYER_UUID, lookedUp.get());
        assertEquals(PLAYER_UUID, canonicalLookup.get());
    }

    @Test
    void rejectsOfflinePlayerBeforeCanonicalStateLookup() {
        AtomicInteger canonicalLookups = new AtomicInteger();
        FabricAuthenticatedPlayerContextResolver resolver = new FabricAuthenticatedPlayerContextResolver(
                uuid -> false,
                uuid -> {
                    canonicalLookups.incrementAndGet();
                    return Optional.of(context());
                }
        );

        assertTrue(resolver.resolve(PLAYER_UUID.toString()).isEmpty());
        assertEquals(0, canonicalLookups.get());
    }

    @Test
    void rejectsMalformedBlankAndNonCanonicalActorIdsBeforeSessionLookup() {
        AtomicInteger sessionLookups = new AtomicInteger();
        AtomicInteger canonicalLookups = new AtomicInteger();
        FabricAuthenticatedPlayerContextResolver resolver = new FabricAuthenticatedPlayerContextResolver(
                uuid -> {
                    sessionLookups.incrementAndGet();
                    return true;
                },
                uuid -> {
                    canonicalLookups.incrementAndGet();
                    return Optional.of(context());
                }
        );

        assertTrue(resolver.resolve(null).isEmpty());
        assertTrue(resolver.resolve("  ").isEmpty());
        assertTrue(resolver.resolve("external-player").isEmpty());
        assertTrue(resolver.resolve("b6b5fca3-4fc5-4fc8-b61e-d0a9057e01a4-extra").isEmpty());
        assertEquals(0, sessionLookups.get());
        assertEquals(0, canonicalLookups.get());
    }

    @Test
    void preservesMissingCanonicalContextAsAuthenticationFailure() {
        AtomicInteger canonicalLookups = new AtomicInteger();
        FabricAuthenticatedPlayerContextResolver resolver = new FabricAuthenticatedPlayerContextResolver(
                uuid -> true,
                uuid -> {
                    canonicalLookups.incrementAndGet();
                    return Optional.empty();
                }
        );

        assertFalse(resolver.resolve(PLAYER_UUID.toString()).isPresent());
        assertEquals(1, canonicalLookups.get());
    }

    @Test
    void treatsNullCanonicalSourceResultAsFailClosed() {
        FabricAuthenticatedPlayerContextResolver resolver = new FabricAuthenticatedPlayerContextResolver(
                uuid -> true,
                uuid -> null
        );

        assertTrue(resolver.resolve(PLAYER_UUID.toString()).isEmpty());
    }

    private static CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext context() {
        return new CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext(
                "player-1",
                List.of("pokemon-1"),
                Map.of("potion", 1),
                ARENA
        );
    }
}
