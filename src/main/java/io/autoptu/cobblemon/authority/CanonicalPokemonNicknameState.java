package io.autoptu.cobblemon.authority;

/** Durable RPG-only display name metadata for one server-owned canonical Pokemon. */
public record CanonicalPokemonNicknameState(
        String pokemonId,
        String ownerPlayerId,
        String nickname,
        long revision
) {
    public CanonicalPokemonNicknameState {
        if (pokemonId == null || pokemonId.isBlank()) throw new IllegalArgumentException("pokemonId must not be blank");
        if (ownerPlayerId == null || ownerPlayerId.isBlank()) throw new IllegalArgumentException("ownerPlayerId must not be blank");
        if (nickname == null || nickname.isBlank()) throw new IllegalArgumentException("nickname must not be blank");
        if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");
        pokemonId = pokemonId.strip();
        ownerPlayerId = ownerPlayerId.strip();
        nickname = nickname.strip();
    }
}
