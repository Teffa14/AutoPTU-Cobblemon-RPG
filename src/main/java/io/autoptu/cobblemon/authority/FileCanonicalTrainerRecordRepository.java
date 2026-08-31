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

/** Durable owner-scoped RPG records. Battle-derived wins/losses are written only by future trusted outcome services. */
public final class FileCanonicalTrainerRecordRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41545243; // ATRC
    private static final int MAX_ENTRIES = 512;
    private final Path recordDirectory;

    public FileCanonicalTrainerRecordRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        recordDirectory = rootDirectory.toAbsolutePath().normalize().resolve("trainer-records");
        try {
            Files.createDirectories(recordDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical trainer record store", error);
        }
    }

    public synchronized TrainerRecord findOrCreate(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        if (Files.exists(path)) return read(path, owner);
        TrainerRecord created = new TrainerRecord(owner, 0L, 0L, Set.of(), List.of(), 0L);
        writeAtomically(path, created);
        return created;
    }

    public synchronized Optional<TrainerRecord> find(String playerId) {
        String owner = requireId(playerId, "playerId");
        Path path = statePath(owner);
        return Files.exists(path) ? Optional.of(read(path, owner)) : Optional.empty();
    }

    /** CAS boundary for explicit server-owned badge/tournament facts and authoritative battle-result commits. */
    public synchronized boolean replaceIfRevision(TrainerRecord replacement, long expectedRevision) {
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        String owner = requireId(replacement.playerId(), "playerId");
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must increment by exactly one");
        }
        Path path = statePath(owner);
        if (!Files.exists(path)) return false;
        TrainerRecord current = read(path, owner);
        if (current.revision() != expectedRevision) return false;
        writeAtomically(path, replacement);
        return true;
    }

    private TrainerRecord read(Path path, String expectedPlayerId) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical trainer record file magic");
                if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical trainer record schema version");
                String playerId = input.readUTF();
                long wins = input.readLong();
                long losses = input.readLong();
                int badgeCount = boundedCount(input.readInt(), "badges");
                var badges = new LinkedHashSet<String>();
                for (int i = 0; i < badgeCount; i++) badges.add(input.readUTF());
                int tournamentCount = boundedCount(input.readInt(), "tournaments");
                var tournaments = new java.util.ArrayList<String>(tournamentCount);
                for (int i = 0; i < tournamentCount; i++) tournaments.add(input.readUTF());
                long revision = input.readLong();
                TrainerRecord state = new TrainerRecord(playerId, wins, losses, badges, tournaments, revision);
                if (!state.playerId().equals(expectedPlayerId)) throw new IllegalStateException("canonical trainer record identity mismatch");
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical trainer record data");
                return state;
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical trainer record", error);
        }
    }

    private void writeAtomically(Path target, TrainerRecord state) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(recordDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encode(state));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical trainer record store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical trainer record", error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private byte[] encode(TrainerRecord state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(state.playerId());
                output.writeLong(state.wins());
                output.writeLong(state.losses());
                output.writeInt(state.badgeIds().size());
                for (String badge : state.badgeIds()) output.writeUTF(badge);
                output.writeInt(state.tournamentRecordIds().size());
                for (String tournament : state.tournamentRecordIds()) output.writeUTF(tournament);
                output.writeLong(state.revision());
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical trainer record", error);
        }
    }

    private Path statePath(String playerId) {
        return recordDirectory.resolve(fileKey(playerId) + ".bin");
    }

    private static int boundedCount(int count, String field) {
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalStateException("invalid canonical trainer record " + field + " count");
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

    public record TrainerRecord(
            String playerId,
            long wins,
            long losses,
            Set<String> badgeIds,
            List<String> tournamentRecordIds,
            long revision
    ) {
        public TrainerRecord {
            playerId = requireId(playerId, "playerId");
            if (wins < 0 || losses < 0) throw new IllegalArgumentException("wins/losses must not be negative");
            badgeIds = Set.copyOf(badgeIds == null ? Set.of() : badgeIds);
            tournamentRecordIds = List.copyOf(tournamentRecordIds == null ? List.of() : tournamentRecordIds);
            if (badgeIds.size() > MAX_ENTRIES || tournamentRecordIds.size() > MAX_ENTRIES) {
                throw new IllegalArgumentException("trainer record collections exceed bounded size");
            }
            if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
