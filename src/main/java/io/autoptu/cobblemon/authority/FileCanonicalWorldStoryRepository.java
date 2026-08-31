package io.autoptu.cobblemon.authority;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Durable owner-scoped world story choices and server-authored consequence flags. */
public final class FileCanonicalWorldStoryRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41545354; // ATST
    private static final int MAX_ENTRIES = 256;
    private final Path storyDirectory;

    public FileCanonicalWorldStoryRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        storyDirectory = rootDirectory.toAbsolutePath().normalize().resolve("world-story-state");
        try {
            Files.createDirectories(storyDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical world story store", error);
        }
    }

    public synchronized StoryState findOrCreate(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        if (Files.exists(path)) return read(path, owner);
        StoryState created = new StoryState(owner, Map.of(), Set.of(), 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<StoryState> find(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        return Files.exists(path) ? Optional.of(read(path, owner)) : Optional.empty();
    }

    public synchronized boolean replaceIfRevision(StoryState replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        String owner = requireId(replacement.playerId(), "playerId");
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        Path path = statePath(owner);
        if (!Files.exists(path)) return false;
        StoryState current = read(path, owner);
        if (current.revision() != expectedRevision) return false;
        writeAtomically(path, replacement);
        return true;
    }

    private StoryState read(Path path, String expectedPlayerId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical world story file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical world story schema version");
                String playerId = input.readUTF();
                int choiceCount = boundedCount(input.readInt(), "choice");
                Map<String, String> choices = new LinkedHashMap<>();
                for (int i = 0; i < choiceCount; i++) choices.put(input.readUTF(), input.readUTF());
                int flagCount = boundedCount(input.readInt(), "flag");
                Set<String> flags = new LinkedHashSet<>();
                for (int i = 0; i < flagCount; i++) flags.add(input.readUTF());
                long revision = input.readLong();
                StoryState state = new StoryState(playerId, choices, flags, revision);
                if (!state.playerId().equals(expectedPlayerId)) throw new IllegalStateException("canonical world story owner mismatch");
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical world story data");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical world story state", error);
        }
    }

    private void writeAtomically(Path target, StoryState state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(storyDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical world story store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical world story state", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private byte[] encode(StoryState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeInt(state.selectedChoices().size());
                for (var entry : state.selectedChoices().entrySet()) {
                    output.writeUTF(entry.getKey());
                    output.writeUTF(entry.getValue());
                }
                output.writeInt(state.storyFlags().size());
                for (String flag : state.storyFlags()) output.writeUTF(flag);
                output.writeLong(state.revision());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical world story state", error);
        }
    }

    private Path statePath(String playerId) { return storyDirectory.resolve(fileKey(playerId) + ".bin"); }

    private static int boundedCount(int count, String field) {
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalStateException("invalid canonical world story " + field + " count");
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

    public record StoryState(String playerId, Map<String, String> selectedChoices, Set<String> storyFlags, long revision) {
        public StoryState {
            playerId = requireId(playerId, "playerId");
            selectedChoices = Map.copyOf(selectedChoices == null ? Map.of() : selectedChoices);
            storyFlags = Set.copyOf(storyFlags == null ? Set.of() : storyFlags);
            if (selectedChoices.size() > MAX_ENTRIES || storyFlags.size() > MAX_ENTRIES) throw new IllegalArgumentException("canonical world story state exceeds capacity");
            selectedChoices.forEach((node, choice) -> { requireId(node, "nodeId"); requireId(choice, "choiceId"); });
            for (String flag : storyFlags) requireId(flag, "storyFlag");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
