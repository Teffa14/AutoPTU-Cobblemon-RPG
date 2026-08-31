package io.autoptu.cobblemon.authority;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Durable owner-scoped read/claim state for server-authored RPG mail. */
public final class FileCanonicalMailRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41544d4c; // ATML
    private static final int MAX_ENTRIES = 256;
    private final Path mailDirectory;

    public FileCanonicalMailRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        mailDirectory = rootDirectory.toAbsolutePath().normalize().resolve("mail-state");
        try {
            Files.createDirectories(mailDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical mail store", error);
        }
    }

    public synchronized MailState findOrCreate(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        if (Files.exists(path)) return read(path, owner);
        MailState created = new MailState(owner, Set.of(), Set.of(), 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<MailState> find(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        return Files.exists(path) ? Optional.of(read(path, owner)) : Optional.empty();
    }

    public synchronized boolean replaceIfRevision(MailState replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        String owner = requireId(replacement.playerId(), "playerId");
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        Path path = statePath(owner);
        if (!Files.exists(path)) return false;
        MailState current = read(path, owner);
        if (current.revision() != expectedRevision) return false;
        writeAtomically(path, replacement);
        return true;
    }

    private MailState read(Path path, String expectedPlayerId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical mail file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical mail schema version");
                String playerId = input.readUTF();
                int readCount = boundedCount(input.readInt(), "read");
                Set<String> readIds = new LinkedHashSet<>();
                for (int i = 0; i < readCount; i++) readIds.add(input.readUTF());
                int claimCount = boundedCount(input.readInt(), "claim");
                Set<String> claimedIds = new LinkedHashSet<>();
                for (int i = 0; i < claimCount; i++) claimedIds.add(input.readUTF());
                long revision = input.readLong();
                MailState state = new MailState(playerId, readIds, claimedIds, revision);
                if (!state.playerId().equals(expectedPlayerId)) throw new IllegalStateException("canonical mail owner mismatch");
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical mail data");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical mail state", error);
        }
    }

    private void writeAtomically(Path target, MailState state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(mailDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical mail store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical mail state", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private byte[] encode(MailState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeInt(state.readMailIds().size());
                for (String id : state.readMailIds()) output.writeUTF(id);
                output.writeInt(state.claimedRewardMailIds().size());
                for (String id : state.claimedRewardMailIds()) output.writeUTF(id);
                output.writeLong(state.revision());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical mail state", error);
        }
    }

    private Path statePath(String playerId) { return mailDirectory.resolve(fileKey(playerId) + ".bin"); }

    private static int boundedCount(int count, String field) {
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalStateException("invalid canonical mail " + field + " count");
        return count;
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
        return value.strip();
    }

    public record MailState(String playerId, Set<String> readMailIds, Set<String> claimedRewardMailIds, long revision) {
        public MailState {
            playerId = requireId(playerId, "playerId");
            readMailIds = Set.copyOf(readMailIds == null ? Set.of() : readMailIds);
            claimedRewardMailIds = Set.copyOf(claimedRewardMailIds == null ? Set.of() : claimedRewardMailIds);
            if (readMailIds.size() > MAX_ENTRIES || claimedRewardMailIds.size() > MAX_ENTRIES) {
                throw new IllegalArgumentException("canonical mail state exceeds capacity");
            }
            for (String id : readMailIds) requireId(id, "mailId");
            for (String id : claimedRewardMailIds) requireId(id, "mailId");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
