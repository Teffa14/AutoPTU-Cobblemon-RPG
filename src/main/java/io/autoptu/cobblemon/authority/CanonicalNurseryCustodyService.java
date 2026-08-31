package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Server-authoritative nursery custody shell.
 *
 * <p>The nursery stores identity/custody only. It deliberately does not calculate breeding,
 * compatibility, eggs, XP, evolution, move learning, inherited moves, training, or PTU effects.</p>
 */
public final class CanonicalNurseryCustodyService {
    public static final String CEDAR_NURSERY = "cedar_nursery";
    public static final int MAX_CUSTODY = 2;
    private static final int MAX_STALE_RETRIES = 16;

    private final FileCanonicalNurseryRepository nurseries;
    private final VersionedCanonicalPokemonStorageRepository storage;
    private final VersionedCanonicalPokemonRepository pokemon;

    public CanonicalNurseryCustodyService(
            FileCanonicalNurseryRepository nurseries,
            VersionedCanonicalPokemonStorageRepository storage,
            VersionedCanonicalPokemonRepository pokemon
    ) {
        this.nurseries = Objects.requireNonNull(nurseries, "nurseries");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.pokemon = Objects.requireNonNull(pokemon, "pokemon");
    }

    public NurserySummary inspect(String authenticatedPlayerId, String facilityId) {
        String playerId = requirePlayerId(authenticatedPlayerId);
        String facility = requireFacility(facilityId);
        FileCanonicalNurseryRepository.NurseryState state = nurseries.findOrCreate(playerId, facility);
        return project(state);
    }

    /** Enrolls a boxed Pokemon into durable custody and removes it from normal storage availability. */
    public NurserySummary enrollFromBox(String authenticatedPlayerId, String facilityId, int oneBasedBoxSlot) {
        String playerId = requirePlayerId(authenticatedPlayerId);
        String facility = requireFacility(facilityId);
        CanonicalPokemonStorageState box = storage.findOrCreate(playerId);
        if (oneBasedBoxSlot < 1 || oneBasedBoxSlot > box.pokemonIds().size()) {
            throw new IllegalArgumentException("box slot must be between 1 and " + box.pokemonIds().size());
        }
        String pokemonId = box.pokemonIds().get(oneBasedBoxSlot - 1);
        requireOwnedPokemon(playerId, pokemonId);

        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            FileCanonicalNurseryRepository.NurseryState current = nurseries.findOrCreate(playerId, facility);
            if (current.pokemonIds().contains(pokemonId)) {
                removeFromStorage(playerId, pokemonId);
                return project(nurseries.findOrCreate(playerId, facility));
            }
            if (current.pokemonIds().size() >= MAX_CUSTODY) {
                throw new IllegalStateException("nursery custody is full");
            }
            ArrayList<String> ids = new ArrayList<>(current.pokemonIds());
            ids.add(pokemonId);
            FileCanonicalNurseryRepository.NurseryState replacement = new FileCanonicalNurseryRepository.NurseryState(
                    playerId, facility, ids, current.revision() + 1);
            if (nurseries.replaceIfRevision(replacement, current.revision())) {
                removeFromStorage(playerId, pokemonId);
                return project(replacement);
            }
        }
        throw new IllegalStateException("nursery enrollment retry exhausted");
    }

    /** Releases custody back to normal boxed storage; it never places a Pokemon directly into party. */
    public NurserySummary releaseToBox(String authenticatedPlayerId, String facilityId, int oneBasedNurserySlot) {
        String playerId = requirePlayerId(authenticatedPlayerId);
        String facility = requireFacility(facilityId);
        FileCanonicalNurseryRepository.NurseryState current = nurseries.findOrCreate(playerId, facility);
        if (oneBasedNurserySlot < 1 || oneBasedNurserySlot > current.pokemonIds().size()) {
            throw new IllegalArgumentException("nursery slot must be between 1 and " + current.pokemonIds().size());
        }
        String pokemonId = current.pokemonIds().get(oneBasedNurserySlot - 1);
        requireOwnedPokemon(playerId, pokemonId);

        // Add first. If the process dies before custody is cleared, startup recovery removes this
        // duplicate box membership and leaves the Pokemon safely in nursery custody for retry.
        addToStorage(playerId, pokemonId);
        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            current = nurseries.findOrCreate(playerId, facility);
            if (!current.pokemonIds().contains(pokemonId)) return project(current);
            ArrayList<String> ids = new ArrayList<>(current.pokemonIds());
            ids.remove(pokemonId);
            FileCanonicalNurseryRepository.NurseryState replacement = new FileCanonicalNurseryRepository.NurseryState(
                    playerId, facility, ids, current.revision() + 1);
            if (nurseries.replaceIfRevision(replacement, current.revision())) return project(replacement);
        }
        throw new IllegalStateException("nursery release retry exhausted; custody remains recoverable");
    }

    /** Reconciles any interrupted enroll/release operation after world restart. */
    public int recoverCustody() {
        int reconciled = 0;
        for (FileCanonicalNurseryRepository.NurseryState state : nurseries.findAll()) {
            requireFacility(state.facilityId());
            for (String pokemonId : state.pokemonIds()) {
                requireOwnedPokemon(state.playerId(), pokemonId);
                if (storage.findOrCreate(state.playerId()).pokemonIds().contains(pokemonId)) {
                    removeFromStorage(state.playerId(), pokemonId);
                    reconciled++;
                }
            }
        }
        return reconciled;
    }

    private void removeFromStorage(String playerId, String pokemonId) {
        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            CanonicalPokemonStorageState current = storage.findOrCreate(playerId);
            if (!current.pokemonIds().contains(pokemonId)) return;
            ArrayList<String> ids = new ArrayList<>(current.pokemonIds());
            ids.remove(pokemonId);
            CanonicalPokemonStorageState replacement = new CanonicalPokemonStorageState(playerId, ids, current.revision() + 1);
            if (storage.replaceIfRevision(playerId, current.revision(), replacement)) return;
        }
        throw new IllegalStateException("nursery storage removal retry exhausted");
    }

    private void addToStorage(String playerId, String pokemonId) {
        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            CanonicalPokemonStorageState current = storage.findOrCreate(playerId);
            if (current.pokemonIds().contains(pokemonId)) return;
            ArrayList<String> ids = new ArrayList<>(current.pokemonIds());
            ids.add(pokemonId);
            CanonicalPokemonStorageState replacement = new CanonicalPokemonStorageState(playerId, ids, current.revision() + 1);
            if (storage.replaceIfRevision(playerId, current.revision(), replacement)) return;
        }
        throw new IllegalStateException("nursery storage addition retry exhausted");
    }

    private CanonicalPokemonState requireOwnedPokemon(String playerId, String pokemonId) {
        CanonicalPokemonState state = pokemon.findPokemon(pokemonId)
                .orElseThrow(() -> new IllegalStateException("nursery references missing canonical Pokemon"));
        if (!state.ownerPlayerId().equals(playerId)) {
            throw new IllegalStateException("nursery references Pokemon owned by another player");
        }
        return state;
    }

    private NurserySummary project(FileCanonicalNurseryRepository.NurseryState state) {
        List<Member> members = state.pokemonIds().stream().map(id -> {
            CanonicalPokemonState pokemonState = requireOwnedPokemon(state.playerId(), id);
            return new Member(id, pokemonState.speciesId(), pokemonState.level());
        }).toList();
        return new NurserySummary(state.facilityId(), members, state.revision());
    }

    private static String requirePlayerId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("authenticatedPlayerId is required");
        return value.strip();
    }

    private static String requireFacility(String value) {
        if (!CEDAR_NURSERY.equals(value)) throw new IllegalArgumentException("unknown server-authored nursery facility");
        return value;
    }

    public record Member(String pokemonId, String speciesId, int level) {}
    public record NurserySummary(String facilityId, List<Member> members, long revision) {}
}