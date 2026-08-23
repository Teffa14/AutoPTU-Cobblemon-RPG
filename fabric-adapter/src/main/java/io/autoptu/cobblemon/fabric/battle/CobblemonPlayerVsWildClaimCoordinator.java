package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.BattleEncounterParticipantRequest;
import io.autoptu.cobblemon.authority.BattleParticipantKind;
import io.autoptu.cobblemon.authority.PlayerVsWildEncounterAuthorityService;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Identity-only bridge from Cobblemon BATTLE_STARTED_PRE to composed player-versus-wild authority.
 *
 * External UUIDs are lookup keys only. The authenticated player context, requested inventory
 * reservation and arena snapshot must come from server-owned state. No Cobblemon stats, HP, moves,
 * abilities, held items, Showdown state, legality or outcomes cross this boundary.
 */
public final class CobblemonPlayerVsWildClaimCoordinator implements CobblemonBattleStartInterceptor.ClaimHandler {
    public record PlayerEncounterContext(
            String canonicalPlayerId,
            List<String> canonicalPokemonIds,
            Map<String, Integer> consumableQuantities,
            BattleArenaSnapshot arena
    ) {
        public PlayerEncounterContext {
            canonicalPlayerId = requireId(canonicalPlayerId, "canonicalPlayerId");
            if (canonicalPokemonIds == null || canonicalPokemonIds.isEmpty()) {
                throw new IllegalArgumentException("canonicalPokemonIds must not be empty");
            }
            canonicalPokemonIds = List.copyOf(canonicalPokemonIds);
            consumableQuantities = consumableQuantities == null
                    ? Map.of()
                    : Map.copyOf(new LinkedHashMap<>(consumableQuantities));
            Objects.requireNonNull(arena, "arena");
        }
    }

    @FunctionalInterface
    public interface AuthenticatedPlayerContextResolver {
        Optional<PlayerEncounterContext> resolve(String externalPlayerActorId);
    }

    @FunctionalInterface
    interface PlayerIdentityBinder {
        boolean bind(String externalPlayerActorId, List<String> externalPokemonIds);
    }

    @FunctionalInterface
    interface WildIdentityBinder {
        boolean bind(String cobblemonBattleId, CobblemonBattleStartInterceptor.ParticipantIdentity externalWild);
    }

    @FunctionalInterface
    interface ReservationHandler {
        boolean tryReserve(
                String playerId,
                List<String> playerPokemonIds,
                Map<String, Integer> consumableQuantities,
                BattleArenaSnapshot arena,
                List<BattleEncounterParticipantRequest> participants
        );
    }

    private final CobblemonCanonicalEncounterIdentityRegistry identityRegistry;
    private final PlayerIdentityBinder playerIdentityBinder;
    private final WildIdentityBinder wildIdentityBinder;
    private final AuthenticatedPlayerContextResolver playerContextResolver;
    private final ReservationHandler reservationHandler;

    public CobblemonPlayerVsWildClaimCoordinator(
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            AuthenticatedPlayerContextResolver playerContextResolver,
            PlayerVsWildEncounterAuthorityService authorityService
    ) {
        this(identityRegistry, null, null, playerContextResolver, reservationHandler(authorityService));
    }

    /**
     * Production composition for a live Fabric world when WILD identities were already registered
     * by a server-owned encounter provisioning service.
     */
    public static CobblemonPlayerVsWildClaimCoordinator persistentWorld(
            MinecraftServer server,
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            PlayerVsWildEncounterAuthorityService authorityService
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(identityRegistry, "identityRegistry");
        return new CobblemonPlayerVsWildClaimCoordinator(
                identityRegistry,
                PersistentCanonicalPlayerPokemonIdentityBinder.fromWorldRuntime(server, identityRegistry)::bind,
                null,
                FabricAuthenticatedPlayerContextResolver.persistentWorld(server, identityRegistry),
                reservationHandler(authorityService)
        );
    }

    /**
     * Production composition that also binds a preprovisioned server-owned WILD roster at claim time.
     * The roster source returns canonical identities only; it must never derive PTU values from the
     * live Cobblemon entity or battle object.
     */
    public static CobblemonPlayerVsWildClaimCoordinator persistentWorld(
            MinecraftServer server,
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            ServerOwnedWildEncounterIdentityBinder.CanonicalWildRosterSource wildRosterSource,
            PlayerVsWildEncounterAuthorityService authorityService
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(identityRegistry, "identityRegistry");
        ServerOwnedWildEncounterIdentityBinder wildBinder = new ServerOwnedWildEncounterIdentityBinder(
                identityRegistry,
                Objects.requireNonNull(wildRosterSource, "wildRosterSource")
        );
        return new CobblemonPlayerVsWildClaimCoordinator(
                identityRegistry,
                PersistentCanonicalPlayerPokemonIdentityBinder.fromWorldRuntime(server, identityRegistry)::bind,
                wildBinder::bind,
                FabricAuthenticatedPlayerContextResolver.persistentWorld(server, identityRegistry),
                reservationHandler(authorityService)
        );
    }

    /**
     * Production composition for a WILD encounter whose canonical blueprint and opaque actor
     * correlation were registered by trusted server logic before BATTLE_STARTED_PRE.
     *
     * The preparation service and the authority service must share the same provisioning repository
     * so the roster resolved here is the canonical WILD state resolved again during reservation.
     */
    public static CobblemonPlayerVsWildClaimCoordinator persistentWorld(
            MinecraftServer server,
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            ServerOwnedWildEncounterPreparationService wildPreparationService,
            PlayerVsWildEncounterAuthorityService authorityService
    ) {
        Objects.requireNonNull(wildPreparationService, "wildPreparationService");
        return persistentWorld(
                server,
                identityRegistry,
                PreparingCanonicalWildRosterSource.fromWorldRuntime(server, wildPreparationService),
                authorityService
        );
    }

    CobblemonPlayerVsWildClaimCoordinator(
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            AuthenticatedPlayerContextResolver playerContextResolver,
            ReservationHandler reservationHandler
    ) {
        this(identityRegistry, null, null, playerContextResolver, reservationHandler);
    }

    CobblemonPlayerVsWildClaimCoordinator(
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            PlayerIdentityBinder playerIdentityBinder,
            AuthenticatedPlayerContextResolver playerContextResolver,
            ReservationHandler reservationHandler
    ) {
        this(identityRegistry, playerIdentityBinder, null, playerContextResolver, reservationHandler);
    }

    CobblemonPlayerVsWildClaimCoordinator(
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            PlayerIdentityBinder playerIdentityBinder,
            WildIdentityBinder wildIdentityBinder,
            AuthenticatedPlayerContextResolver playerContextResolver,
            ReservationHandler reservationHandler
    ) {
        this.identityRegistry = Objects.requireNonNull(identityRegistry, "identityRegistry");
        this.playerIdentityBinder = playerIdentityBinder;
        this.wildIdentityBinder = wildIdentityBinder;
        this.playerContextResolver = Objects.requireNonNull(playerContextResolver, "playerContextResolver");
        this.reservationHandler = Objects.requireNonNull(reservationHandler, "reservationHandler");
    }

    @Override
    public boolean tryClaim(CobblemonBattleStartInterceptor.BattleStartSignal signal) {
        if (signal == null || signal.participants().size() != 2) return false;

        CobblemonBattleStartInterceptor.ParticipantIdentity externalPlayer = null;
        CobblemonBattleStartInterceptor.ParticipantIdentity externalWild = null;
        for (CobblemonBattleStartInterceptor.ParticipantIdentity participant : signal.participants()) {
            if (participant.kind() == CobblemonBattleStartInterceptor.ParticipantKind.PLAYER) {
                if (externalPlayer != null) return false;
                externalPlayer = participant;
            } else if (participant.kind() == CobblemonBattleStartInterceptor.ParticipantKind.WILD) {
                if (externalWild != null) return false;
                externalWild = participant;
            } else {
                return false;
            }
        }
        if (externalPlayer == null || externalWild == null || externalPlayer.side() == externalWild.side()) {
            return false;
        }

        if (playerIdentityBinder != null
                && !playerIdentityBinder.bind(externalPlayer.actorId(), externalPlayer.pokemonIds())) {
            return false;
        }
        if (wildIdentityBinder != null && !wildIdentityBinder.bind(signal.cobblemonBattleId(), externalWild)) {
            return false;
        }

        Optional<PlayerEncounterContext> contextResult = playerContextResolver.resolve(externalPlayer.actorId());
        if (contextResult.isEmpty()) return false;

        ArrayList<BattleEncounterParticipantRequest> canonicalParticipants = new ArrayList<>(2);
        for (CobblemonBattleStartInterceptor.ParticipantIdentity participant : signal.participants()) {
            Optional<BattleEncounterParticipantRequest> resolved = identityRegistry.resolve(participant);
            if (resolved.isEmpty()) return false;
            canonicalParticipants.add(resolved.get());
        }

        BattleEncounterParticipantRequest canonicalPlayer = canonicalParticipants.stream()
                .filter(participant -> participant.participantKind() == BattleParticipantKind.PLAYER)
                .findFirst()
                .orElse(null);
        if (canonicalPlayer == null) return false;

        PlayerEncounterContext context = contextResult.get();
        if (!context.canonicalPlayerId().equals(canonicalPlayer.participantId())) return false;
        if (!context.canonicalPokemonIds().equals(canonicalPlayer.combatantIds())) return false;

        return reservationHandler.tryReserve(
                context.canonicalPlayerId(),
                context.canonicalPokemonIds(),
                context.consumableQuantities(),
                context.arena(),
                List.copyOf(canonicalParticipants)
        );
    }

    private static ReservationHandler reservationHandler(PlayerVsWildEncounterAuthorityService authorityService) {
        PlayerVsWildEncounterAuthorityService authority = Objects.requireNonNull(authorityService, "authorityService");
        return (playerId, playerPokemonIds, consumableQuantities, arena, participants) ->
                authority.reserve(
                        playerId,
                        playerPokemonIds,
                        consumableQuantities,
                        arena,
                        participants
                ).allowed();
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
