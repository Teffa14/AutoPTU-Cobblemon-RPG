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
    static final int SCHEMA_VERSION = 2;
    private static final int LEGACY_SCHEMA_VERSION = 1;
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
        JournalState created = new JournalState(owner, 0L, Map.of(), null);
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
        JournalState updated = new JournalState(owner, nextRevision, entries, current.trackedQuestId());
        writeAtomically(statePath(owner), updated);
        return new AcceptResult(AcceptStatus.ACCEPTED, updated, entry);
    }

    public synchronized TrackResult track(String playerId, String questId, long expectedRevision) {
        String owner = requireId(playerId, "playerId");
        String quest = requireId(questId, "questId");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        JournalState current = findOrCreate(owner);
        if (!current.entries().containsKey(quest)) {
            return new TrackResult(TrackStatus.NOT_ACCEPTED, current);
        }
        if (quest.equals(current.trackedQuestId())) {
            return new TrackResult(TrackStatus.ALREADY_TRACKED, current);
        }
        if (current.revision() != expectedRevision) {
            return new TrackResult(TrackStatus.STALE_REVISION, current);
        }
        long nextRevision = Math.addExact(current.revision(), 1L);
        JournalState updated = new JournalState(owner, nextRevision, current.entries(), quest);
        writeAtomically(statePath(owner), updated);
        return new TrackResult(TrackStatus.TRACKED, updated);
    }

    private JournalState read(Path path, String expectedPlayerId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical quest journal file magic");
                int schemaVersion = input.readInt();
                if (schemaVersion != LEGACY_SCHEMA_VERSION && schemaVersion != SCHEMA_VERSION) {
                    throw new IllegalStateException("unsupported canonical quest journal schema version");
                }
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
                String trackedQuestId = null;
                if (schemaVersion >= 2 && input.readBoolean()) trackedQuestId = input.readUTF();
                if (trackedQuestId != null && !entries.containsKey(trackedQuestId)) {
                    throw new IllegalStateException("tracked canonical quest is not present in the journal");
                }
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical quest journal data");
                return new JournalState(playerId, revision, entries, trackedQuestId);
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
                output.writeBoolean(state.trackedQuestId() != null);
                if (state.trackedQuestId() != null) output.writeUTF(state.trackedQuestId());
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

    public record JournalState(String playerId, long revision, Map<String, QuestEntry> entries, String trackedQuestId) {
        public JournalState {
            playerId = requireId(playerId, "playerId");
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
            if (entries == null) throw new IllegalArgumentException("entries are required");
            entries = Map.copyOf(entries);
            if (trackedQuestId != null) {
                trackedQuestId = requireId(trackedQuestId, "trackedQuestId");
                if (!entries.containsKey(trackedQuestId)) {
                    throw new IllegalArgumentException("trackedQuestId must reference an accepted journal quest");
                }
            }
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
    public enum TrackStatus { TRACKED, ALREADY_TRACKED, NOT_ACCEPTED, STALE_REVISION }

    public record AcceptResult(AcceptStatus status, JournalState journal, QuestEntry entry) {
        public AcceptResult {
            if (status == null) throw new IllegalArgumentException("status is required");
            if (journal == null) throw new IllegalArgumentException("journal is required");
        }
    }

    public record TrackResult(TrackStatus status, JournalState journal) {
        public TrackResult {
            if (status == null) throw new IllegalArgumentException("status is required");
            if (journal == null) throw new IllegalArgumentException("journal is required");
        }
    }
}
