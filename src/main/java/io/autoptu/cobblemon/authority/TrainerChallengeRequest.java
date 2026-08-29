package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;

/** Server-authored world challenge intent. It is not a battle result or legality decision. */
public record TrainerChallengeRequest(
        String playerId,
        String challengeId,
        String npcId,
        List<String> playerPokemonIds,
        long partyRevision
) {
    public TrainerChallengeRequest {
        playerId = requireText(playerId, "playerId");
        challengeId = requireText(challengeId, "challengeId");
        npcId = requireText(npcId, "npcId");
        playerPokemonIds = List.copyOf(Objects.requireNonNull(playerPokemonIds, "playerPokemonIds"));
        if (playerPokemonIds.isEmpty()) throw new IllegalArgumentException("playerPokemonIds cannot be empty");
        if (playerPokemonIds.stream().anyMatch(id -> id == null || id.isBlank())) throw new IllegalArgumentException("playerPokemonIds contain blank identity");
        if (partyRevision < 0) throw new IllegalArgumentException("partyRevision cannot be negative");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
