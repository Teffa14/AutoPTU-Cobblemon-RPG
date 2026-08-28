package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Server-authoritative party/box transfer orchestration.
 *
 * <p>Clients select only a one-based source slot. Pokemon identity, ownership, current party/box
 * membership and revisions are always re-resolved from durable server state. Cross-aggregate moves
 * are journaled so a restart can finish an interrupted transfer without duplicating or losing the
 * canonical Pokemon.</p>
 */
public final class CanonicalPokemonStorageTransferService {
    private static final int MAX_STALE_RETRIES = 16;

    private final VersionedCanonicalPlayerEncounterProfileRepository parties;
    private final VersionedCanonicalPokemonStorageRepository storage;
    private final VersionedCanonicalPokemonRepository pokemon;
    private final FileCanonicalPokemonTransferRepository transfers;

    public CanonicalPokemonStorageTransferService(
            VersionedCanonicalPlayerEncounterProfileRepository parties,
            VersionedCanonicalPokemonStorageRepository storage,
            VersionedCanonicalPokemonRepository pokemon,
            FileCanonicalPokemonTransferRepository transfers
    ) {
        this.parties = Objects.requireNonNull(parties, "parties");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.pokemon = Objects.requireNonNull(pokemon, "pokemon");
        this.transfers = Objects.requireNonNull(transfers, "transfers");
    }

    public TransferResult deposit(String transferId, String authenticatedPlayerId, int oneBasedPartySlot) {
        String playerId = requirePlayerId(authenticatedPlayerId);
        FileCanonicalPokemonTransferRepository.TransferAttempt existing = transfers.find(transferId).orElse(null);
        if (existing != null) {
            requireRetryIdentity(existing, playerId, FileCanonicalPokemonTransferRepository.Direction.DEPOSIT);
            return resume(existing);
        }

        CanonicalPlayerEncounterProfile party = parties.findProfile(playerId)
                .orElseThrow(() -> new IllegalStateException("persistent canonical party is not configured"));
        if (oneBasedPartySlot < 1 || oneBasedPartySlot > party.pokemonIds().size()) {
            throw new IllegalArgumentException("party slot must be between 1 and " + party.pokemonIds().size());
        }
        if (party.pokemonIds().size() <= 1) {
            throw new IllegalArgumentException("cannot deposit the last active party Pokemon");
        }
        String pokemonId = party.pokemonIds().get(oneBasedPartySlot - 1);
        CanonicalPokemonStorageState box = storage.findOrCreate(playerId);
        requireOwnedPokemon(playerId, pokemonId);
        if (box.pokemonIds().contains(pokemonId)) {
            throw new IllegalStateException("canonical Pokemon already exists in storage");
        }

        FileCanonicalPokemonTransferRepository.TransferAttempt attempt = transfers.createIfAbsent(
                new FileCanonicalPokemonTransferRepository.TransferAttempt(
                        transferId,
                        playerId,
                        FileCanonicalPokemonTransferRepository.Direction.DEPOSIT,
                        pokemonId,
                        FileCanonicalPokemonTransferRepository.Stage.CREATED
                ));
        return resume(attempt);
    }

    public TransferResult withdraw(String transferId, String authenticatedPlayerId, int oneBasedBoxSlot) {
        String playerId = requirePlayerId(authenticatedPlayerId);
        FileCanonicalPokemonTransferRepository.TransferAttempt existing = transfers.find(transferId).orElse(null);
        if (existing != null) {
            requireRetryIdentity(existing, playerId, FileCanonicalPokemonTransferRepository.Direction.WITHDRAW);
            return resume(existing);
        }

        CanonicalPlayerEncounterProfile party = parties.findProfile(playerId)
                .orElseThrow(() -> new IllegalStateException("persistent canonical party is not configured"));
        CanonicalPokemonStorageState box = storage.findOrCreate(playerId);
        if (oneBasedBoxSlot < 1 || oneBasedBoxSlot > box.pokemonIds().size()) {
            throw new IllegalArgumentException("box slot must be between 1 and " + box.pokemonIds().size());
        }
        String pokemonId = box.pokemonIds().get(oneBasedBoxSlot - 1);
        requireOwnedPokemon(playerId, pokemonId);
        if (party.pokemonIds().contains(pokemonId)) {
            throw new IllegalStateException("canonical Pokemon already exists in the active party");
        }

        FileCanonicalPokemonTransferRepository.TransferAttempt attempt = transfers.createIfAbsent(
                new FileCanonicalPokemonTransferRepository.TransferAttempt(
                        transferId,
                        playerId,
                        FileCanonicalPokemonTransferRepository.Direction.WITHDRAW,
                        pokemonId,
                        FileCanonicalPokemonTransferRepository.Stage.CREATED
                ));
        return resume(attempt);
    }

    public List<TransferResult> recoverPending() {
        return transfers.findPending().stream().map(this::resume).toList();
    }

    private TransferResult resume(FileCanonicalPokemonTransferRepository.TransferAttempt original) {
        FileCanonicalPokemonTransferRepository.TransferAttempt attempt = requireAttempt(original.transferId());
        requireOwnedPokemon(attempt.playerId(), attempt.pokemonId());

        if (attempt.stage() == FileCanonicalPokemonTransferRepository.Stage.CREATED) {
            if (attempt.direction() == FileCanonicalPokemonTransferRepository.Direction.DEPOSIT) {
                removeFromParty(attempt.playerId(), attempt.pokemonId());
            } else {
                removeFromStorage(attempt.playerId(), attempt.pokemonId());
            }
            advance(attempt, FileCanonicalPokemonTransferRepository.Stage.SOURCE_REMOVED);
            attempt = requireAttempt(attempt.transferId());
        }

        if (attempt.stage() == FileCanonicalPokemonTransferRepository.Stage.SOURCE_REMOVED) {
            if (attempt.direction() == FileCanonicalPokemonTransferRepository.Direction.DEPOSIT) {
                addToStorage(attempt.playerId(), attempt.pokemonId());
            } else {
                addToParty(attempt.playerId(), attempt.pokemonId());
            }
            advance(attempt, FileCanonicalPokemonTransferRepository.Stage.TARGET_ADDED);
            attempt = requireAttempt(attempt.transferId());
        }

        if (attempt.stage() == FileCanonicalPokemonTransferRepository.Stage.TARGET_ADDED) {
            advance(attempt, FileCanonicalPokemonTransferRepository.Stage.COMMITTED);
            attempt = requireAttempt(attempt.transferId());
        }

        if (attempt.stage() != FileCanonicalPokemonTransferRepository.Stage.COMMITTED) {
            throw new IllegalStateException("Pokemon transfer recovery stopped at non-terminal stage " + attempt.stage());
        }
        return project(attempt);
    }

    private void removeFromParty(String playerId, String pokemonId) {
        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            CanonicalPlayerEncounterProfile current = parties.findProfile(playerId)
                    .orElseThrow(() -> new IllegalStateException("persistent canonical party disappeared during transfer"));
            if (!current.pokemonIds().contains(pokemonId)) return;
            if (current.pokemonIds().size() <= 1) {
                throw new IllegalStateException("cannot remove the last active party Pokemon during transfer recovery");
            }
            ArrayList<String> replacementIds = new ArrayList<>(current.pokemonIds());
            replacementIds.remove(pokemonId);
            CanonicalPlayerEncounterProfile replacement = new CanonicalPlayerEncounterProfile(
                    current.playerId(), replacementIds, current.consumableQuantities(), current.arena(), current.revision() + 1);
            if (parties.replaceProfileIfRevision(playerId, current.revision(), replacement)) return;
        }
        throw new IllegalStateException("party transfer source mutation retry exhausted");
    }

    private void removeFromStorage(String playerId, String pokemonId) {
        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            CanonicalPokemonStorageState current = storage.findOrCreate(playerId);
            if (!current.pokemonIds().contains(pokemonId)) return;
            ArrayList<String> replacementIds = new ArrayList<>(current.pokemonIds());
            replacementIds.remove(pokemonId);
            CanonicalPokemonStorageState replacement = new CanonicalPokemonStorageState(
                    playerId, replacementIds, current.revision() + 1);
            if (storage.replaceIfRevision(playerId, current.revision(), replacement)) return;
        }
        throw new IllegalStateException("box transfer source mutation retry exhausted");
    }

    private void addToParty(String playerId, String pokemonId) {
        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            CanonicalPlayerEncounterProfile current = parties.findProfile(playerId)
                    .orElseThrow(() -> new IllegalStateException("persistent canonical party disappeared during transfer"));
            if (current.pokemonIds().contains(pokemonId)) return;
            CanonicalPokemonStorageState currentBox = storage.findOrCreate(playerId);
            if (currentBox.pokemonIds().contains(pokemonId)) {
                throw new IllegalStateException("cannot add Pokemon to party while it remains in storage");
            }
            ArrayList<String> replacementIds = new ArrayList<>(current.pokemonIds());
            replacementIds.add(pokemonId);
            CanonicalPlayerEncounterProfile replacement = new CanonicalPlayerEncounterProfile(
                    current.playerId(), replacementIds, current.consumableQuantities(), current.arena(), current.revision() + 1);
            if (parties.replaceProfileIfRevision(playerId, current.revision(), replacement)) return;
        }
        throw new IllegalStateException("party transfer target mutation retry exhausted");
    }

    private void addToStorage(String playerId, String pokemonId) {
        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            CanonicalPokemonStorageState current = storage.findOrCreate(playerId);
            if (current.pokemonIds().contains(pokemonId)) return;
            CanonicalPlayerEncounterProfile currentParty = parties.findProfile(playerId)
                    .orElseThrow(() -> new IllegalStateException("persistent canonical party disappeared during transfer"));
            if (currentParty.pokemonIds().contains(pokemonId)) {
                throw new IllegalStateException("cannot add Pokemon to storage while it remains in the active party");
            }
            ArrayList<String> replacementIds = new ArrayList<>(current.pokemonIds());
            replacementIds.add(pokemonId);
            CanonicalPokemonStorageState replacement = new CanonicalPokemonStorageState(
                    playerId, replacementIds, current.revision() + 1);
            if (storage.replaceIfRevision(playerId, current.revision(), replacement)) return;
        }
        throw new IllegalStateException("box transfer target mutation retry exhausted");
    }

    private TransferResult project(FileCanonicalPokemonTransferRepository.TransferAttempt attempt) {
        CanonicalPlayerEncounterProfile party = parties.findProfile(attempt.playerId())
                .orElseThrow(() -> new IllegalStateException("persistent canonical party disappeared after transfer"));
        CanonicalPokemonStorageState box = storage.findOrCreate(attempt.playerId());
        boolean inParty = party.pokemonIds().contains(attempt.pokemonId());
        boolean inBox = box.pokemonIds().contains(attempt.pokemonId());
        boolean expectedParty = attempt.direction() == FileCanonicalPokemonTransferRepository.Direction.WITHDRAW;
        if (inParty != expectedParty || inBox == expectedParty) {
            throw new IllegalStateException("committed Pokemon transfer does not match canonical destination state");
        }
        return new TransferResult(attempt.direction(), attempt.pokemonId(), party.revision(), box.revision());
    }

    private CanonicalPokemonState requireOwnedPokemon(String playerId, String pokemonId) {
        CanonicalPokemonState state = pokemon.findPokemon(pokemonId)
                .orElseThrow(() -> new IllegalStateException("transfer references missing canonical Pokemon"));
        if (!state.ownerPlayerId().equals(playerId)) {
            throw new IllegalStateException("transfer references Pokemon owned by another player");
        }
        return state;
    }

    private void advance(
            FileCanonicalPokemonTransferRepository.TransferAttempt attempt,
            FileCanonicalPokemonTransferRepository.Stage next
    ) {
        if (!transfers.advance(attempt.transferId(), attempt.stage(), next)) {
            FileCanonicalPokemonTransferRepository.TransferAttempt current = requireAttempt(attempt.transferId());
            if (current.stage() != next) throw new IllegalStateException("Pokemon transfer journal stage changed unexpectedly");
        }
    }

    private FileCanonicalPokemonTransferRepository.TransferAttempt requireAttempt(String transferId) {
        return transfers.find(transferId)
                .orElseThrow(() -> new IllegalStateException("Pokemon transfer journal is missing"));
    }

    private static void requireRetryIdentity(
            FileCanonicalPokemonTransferRepository.TransferAttempt attempt,
            String playerId,
            FileCanonicalPokemonTransferRepository.Direction direction
    ) {
        if (!attempt.playerId().equals(playerId) || attempt.direction() != direction) {
            throw new IllegalStateException("transferId already belongs to a different immutable transfer intent");
        }
    }

    private static String requirePlayerId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("authenticatedPlayerId is required");
        return value.strip();
    }

    public record TransferResult(
            FileCanonicalPokemonTransferRepository.Direction direction,
            String pokemonId,
            long partyRevision,
            long storageRevision
    ) {
        public TransferResult {
            direction = Objects.requireNonNull(direction, "direction");
            if (pokemonId == null || pokemonId.isBlank()) throw new IllegalArgumentException("pokemonId is required");
            if (partyRevision < 0 || storageRevision < 0) throw new IllegalArgumentException("revisions must be non-negative");
        }
    }
}
