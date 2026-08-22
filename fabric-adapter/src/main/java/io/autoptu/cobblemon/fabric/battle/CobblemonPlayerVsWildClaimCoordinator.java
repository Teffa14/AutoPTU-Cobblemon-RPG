package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.BattleEncounterParticipantRequest;
import io.autoptu.cobblemon.authority.BattleParticipantKind;
import io.autoptu.cobblemon.authority.PlayerVsWildBattleReservationDecision;

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
    public interface ReservationHandler {
        PlayerVsWildBattleReservationDecision reserve(
                String playerId,
                List<String> playerPokemonIds,
                Map<String, Integer> consumableQuantities,
                BattleArenaSnapshot arena,
                List<BattleEncounterParticipantRequest> participants
        );
    }

    private final CobblemonCanonicalEncounterIdentityRegistry identityRegistry;
    private final AuthenticatedPlayerContextResolver playerContextResolver;
    private final ReservationHandler reservationHandler;

    public CobblemonPlayerVsWildClaimCoordinator(
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            AuthenticatedPlayerContextResolver playerContextResolver,
            ReservationHandler reservationHandler
    ) {
        this.identityRegistry = Objects.requireNonNull(identityRegistry, "identityRegistry");
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

        PlayerVsWildBattleReservationDecision decision = reservationHandler.reserve(
                context.canonicalPlayerId(),
                context.canonicalPokemonIds(),
                context.consumableQuantities(),
                context.arena(),
                List.copyOf(canonicalParticipants)
        );
        return decision != null && decision.allowed();
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
