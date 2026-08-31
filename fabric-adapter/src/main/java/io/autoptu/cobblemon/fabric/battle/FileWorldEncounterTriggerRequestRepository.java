package io.autoptu.cobblemon.fabric.battle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * World-save persistence for one pending visible-world encounter request per canonical player.
 *
 * The file contains only server-owned encounter identity/correlation and the observed world context.
 * It deliberately contains no PTU battle state or Cobblemon Pokemon payload.
 */
public final class FileWorldEncounterTriggerRequestRepository implements WorldEncounterTriggerRequestRepository {
    private static final int MAGIC = 0x41544553; // ATES
    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_STRING_BYTES = 16_384;

    private final Path directory;

    public FileWorldEncounterTriggerRequestRepository(Path canonicalStateRoot) {
        Objects.requireNonNull(canonicalStateRoot, "canonicalStateRoot");
        this.directory = canonicalStateRoot.resolve("active-encounter-sessions").normalize();
    }

    @Override
    public synchronized Optional<WorldEncounterTriggerRequestService.Request> findPending(String canonicalPlayerId) {
        String owner = requireId(canonicalPlayerId, "canonicalPlayerId");
        Path path = pathFor(owner);
        if (!Files.exists(path)) return Optional.empty();
        WorldEncounterTriggerRequestService.Request request = read(path);
        if (!owner.equals(request.canonicalPlayerId())) {
            throw new IllegalStateException("active encounter session owner mismatch");
        }
        return Optional.of(request);
    }

    @Override
    public synchronized boolean saveIfAbsent(WorldEncounterTriggerRequestService.Request request) {
        Objects.requireNonNull(request, "request");
        String owner = requireId(request.canonicalPlayerId(), "canonicalPlayerId");
        Path path = pathFor(owner);
        if (Files.exists(path)) {
            WorldEncounterTriggerRequestService.Request existing = read(path);
            if (!owner.equals(existing.canonicalPlayerId())) {
                throw new IllegalStateException("active encounter session owner mismatch");
            }
            return false;
        }
        writeAtomically(path, request);
        return true;
    }

    @Override
    public synchronized boolean clear(String canonicalPlayerId) {
        String owner = requireId(canonicalPlayerId, "canonicalPlayerId");
        Path path = pathFor(owner);
        if (!Files.exists(path)) return false;
        try {
            Files.delete(path);
            forceDirectory(directory);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("failed to clear active encounter session", e);
        }
    }

    private WorldEncounterTriggerRequestService.Request read(Path path) {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            if (input.readInt() != MAGIC) throw new IllegalStateException("invalid active encounter session magic");
            if (input.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported active encounter session schema");
            String canonicalEncounterId = readString(input);
            String canonicalPlayerId = readString(input);
            String externalWildActorId = input.readBoolean() ? readString(input) : null;
            String zoneId = readString(input);
            String contextId = readString(input);
            String dimensionId = readString(input);
            int blockX = input.readInt();
            int blockY = input.readInt();
            int blockZ = input.readInt();
            long serverTick = input.readLong();
            if (input.read() != -1) throw new IllegalStateException("active encounter session contains trailing bytes");
            return new WorldEncounterTriggerRequestService.Request(
                    canonicalEncounterId,
                    canonicalPlayerId,
                    externalWildActorId,
                    zoneId,
                    contextId,
                    dimensionId,
                    blockX,
                    blockY,
                    blockZ,
                    serverTick
            );
        } catch (EOFException e) {
            throw new IllegalStateException("truncated active encounter session", e);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read active encounter session", e);
        }
    }

    private void writeAtomically(Path path, WorldEncounterTriggerRequestService.Request request) {
        try {
            Files.createDirectories(directory);
            Path temporary = Files.createTempFile(directory, "active-encounter-", ".tmp");
            boolean moved = false;
            try {
                try (FileChannel channel = FileChannel.open(
                        temporary,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING
                ); DataOutputStream output = new DataOutputStream(new BufferedOutputStream(java.nio.channels.Channels.newOutputStream(channel)))) {
                    output.writeInt(MAGIC);
                    output.writeInt(SCHEMA_VERSION);
                    writeString(output, request.canonicalEncounterId());
                    writeString(output, request.canonicalPlayerId());
                    output.writeBoolean(request.externalWildActorId() != null);
                    if (request.externalWildActorId() != null) writeString(output, request.externalWildActorId());
                    writeString(output, request.zoneId());
                    writeString(output, request.contextId());
                    writeString(output, request.dimensionId());
                    output.writeInt(request.blockX());
                    output.writeInt(request.blockY());
                    output.writeInt(request.blockZ());
                    output.writeLong(request.serverTick());
                    output.flush();
                    channel.force(true);
                }
                try {
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, path);
                }
                moved = true;
                forceDirectory(directory);
            } finally {
                if (!moved) Files.deleteIfExists(temporary);
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to persist active encounter session", e);
        }
    }

    private Path pathFor(String canonicalPlayerId) {
        return directory.resolve(sha256(canonicalPlayerId) + ".bin");
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = requireId(value, "persistedString").getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) throw new IllegalArgumentException("persisted string is too long");
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length <= 0 || length > MAX_STRING_BYTES) throw new IllegalStateException("invalid active encounter session string length");
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) throw new EOFException("truncated active encounter session string");
        return requireId(new String(bytes, StandardCharsets.UTF_8), "persistedString");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static void forceDirectory(Path directory) {
        try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException ignored) {
            // The file itself is already forced; some platforms do not permit directory fsync.
        }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
