package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Crash-recoverable exactly-once coordinator for a trusted post-battle payload.
 *
 * <p>The battle outcome repository remains responsible for atomically applying trusted item
 * consumption and releasing battle locks. This coordinator writes the complete engine-authored
 * payload first, commits that outcome idempotently, then advances every canonical Pokemon through
 * revision CAS. A retry after process restart replays the same frozen payload and cannot reroll or
 * infer any PTU result.</p>
 */
public final class AuthoritativePostBattleTransactionService {
    private final BattleSnapshotRepository snapshotRepository;
    private final BattleOutcomeRepository outcomeRepository;
    private final BattleOutcomeCommitService outcomeCommitService;
    private final AuthoritativePostBattlePokemonCommitService pokemonCommitService;
    private final FileAuthoritativePostBattleTransactionRepository transactions;

    public AuthoritativePostBattleTransactionService(
            BattleSnapshotRepository snapshotRepository,
            BattleOutcomeRepository outcomeRepository,
            VersionedCanonicalPokemonRepository pokemonRepository,
            FileAuthoritativePostBattleTransactionRepository transactions
    ) {
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "snapshotRepository");
        this.outcomeRepository = Objects.requireNonNull(outcomeRepository, "outcomeRepository");
        this.outcomeCommitService = new BattleOutcomeCommitService(snapshotRepository, outcomeRepository);
        this.pokemonCommitService = new AuthoritativePostBattlePokemonCommitService(
                Objects.requireNonNull(pokemonRepository, "pokemonRepository"));
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    public AuthoritativePostBattleTransactionDecision commit(
            String playerId,
            String reservationId,
            String engineTranscriptDigest,
            Map<String, Integer> consumedItemQuantities,
            List<AuthoritativePostBattlePokemonFinalState> pokemonFinalStates
    ) {
        final AuthoritativePostBattleTransaction requested;
        try {
            requested = AuthoritativePostBattleTransaction.prepared(
                    reservationId, playerId, engineTranscriptDigest, consumedItemQuantities, pokemonFinalStates);
        } catch (IllegalArgumentException | NullPointerException error) {
            return AuthoritativePostBattleTransactionDecision.rejected("invalid_request");
        }

        AuthoritativePostBattleTransaction existing = transactions.find(reservationId).orElse(null);
        if (existing != null && !existing.samePayload(requested)) {
            return AuthoritativePostBattleTransactionDecision.rejected("post_battle_transaction_payload_conflict");
        }
        if (existing != null && existing.phase() == AuthoritativePostBattleTransaction.Phase.COMMITTED) {
            return AuthoritativePostBattleTransactionDecision.committed(existing, true);
        }

        String preflightError = validateFrozenAuthority(requested);
        if (preflightError != null) {
            return AuthoritativePostBattleTransactionDecision.rejected(preflightError);
        }

        if (existing == null) {
            transactions.createIfAbsent(requested);
            existing = transactions.find(reservationId).orElse(null);
            if (existing == null) {
                return AuthoritativePostBattleTransactionDecision.recoveryRequired(
                        "post_battle_transaction_journal_unavailable", requested);
            }
            if (!existing.samePayload(requested)) {
                return AuthoritativePostBattleTransactionDecision.rejected("post_battle_transaction_payload_conflict");
            }
        }

        BattleOutcomeDecision outcome = outcomeCommitService.commitEngineOutcome(
                requested.playerId(),
                requested.reservationId(),
                requested.engineTranscriptDigest(),
                requested.consumedItemQuantities());
        if (!outcome.accepted()) {
            return AuthoritativePostBattleTransactionDecision.recoveryRequired(
                    "outcome_commit_pending:" + outcome.reason(), existing);
        }

        Map<String, Long> frozenRevisions = outcome.outcome().pokemonRevisions();
        for (AuthoritativePostBattlePokemonFinalState state : requested.pokemonFinalStates()) {
            Long frozenRevision = frozenRevisions.get(state.pokemonId());
            if (frozenRevision == null || frozenRevision.longValue() != state.expectedRevision()) {
                return AuthoritativePostBattleTransactionDecision.recoveryRequired(
                        "outcome_revision_mismatch:" + state.pokemonId(), existing);
            }
            AuthoritativePostBattlePokemonCommitDecision pokemon = pokemonCommitService.commit(
                    requested.playerId(),
                    state.pokemonId(),
                    state.expectedRevision(),
                    state.health(),
                    state.statusState(),
                    state.injuryState());
            if (!pokemon.accepted()) {
                return AuthoritativePostBattleTransactionDecision.recoveryRequired(
                        "pokemon_commit_pending:" + state.pokemonId() + ":" + pokemon.reason(), existing);
            }
        }

        if (!transactions.markCommitted(reservationId)) {
            return AuthoritativePostBattleTransactionDecision.recoveryRequired(
                    "post_battle_transaction_commit_marker_pending", existing);
        }
        AuthoritativePostBattleTransaction committed = transactions.find(reservationId).orElseThrow();
        return AuthoritativePostBattleTransactionDecision.committed(committed, outcome.idempotent());
    }

    /** Replays every durable PREPARED transaction after a server restart. */
    public List<AuthoritativePostBattleTransactionDecision> recoverPending() {
        return transactions.findPending().stream()
                .map(transaction -> commit(
                        transaction.playerId(),
                        transaction.reservationId(),
                        transaction.engineTranscriptDigest(),
                        transaction.consumedItemQuantities(),
                        transaction.pokemonFinalStates()))
                .toList();
    }

    private String validateFrozenAuthority(AuthoritativePostBattleTransaction requested) {
        BattleOutcomeCommit committedOutcome = outcomeRepository.findCommittedOutcome(requested.reservationId()).orElse(null);
        if (committedOutcome != null) {
            if (!committedOutcome.playerId().equals(requested.playerId())) return "battle_reservation_not_owned";
            if (!committedOutcome.engineTranscriptDigest().equals(requested.engineTranscriptDigest())) {
                return "engine_transcript_digest_conflict";
            }
            if (!consumptionMap(committedOutcome).equals(requested.consumedItemQuantities())) {
                return "consumed_item_payload_conflict";
            }
            return validatePokemonRevisions(committedOutcome.pokemonRevisions(), requested.pokemonFinalStates());
        }

        BattleAuthoritySnapshot snapshot = snapshotRepository.findSnapshot(requested.reservationId()).orElse(null);
        if (snapshot == null) return "unknown_battle_reservation";
        if (!snapshot.playerId().equals(requested.playerId())) return "battle_reservation_not_owned";
        LinkedHashMap<String, Long> revisions = new LinkedHashMap<>();
        for (BattlePokemonSnapshot pokemon : snapshot.roster()) revisions.put(pokemon.pokemonId(), pokemon.revision());
        return validatePokemonRevisions(revisions, requested.pokemonFinalStates());
    }

    private static String validatePokemonRevisions(
            Map<String, Long> frozenRevisions,
            List<AuthoritativePostBattlePokemonFinalState> states
    ) {
        if (frozenRevisions.size() != states.size()) return "post_battle_pokemon_roster_incomplete";
        Set<String> supplied = new LinkedHashSet<>();
        for (AuthoritativePostBattlePokemonFinalState state : states) {
            supplied.add(state.pokemonId());
            Long revision = frozenRevisions.get(state.pokemonId());
            if (revision == null) return "post_battle_pokemon_not_reserved:" + state.pokemonId();
            if (revision.longValue() != state.expectedRevision()) {
                return "post_battle_pokemon_revision_mismatch:" + state.pokemonId();
            }
        }
        if (!supplied.equals(frozenRevisions.keySet())) return "post_battle_pokemon_roster_incomplete";
        return null;
    }

    private static Map<String, Integer> consumptionMap(BattleOutcomeCommit outcome) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (BattleItemConsumption consumption : outcome.consumedItems()) {
            result.put(consumption.itemInstanceId(), consumption.quantity());
        }
        return Map.copyOf(result);
    }
}
