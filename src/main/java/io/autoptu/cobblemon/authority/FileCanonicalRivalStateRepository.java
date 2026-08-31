package io.autoptu.cobblemon.authority;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Durable owner-scoped RPG rival identity, authored history keys and story flags. */
public final class FileCanonicalRivalStateRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41545256; // ATRV
    private static final int MAX_ENTRIES = 256;
    private final Path rivalDirectory;

    public FileCanonicalRivalStateRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        rivalDirectory = rootDirectory.toAbsolutePath().normalize().resolve("rival-state");
        try {
            Files.createDirectories(rivalDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical rival state store", error);
        }
    }

    /** Creates an empty narrative shell only; it does not imply meeting, battle history or outcomes. */
    public synchronized RivalState findOrCreate(String playerId, String rivalId) {
        String owner = requireId(playerId, "playerId");
        String rival = requireId(rivalId, "rivalId");
        Path path = statePath(owner, rival);
        if (Files.exists(path)) return read(path, owner, rival);
        RivalState created = new RivalState(owner, rival, List.of(), Set.of(), 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<RivalState> find(String playerId, String rivalId) {
        String owner = requireId(playerId, "playerId");
        String rival = requireId(rivalId, "rivalId");
        Path path = statePath(owner, rival);
        return Files.exists(path) ? Optional.of(read(path, owner, rival)) : Optional.empty();
    }

    /** Revision-CAS boundary for future server-observed authored story events. */
    public synchronized boolean replaceIfRevision(RivalState replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        String owner = requireId(replacement.playerId(), "playerId");
        String rival = requireId(replacement.rivalId(), "rivalId");
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        Path path = statePath(owner, rival);
        if (!Files.exists(path)) return false;
        RivalState current = read(path, owner, rival);
        if (current.revision() != expectedRevision) return false;
        writeAtomically(path, replacement);
        return true;
    }

    private RivalState read(Path path, String expectedPlayerId, String expectedRivalId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical rival state file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical rival state schema version");
                String playerId = input.readUTF();
                String rivalId = input.readUTF();
                int historyCount = boundedCount(input.readInt(), "history");
                var history = new java.util.ArrayList<String>(historyCount);
                for (int i = 0; i < historyCount; i++) history.add(input.readUTF());
                int flagCount = boundedCount(input.readInt(), "story flags");
                var flags = new LinkedHashSet<String>();
                for (int i = 0; i < flagCount; i++) flags.add(input.readUTF());
                long revision = input.readLong();
                RivalState state = new RivalState(playerId, rivalId, history, flags, revision);
                if (!state.playerId().equals(expectedPlayerId) || !state.rivalId().equals(expectedRivalId)) {
                    throw new IllegalStateException("canonical rival state identity mismatch");
                }
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical rival state data");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical rival state", error);
        }
    }

    private void writeAtomically(Path target, RivalState state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(rivalDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical rival state store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical rival state", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private byte[] encode(RivalState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeUTF(state.rivalId());
                output.writeInt(state.historyEventKeys().size());
                for (String event : state.historyEventKeys()) output.writeUTF(event);
                output.writeInt(state.storyFlags().size());
                for (String flag : state.storyFlags()) output.writeUTF(flag);
                output.writeLong(state.revision());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical rival state", error);
        }
    }

    private Path statePath(String playerId, String rivalId) {
        return rivalDirectory.resolve(fileKey(playerId + "\u0000" + rivalId) + ".bin");
    }

    private static int boundedCount(int count, String field) {
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalStateException("invalid canonical rival " + field + " count");
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
        return value.trim();
    }

    public record RivalState(String playerId, String rivalId, List<String> historyEventKeys, Set<String> storyFlags, long revision) {
        public RivalState {
            playerId = requireId(playerId, "playerId");
            rivalId = requireId(rivalId, "rivalId");
            historyEventKeys = List.copyOf(historyEventKeys == null ? List.of() : historyEventKeys);
            storyFlags = Set.copyOf(storyFlags == null ? Set.of() : storyFlags);
            if (historyEventKeys.size() > MAX_ENTRIES || storyFlags.size() > MAX_ENTRIES) {
                throw new IllegalArgumentException("canonical rival state exceeds bounded history/flag capacity");
            }
            for (String event : historyEventKeys) requireId(event, "historyEventKey");
            for (String flag : storyFlags) requireId(flag, "storyFlag");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
