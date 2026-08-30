package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Durable server-owned payload for one authoritative post-battle commit. */
public record AuthoritativePostBattleTransaction(
        String reservationId,
        String playerId,
        String engineTranscriptDigest,
        Map<String, Integer> consumedItemQuantities,
        List<AuthoritativePostBattlePokemonFinalState> pokemonFinalStates,
        Phase phase
) {
    public AuthoritativePostBattleTransaction {
        reservationId = requireText(reservationId, "reservationId");
        playerId = requireText(playerId, "playerId");
        engineTranscriptDigest = requireText(engineTranscriptDigest, "engineTranscriptDigest");
        phase = Objects.requireNonNull(phase, "phase");

        LinkedHashMap<String, Integer> consumptions = new LinkedHashMap<>();
        if (consumedItemQuantities != null) {
            consumedItemQuantities.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                String itemId = requireText(entry.getKey(), "consumed item id");
                Integer quantity = entry.getValue();
                if (quantity == null || quantity <= 0) {
                    throw new IllegalArgumentException("consumed item quantity must be > 0");
                }
                consumptions.put(itemId, quantity);
            });
        }
        consumedItemQuantities = Map.copyOf(consumptions);

        pokemonFinalStates = pokemonFinalStates == null ? List.of() : List.copyOf(pokemonFinalStates);
        if (pokemonFinalStates.isEmpty()) {
            throw new IllegalArgumentException("pokemonFinalStates must not be empty");
        }
        Set<String> pokemonIds = new LinkedHashSet<>();
        for (AuthoritativePostBattlePokemonFinalState state : pokemonFinalStates) {
            if (state == null || !pokemonIds.add(state.pokemonId())) {
                throw new IllegalArgumentException("pokemonFinalStates must contain unique non-null Pokemon");
            }
        }
    }

    public static AuthoritativePostBattleTransaction prepared(
            String reservationId,
            String playerId,
            String engineTranscriptDigest,
            Map<String, Integer> consumedItemQuantities,
            List<AuthoritativePostBattlePokemonFinalState> pokemonFinalStates
    ) {
        return new AuthoritativePostBattleTransaction(
                reservationId, playerId, engineTranscriptDigest, consumedItemQuantities, pokemonFinalStates, Phase.PREPARED);
    }

    public AuthoritativePostBattleTransaction committed() {
        return new AuthoritativePostBattleTransaction(
                reservationId, playerId, engineTranscriptDigest, consumedItemQuantities, pokemonFinalStates, Phase.COMMITTED);
    }

    public boolean samePayload(AuthoritativePostBattleTransaction other) {
        return other != null
                && reservationId.equals(other.reservationId)
                && playerId.equals(other.playerId)
                && engineTranscriptDigest.equals(other.engineTranscriptDigest)
                && consumedItemQuantities.equals(other.consumedItemQuantities)
                && pokemonFinalStates.equals(other.pokemonFinalStates);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.strip();
    }

    public enum Phase {
        PREPARED,
        COMMITTED
    }
}
