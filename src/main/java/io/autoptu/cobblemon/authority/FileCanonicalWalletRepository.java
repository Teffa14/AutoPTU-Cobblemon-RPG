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
import java.util.Optional;

/** Durable server-owned wallet state. This repository stores value only; it defines no shop or PTU item rules. */
public final class FileCanonicalWalletRepository {
    static final int SCHEMA_VERSION = 1;
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
        String owner = requireId(playerId);
        Path path = statePath(owner);
        if (Files.exists(path)) return read(path, owner);
        WalletState created = new WalletState(owner, CURRENCY_ID, 0L, 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<WalletState> find(String playerId) {
        String owner = requireId(playerId);
        Path path = statePath(owner);
        return Files.exists(path) ? Optional.of(read(path, owner)) : Optional.empty();
    }

    /** Revision-CAS mutation boundary for future server-owned economy services. */
    public synchronized boolean replaceIfRevision(WalletState replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        String owner = requireId(replacement.playerId());
        Path path = statePath(owner);
        if (!Files.exists(path)) return false;
        WalletState current = read(path, owner);
        if (current.revision() != expectedRevision) return false;
        if (!current.currencyId().equals(replacement.currencyId())) {
            throw new IllegalArgumentException("wallet currency identity cannot change");
        }
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        writeAtomically(path, replacement);
        return true;
    }

    private WalletState read(Path path, String expectedPlayerId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical wallet file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical wallet schema version");
                WalletState state = new WalletState(input.readUTF(), input.readUTF(), input.readLong(), input.readLong());
                if (!state.playerId().equals(expectedPlayerId)) throw new IllegalStateException("canonical wallet owner mismatch");
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical wallet data");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical wallet", error);
        }
    }

    private void writeAtomically(Path target, WalletState state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(walletDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
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

    private byte[] encode(WalletState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeUTF(state.currencyId());
                output.writeLong(state.balance());
                output.writeLong(state.revision());
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

    private static String requireId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("playerId must not be blank");
        return value.trim();
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
}
