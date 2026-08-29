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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Durable owner-scoped quest journal. It persists RPG progression only and applies no PTU rules or rewards. */
public final class FileCanonicalQuestJournalRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x4150514a; // APQJ
    private final Path journalDirectory;

    public FileCanonicalQuestJournalRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        journalDirectory = rootDirectory.toAbsolutePath().normalize().resolve("quest-journals");
        try {
            Files.createDirectories(journalDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical quest journal store", error);
        }
    }

    public synchronized JournalState findOrCreate(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        if (Files.exists(path)) return read(path, owner);
        JournalState created = new JournalState(owner, 0L, Map.of());
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<JournalState> find(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        return Files.exists(path) ? Optional.of(read(path, owner)) : Optional.empty();
    }

    public synchronized AcceptResult accept(String playerId, String questId, long expectedRevision) {
        String owner = requireId(playerId, "playerId");
        String quest = requireId(questId, "questId");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        JournalState current = findOrCreate(owner);
        QuestEntry existing = current.entries().get(quest);
        if (existing != null) return new AcceptResult(AcceptStatus.ALREADY_ACCEPTED, current, existing);
        if (current.revision() != expectedRevision) return new AcceptResult(AcceptStatus.STALE_REVISION, current, null);
        long nextRevision = Math.addExact(current.revision(), 1L);
        QuestEntry entry = new QuestEntry(quest, QuestState.ACCEPTED, nextRevision);
        LinkedHashMap<String, QuestEntry> entries = new LinkedHashMap<>(current.entries());
        entries.put(quest, entry);
        JournalState updated = new JournalState(owner, nextRevision, entries);
        writeAtomically(statePath(owner), updated);
        return new AcceptResult(AcceptStatus.ACCEPTED, updated, entry);
    }

    private JournalState read(Path path, String expectedPlayerId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical quest journal file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical quest journal schema version");
                String playerId = input.readUTF();
                if (!playerId.equals(expectedPlayerId)) throw new IllegalStateException("canonical quest journal owner mismatch");
                long revision = input.readLong();
                int count = input.readInt();
                if (count < 0) throw new IllegalStateException("invalid canonical quest entry count");
                LinkedHashMap<String, QuestEntry> entries = new LinkedHashMap<>();
                for (int i = 0; i < count; i++) {
                    QuestEntry entry = new QuestEntry(input.readUTF(), QuestState.valueOf(input.readUTF()), input.readLong());
                    if (entries.put(entry.questId(), entry) != null) throw new IllegalStateException("duplicate canonical quest entry");
                }
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical quest journal data");
                return new JournalState(playerId, revision, entries);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical quest journal", error);
        }
    }

    private void writeAtomically(Path target, JournalState state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(journalDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical quest journal store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical quest journal", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private byte[] encode(JournalState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeLong(state.revision());
                output.writeInt(state.entries().size());
                for (QuestEntry entry : state.entries().values()) {
                    output.writeUTF(entry.questId());
                    output.writeUTF(entry.state().name());
                    output.writeLong(entry.acceptedRevision());
                }
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical quest journal", error);
        }
    }

    private Path statePath(String playerId) {
        return journalDirectory.resolve(fileKey(playerId) + ".bin");
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

    public record JournalState(String playerId, long revision, Map<String, QuestEntry> entries) {
        public JournalState {
            playerId = requireId(playerId, "playerId");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
            if (entries == null) throw new IllegalArgumentException("entries are required");
            entries = Map.copyOf(entries);
        }
    }

    public record QuestEntry(String questId, QuestState state, long acceptedRevision) {
        public QuestEntry {
            questId = requireId(questId, "questId");
            if (state == null) throw new IllegalArgumentException("state is required");
            if (acceptedRevision <= 0) throw new IllegalArgumentException("acceptedRevision must be positive");
        }
    }

    public enum QuestState { ACCEPTED }
    public enum AcceptStatus { ACCEPTED, ALREADY_ACCEPTED, STALE_REVISION }

    public record AcceptResult(AcceptStatus status, JournalState journal, QuestEntry entry) {
        public AcceptResult {
            if (status == null) throw new IllegalArgumentException("status is required");
            if (journal == null) throw new IllegalArgumentException("journal is required");
        }
    }
}
