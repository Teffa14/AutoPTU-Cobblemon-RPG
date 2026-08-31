package io.autoptu.cobblemon.authority;

import java.util.Objects;

/**
 * Claims authored quest rewards from durable server-owned quest state.
 * The caller supplies only player and quest identity; reward value and eligibility stay authoritative.
 */
public final class CanonicalQuestRewardService {
    private final CanonicalQuestRewardCatalogue rewards;
    private final FileCanonicalQuestJournalRepository journals;
    private final CanonicalQuestObjectiveService objectives;
    private final CanonicalWalletTransactionService wallet;

    public CanonicalQuestRewardService(
            CanonicalQuestRewardCatalogue rewards,
            FileCanonicalQuestJournalRepository journals,
            CanonicalQuestObjectiveService objectives,
            FileCanonicalWalletRepository wallets
    ) {
        this.rewards = Objects.requireNonNull(rewards, "rewards");
        this.journals = Objects.requireNonNull(journals, "journals");
        this.objectives = Objects.requireNonNull(objectives, "objectives");
        this.wallet = new CanonicalWalletTransactionService(Objects.requireNonNull(wallets, "wallets"));
    }

    public ClaimResult claim(String playerId, String questId) {
        String normalizedPlayerId = requireText(playerId, "playerId");
        String normalizedQuestId = requireText(questId, "questId");
        CanonicalQuestRewardCatalogue.Reward reward = rewards.reward(normalizedQuestId).orElse(null);
        if (reward == null) return new ClaimResult(Status.NO_AUTHORED_REWARD, null, null);

        var journal = journals.findOrCreate(normalizedPlayerId);
        if (!journal.entries().containsKey(normalizedQuestId)) {
            return new ClaimResult(Status.QUEST_NOT_ACCEPTED, reward, null);
        }
        var progress = objectives.inspectQuest(normalizedPlayerId, normalizedQuestId);
        if (!progress.complete()) {
            return new ClaimResult(Status.OBJECTIVES_INCOMPLETE, reward, null);
        }

        var transaction = wallet.credit(
                reward.transactionId(),
                normalizedPlayerId,
                reward.amount(),
                reward.sourceId()
        );
        if (!reward.currencyId().equals(transaction.currencyId())) {
            throw new IllegalStateException("authored quest reward currency does not match canonical wallet currency");
        }
        Status status = switch (transaction.status()) {
            case APPLIED -> Status.APPLIED;
            case ALREADY_APPLIED -> Status.ALREADY_APPLIED;
            case TRANSACTION_CONFLICT -> Status.TRANSACTION_CONFLICT;
            case RETRY_EXHAUSTED -> Status.RETRY_EXHAUSTED;
            case INSUFFICIENT_FUNDS -> throw new IllegalStateException("credit cannot fail for insufficient funds");
        };
        return new ClaimResult(status, reward, transaction);
    }

    public enum Status {
        APPLIED,
        ALREADY_APPLIED,
        QUEST_NOT_ACCEPTED,
        OBJECTIVES_INCOMPLETE,
        NO_AUTHORED_REWARD,
        TRANSACTION_CONFLICT,
        RETRY_EXHAUSTED
    }

    public record ClaimResult(
            Status status,
            CanonicalQuestRewardCatalogue.Reward reward,
            CanonicalWalletTransactionService.TransactionResult transaction
    ) {
        public ClaimResult {
            status = Objects.requireNonNull(status, "status");
        }
        public boolean committed() {
            return status == Status.APPLIED || status == Status.ALREADY_APPLIED;
        }
        public boolean newlyApplied() {
            return status == Status.APPLIED;
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
