package io.autoptu.cobblemon.authority;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Durable server-owned wallet state. This repository stores value only; it defines no shop or PTU item rules. */
public final class FileCanonicalWalletRepository {
    static final int SCHEMA_VERSION = 2;
    private static final int LEGACY_SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x4150574C; // APWL
    private static final String CURRENCY_ID = "ouros_credit";
    private final Path walletDirectory;

    public FileCanonicalWalletRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        walletDirectory = rootDirectory.toAbsolutePath().normalize().resolve("wallets");
        try {
            Files.createDirectories(walletDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical wallet store", error);
        }
    }

    public synchronized WalletState findOrCreate(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        if (Files.exists(path)) return readDocument(path, owner).state();
        WalletDocument created = new WalletDocument(new WalletState(owner, CURRENCY_ID, 0L, 0L), new LinkedHashMap<>());
        writeAtomically(path, created);
        return created.state();
    }

    public synchronized Optional<WalletState> find(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        return Files.exists(path) ? Optional.of(readDocument(path, owner).state()) : Optional.empty();
    }

    /** Revision-CAS mutation boundary retained for non-economy migration/bootstrap operations. */
    public synchronized boolean replaceIfRevision(WalletState replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        String owner = requireId(replacement.playerId(), "playerId");
        Path path = statePath(owner);
        if (!Files.exists(path)) return false;
        WalletDocument document = readDocument(path, owner);
        WalletState current = document.state();
        if (current.revision() != expectedRevision) return false;
        if (!current.currencyId().equals(replacement.currencyId())) {
            throw new IllegalArgumentException("wallet currency identity cannot change");
        }
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        writeAtomically(path, new WalletDocument(replacement, document.appliedTransactions()));
        return true;
    }

    /**
     * Commits one server-authorized currency mutation together with its idempotency receipt.
     * Wallet value and receipt share one atomic file replacement, so a crash cannot expose one without the other.
     */
    public synchronized TransactionCommitResult commitTransaction(
            String transactionId,
            String playerId,
            TransactionDirection direction,
            long amount,
            String sourceId,
            long expectedRevision
    ) {
        String txId = requireId(transactionId, "transactionId");
        String owner = requireId(playerId, "playerId");
        String source = requireId(sourceId, "sourceId");
        if (direction == null) throw new IllegalArgumentException("direction is required");
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");

        Path path = statePath(owner);
        if (!Files.exists(path)) {
            writeAtomically(path, new WalletDocument(
                    new WalletState(owner, CURRENCY_ID, 0L, 0L),
                    new LinkedHashMap<>()));
        }
        WalletDocument document = readDocument(path, owner);
        WalletState current = document.state();
        AppliedTransaction existing = document.appliedTransactions().get(txId);
        if (existing != null) {
            if (!existing.matches(direction, amount, source)) {
                return new TransactionCommitResult(TransactionCommitStatus.TRANSACTION_CONFLICT, current, existing);
            }
            return new TransactionCommitResult(TransactionCommitStatus.ALREADY_APPLIED, current, existing);
        }
        if (current.revision() != expectedRevision) {
            return new TransactionCommitResult(TransactionCommitStatus.STALE_REVISION, current, null);
        }

        long nextBalance;
        if (direction == TransactionDirection.DEBIT) {
            if (current.balance() < amount) {
                return new TransactionCommitResult(TransactionCommitStatus.INSUFFICIENT_FUNDS, current, null);
            }
            nextBalance = current.balance() - amount;
        } else {
            try {
                nextBalance = Math.addExact(current.balance(), amount);
            } catch (ArithmeticException overflow) {
                throw new IllegalArgumentException("wallet balance overflow", overflow);
            }
        }

        long nextRevision;
        try {
            nextRevision = Math.addExact(current.revision(), 1L);
        } catch (ArithmeticException overflow) {
            throw new IllegalStateException("wallet revision overflow", overflow);
        }
        WalletState updated = new WalletState(owner, current.currencyId(), nextBalance, nextRevision);
        AppliedTransaction receipt = new AppliedTransaction(
                txId,
                direction,
                amount,
                source,
                current.balance(),
                nextBalance,
                nextRevision
        );
        LinkedHashMap<String, AppliedTransaction> receipts = new LinkedHashMap<>(document.appliedTransactions());
        receipts.put(txId, receipt);
        writeAtomically(path, new WalletDocument(updated, receipts));
        return new TransactionCommitResult(TransactionCommitStatus.APPLIED, updated, receipt);
    }

    public synchronized Optional<AppliedTransaction> findAppliedTransaction(String playerId, String transactionId) {
        String owner = requireId(playerId, "playerId");
        String txId = requireId(transactionId, "transactionId");
        Path path = statePath(owner);
        if (!Files.exists(path)) return Optional.empty();
        return Optional.ofNullable(readDocument(path, owner).appliedTransactions().get(txId));
    }

    private WalletDocument readDocument(Path path, String expectedPlayerId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical wallet file magic");
                int schemaVersion = input.readInt();
                if (schemaVersion != LEGACY_SCHEMA_VERSION && schemaVersion != SCHEMA_VERSION) {
                    throw new IllegalStateException("unsupported canonical wallet schema version");
                }
                WalletState state = new WalletState(input.readUTF(), input.readUTF(), input.readLong(), input.readLong());
                if (!state.playerId().equals(expectedPlayerId)) throw new IllegalStateException("canonical wallet owner mismatch");
                LinkedHashMap<String, AppliedTransaction> receipts = new LinkedHashMap<>();
                if (schemaVersion >= 2) {
                    int count = input.readInt();
                    if (count < 0) throw new IllegalStateException("invalid canonical wallet transaction count");
                    for (int i = 0; i < count; i++) {
                        AppliedTransaction receipt = new AppliedTransaction(
                                input.readUTF(),
                                TransactionDirection.valueOf(input.readUTF()),
                                input.readLong(),
                                input.readUTF(),
                                input.readLong(),
                                input.readLong(),
                                input.readLong()
                        );
                        if (receipts.put(receipt.transactionId(), receipt) != null) {
                            throw new IllegalStateException("duplicate canonical wallet transaction id");
                        }
                    }
                }
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical wallet data");
                return new WalletDocument(state, receipts);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical wallet", error);
        }
    }

    private void writeAtomically(Path target, WalletDocument document) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(walletDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(document));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical wallet store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical wallet", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private byte[] encode(WalletDocument document) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                WalletState state = document.state();
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeUTF(state.currencyId());
                output.writeLong(state.balance());
                output.writeLong(state.revision());
                output.writeInt(document.appliedTransactions().size());
                for (AppliedTransaction receipt : document.appliedTransactions().values()) {
                    output.writeUTF(receipt.transactionId());
                    output.writeUTF(receipt.direction().name());
                    output.writeLong(receipt.amount());
                    output.writeUTF(receipt.sourceId());
                    output.writeLong(receipt.balanceBefore());
                    output.writeLong(receipt.balanceAfter());
                    output.writeLong(receipt.resultingRevision());
                }
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical wallet", error);
        }
    }

    private Path statePath(String playerId) {
        return walletDirectory.resolve(fileKey(playerId) + ".bin");
    }

    private static String fileKey(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", error);
        }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private record WalletDocument(WalletState state, Map<String, AppliedTransaction> appliedTransactions) {
        private WalletDocument {
            if (state == null) throw new IllegalArgumentException("state is required");
            if (appliedTransactions == null) throw new IllegalArgumentException("appliedTransactions is required");
            appliedTransactions = Map.copyOf(appliedTransactions);
        }
    }

    public record WalletState(String playerId, String currencyId, long balance, long revision) {
        public WalletState {
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId must not be blank");
            if (currencyId == null || currencyId.isBlank()) throw new IllegalArgumentException("currencyId must not be blank");
            if (balance < 0) throw new IllegalArgumentException("wallet balance must not be negative");
            if (revision < 0) throw new IllegalArgumentException("wallet revision must not be negative");
            playerId = playerId.trim();
            currencyId = currencyId.trim();
        }
    }

    public enum TransactionDirection {
        CREDIT,
        DEBIT
    }

    public enum TransactionCommitStatus {
        APPLIED,
        ALREADY_APPLIED,
        INSUFFICIENT_FUNDS,
        STALE_REVISION,
        TRANSACTION_CONFLICT
    }

    public record AppliedTransaction(
            String transactionId,
            TransactionDirection direction,
            long amount,
            String sourceId,
            long balanceBefore,
            long balanceAfter,
            long resultingRevision
    ) {
        public AppliedTransaction {
            transactionId = requireId(transactionId, "transactionId");
            if (direction == null) throw new IllegalArgumentException("direction is required");
            if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
            sourceId = requireId(sourceId, "sourceId");
            if (balanceBefore < 0 || balanceAfter < 0) throw new IllegalArgumentException("transaction balances must not be negative");
            if (resultingRevision <= 0) throw new IllegalArgumentException("resultingRevision must be positive");
        }

        boolean matches(TransactionDirection expectedDirection, long expectedAmount, String expectedSourceId) {
            return direction == expectedDirection && amount == expectedAmount && sourceId.equals(expectedSourceId);
        }
    }

    public record TransactionCommitResult(
            TransactionCommitStatus status,
            WalletState wallet,
            AppliedTransaction transaction
    ) {
        public TransactionCommitResult {
            if (status == null) throw new IllegalArgumentException("status is required");
            if (wallet == null) throw new IllegalArgumentException("wallet is required");
        }
    }
}
