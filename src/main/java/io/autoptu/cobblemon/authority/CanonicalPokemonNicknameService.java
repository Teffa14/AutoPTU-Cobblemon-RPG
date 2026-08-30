package io.autoptu.cobblemon.authority;

import java.util.Objects;
import java.util.Optional;

/** Resolves party slot and ownership on the server before mutating RPG-only nickname metadata. */
public final class CanonicalPokemonNicknameService {
    private static final int MAX_RETRIES = 3;
    public static final int MAX_NICKNAME_CODE_POINTS = 24;

    public enum Outcome { APPLIED, ALREADY_SET, INVALID_NAME, NO_PARTY, INVALID_SLOT, POKEMON_MISSING, NOT_OWNER, CONCURRENT_WRITE }
    public record Decision(Outcome outcome, String pokemonId, String nickname, long revision, String reason) {
        public Decision { Objects.requireNonNull(outcome, "outcome"); reason = reason == null ? "" : reason; }
    }

    private final VersionedCanonicalPlayerEncounterProfileRepository partyRepository;
    private final VersionedCanonicalPokemonRepository pokemonRepository;
    private final VersionedCanonicalPokemonNicknameRepository nicknameRepository;

    public CanonicalPokemonNicknameService(
            VersionedCanonicalPlayerEncounterProfileRepository partyRepository,
            VersionedCanonicalPokemonRepository pokemonRepository,
            VersionedCanonicalPokemonNicknameRepository nicknameRepository) {
        this.partyRepository = Objects.requireNonNull(partyRepository, "partyRepository");
        this.pokemonRepository = Objects.requireNonNull(pokemonRepository, "pokemonRepository");
        this.nicknameRepository = Objects.requireNonNull(nicknameRepository, "nicknameRepository");
    }

    public Decision setNickname(String playerId, int oneBasedSlot, String requestedNickname) {
        String canonicalPlayerId = playerId == null ? "" : playerId.strip();
        String nickname = normalize(requestedNickname);
        if (canonicalPlayerId.isEmpty()) return new Decision(Outcome.NO_PARTY, null, null, -1, "canonical player id is required");
        if (!isValidNickname(nickname)) return new Decision(Outcome.INVALID_NAME, null, null, -1, "nickname must be 1-24 visible characters and contain no control characters");

        Optional<CanonicalPlayerEncounterProfile> profileResult = partyRepository.findProfile(canonicalPlayerId);
        if (profileResult.isEmpty()) return new Decision(Outcome.NO_PARTY, null, null, -1, "persistent canonical party is not configured");
        CanonicalPlayerEncounterProfile profile = profileResult.get();
        if (oneBasedSlot < 1 || oneBasedSlot > profile.pokemonIds().size()) {
            return new Decision(Outcome.INVALID_SLOT, null, null, -1, "party slot must be between 1 and " + profile.pokemonIds().size());
        }
        String pokemonId = profile.pokemonIds().get(oneBasedSlot - 1);
        Optional<CanonicalPokemonState> pokemonResult = pokemonRepository.findPokemon(pokemonId);
        if (pokemonResult.isEmpty()) return new Decision(Outcome.POKEMON_MISSING, pokemonId, null, -1, "canonical Pokemon is missing");
        if (!pokemonResult.get().ownerPlayerId().equals(canonicalPlayerId)) {
            return new Decision(Outcome.NOT_OWNER, pokemonId, null, -1, "canonical Pokemon ownership does not match authenticated player");
        }

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Optional<CanonicalPokemonNicknameState> currentResult = nicknameRepository.findNickname(pokemonId);
            if (currentResult.isEmpty()) {
                CanonicalPokemonNicknameState initial = new CanonicalPokemonNicknameState(pokemonId, canonicalPlayerId, nickname, 0);
                if (nicknameRepository.createNicknameIfAbsent(initial)) return new Decision(Outcome.APPLIED, pokemonId, nickname, 0, "");
                continue;
            }
            CanonicalPokemonNicknameState current = currentResult.get();
            if (!current.ownerPlayerId().equals(canonicalPlayerId)) return new Decision(Outcome.NOT_OWNER, pokemonId, null, current.revision(), "nickname ownership does not match authenticated player");
            if (current.nickname().equals(nickname)) return new Decision(Outcome.ALREADY_SET, pokemonId, nickname, current.revision(), "");
            CanonicalPokemonNicknameState replacement = new CanonicalPokemonNicknameState(pokemonId, canonicalPlayerId, nickname, current.revision() + 1);
            if (nicknameRepository.replaceNicknameIfRevision(pokemonId, current.revision(), replacement)) {
                return new Decision(Outcome.APPLIED, pokemonId, nickname, replacement.revision(), "");
            }
        }
        return new Decision(Outcome.CONCURRENT_WRITE, pokemonId, null, -1, "nickname changed concurrently; retry the request");
    }

    private static String normalize(String value) { return value == null ? "" : value.strip(); }
    private static boolean isValidNickname(String value) {
        if (value.isEmpty() || value.codePointCount(0, value.length()) > MAX_NICKNAME_CODE_POINTS) return false;
        return value.codePoints().noneMatch(Character::isISOControl);
    }
}
