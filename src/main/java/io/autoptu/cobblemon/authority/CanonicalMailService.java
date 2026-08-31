package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Reusable server authority for RPG inbox reads and exactly-once authored mail rewards. */
public final class CanonicalMailService {
    private static final int MAX_CAS_RETRIES = 8;

    private final CanonicalMailCatalogue catalogue;
    private final FileCanonicalMailRepository repository;
    private final CanonicalWalletTransactionService wallet;

    public CanonicalMailService(
            CanonicalMailCatalogue catalogue,
            FileCanonicalMailRepository repository,
            FileCanonicalWalletRepository wallets
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.wallet = new CanonicalWalletTransactionService(Objects.requireNonNull(wallets, "wallets"));
    }

    public Inbox inspect(String playerId) {
        String owner = requireText(playerId, "playerId");
        var state = repository.findOrCreate(owner);
        List<MessageView> messages = new ArrayList<>();
        for (var mail : catalogue.messages()) {
            messages.add(view(mail, state));
        }
        return new Inbox(owner, messages, state.revision());
    }

    public ReadResult read(String playerId, String mailId) {
        String owner = requireText(playerId, "playerId");
        CanonicalMailCatalogue.Mail mail = requireMail(mailId);
        for (int attempt = 0; attempt < MAX_CAS_RETRIES; attempt++) {
            var current = repository.findOrCreate(owner);
            if (current.readMailIds().contains(mail.mailId())) {
                return new ReadResult(false, view(mail, current), current.revision());
            }
            var readIds = new LinkedHashSet<>(current.readMailIds());
            readIds.add(mail.mailId());
            var replacement = new FileCanonicalMailRepository.MailState(
                    owner, readIds, current.claimedRewardMailIds(), current.revision() + 1);
            if (repository.replaceIfRevision(replacement, current.revision())) {
                return new ReadResult(true, view(mail, replacement), replacement.revision());
            }
        }
        throw new IllegalStateException("canonical mail read retry exhausted");
    }

    public ClaimResult claimReward(String playerId, String mailId) {
        String owner = requireText(playerId, "playerId");
        CanonicalMailCatalogue.Mail mail = requireMail(mailId);
        CanonicalMailCatalogue.Reward reward = mail.reward();
        if (reward == null) return new ClaimResult(ClaimStatus.NO_REWARD, view(mail, repository.findOrCreate(owner)), null);

        var initial = repository.findOrCreate(owner);
        if (initial.claimedRewardMailIds().contains(mail.mailId())) {
            return new ClaimResult(ClaimStatus.ALREADY_CLAIMED, view(mail, initial), null);
        }

        var transaction = wallet.credit(
                "mail-reward:" + mail.mailId(),
                owner,
                reward.amount(),
                reward.sourceId());
        if (!reward.currencyId().equals(transaction.currencyId())) {
            throw new IllegalStateException("authored mail reward currency does not match canonical wallet currency");
        }
        if (transaction.status() == CanonicalWalletTransactionService.Status.TRANSACTION_CONFLICT) {
            return new ClaimResult(ClaimStatus.TRANSACTION_CONFLICT, view(mail, repository.findOrCreate(owner)), transaction);
        }
        if (transaction.status() == CanonicalWalletTransactionService.Status.RETRY_EXHAUSTED) {
            return new ClaimResult(ClaimStatus.RETRY_EXHAUSTED, view(mail, repository.findOrCreate(owner)), transaction);
        }
        if (transaction.status() == CanonicalWalletTransactionService.Status.INSUFFICIENT_FUNDS) {
            throw new IllegalStateException("mail credit cannot fail for insufficient funds");
        }

        for (int attempt = 0; attempt < MAX_CAS_RETRIES; attempt++) {
            var current = repository.findOrCreate(owner);
            if (current.claimedRewardMailIds().contains(mail.mailId())) {
                return new ClaimResult(ClaimStatus.ALREADY_CLAIMED, view(mail, current), transaction);
            }
            var claimedIds = new LinkedHashSet<>(current.claimedRewardMailIds());
            claimedIds.add(mail.mailId());
            var replacement = new FileCanonicalMailRepository.MailState(
                    owner, current.readMailIds(), claimedIds, current.revision() + 1);
            if (repository.replaceIfRevision(replacement, current.revision())) {
                return new ClaimResult(
                        transaction.status() == CanonicalWalletTransactionService.Status.APPLIED
                                ? ClaimStatus.APPLIED : ClaimStatus.RECOVERED_AFTER_WALLET_COMMIT,
                        view(mail, replacement),
                        transaction);
            }
        }
        throw new IllegalStateException("canonical mail claim retry exhausted after wallet commit");
    }

    private CanonicalMailCatalogue.Mail requireMail(String mailId) {
        String normalized = requireText(mailId, "mailId");
        return catalogue.message(normalized)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical mail: " + normalized));
    }

    private static MessageView view(CanonicalMailCatalogue.Mail mail, FileCanonicalMailRepository.MailState state) {
        return new MessageView(
                mail.mailId(),
                mail.sender(),
                mail.subject(),
                mail.body(),
                state.readMailIds().contains(mail.mailId()),
                mail.reward() != null,
                state.claimedRewardMailIds().contains(mail.mailId()),
                mail.reward() == null ? null : mail.reward().currencyId(),
                mail.reward() == null ? 0L : mail.reward().amount());
    }

    public record Inbox(String playerId, List<MessageView> messages, long revision) {
        public Inbox {
            playerId = requireText(playerId, "playerId");
            messages = List.copyOf(messages == null ? List.of() : messages);
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }
    }

    public record MessageView(
            String mailId,
            String sender,
            String subject,
            String body,
            boolean read,
            boolean hasReward,
            boolean rewardClaimed,
            String rewardCurrencyId,
            long rewardAmount
    ) { }

    public record ReadResult(boolean newlyRead, MessageView message, long revision) { }

    public enum ClaimStatus {
        APPLIED,
        RECOVERED_AFTER_WALLET_COMMIT,
        ALREADY_CLAIMED,
        NO_REWARD,
        TRANSACTION_CONFLICT,
        RETRY_EXHAUSTED
    }

    public record ClaimResult(
            ClaimStatus status,
            MessageView message,
            CanonicalWalletTransactionService.TransactionResult transaction
    ) {
        public ClaimResult {
            status = Objects.requireNonNull(status, "status");
            message = Objects.requireNonNull(message, "message");
        }
        public boolean committed() {
            return status == ClaimStatus.APPLIED
                    || status == ClaimStatus.RECOVERED_AFTER_WALLET_COMMIT
                    || status == ClaimStatus.ALREADY_CLAIMED;
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
