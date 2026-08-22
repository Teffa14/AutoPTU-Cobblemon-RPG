package io.autoptu.cobblemon.authority;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Durable canonical item-instance and reservation store.
 *
 * <p>Each item file contains the server-owned item state plus at most one active reservation.
 * Reservation creation, commit and release all replace that one file atomically while holding an
 * in-process lock and an OS file lock. A reservation therefore survives process restart without
 * allowing a second reservation to overbook the same item revision.</p>
 *
 * <p>Pokemon lookup remains an injected read-only dependency for now. This slice deliberately does
 * not claim durable Pokemon persistence; callers may compose this repository with the current
 * canonical Pokemon source until that separate aggregate store is implemented.</p>
 */
public final class FileCanonicalItemReservationRepository implements CanonicalAssetRepository {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x41504952; // APIR
    private static final ConcurrentMap<Path, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();

    private final Path itemsDirectory;
    private final Function<String, Optional<CanonicalPokemonState>> pokemonLookup;

    public FileCanonicalItemReservationRepository(
            Path rootDirectory,
            Function<String, Optional<CanonicalPokemonState>> pokemonLookup
    ) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        if (pokemonLookup == null) throw new IllegalArgumentException("pokemonLookup is required");
        this.itemsDirectory = rootDirectory.toAbsolutePath().normalize().resolve("items");
        this.pokemonLookup = pokemonLookup;
        try {
            Files.createDirectories(itemsDirectory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create canonical item store", error);
        }
    }

    public FileCanonicalItemReservationRepository(Path rootDirectory) {
        this(rootDirectory, ignored -> Optional.empty());
    }

    @Override
    public Optional<CanonicalPokemonState> findPokemon(String pokemonId) {
        if (pokemonId == null || pokemonId.isBlank()) return Optional.empty();
        return pokemonLookup.apply(pokemonId);
    }

    @Override
    public Optional<CanonicalItemInstance> findItem(String itemInstanceId) {
        String itemId = requireId("itemInstanceId", itemInstanceId);
        Path path = statePath(itemId);
        if (!Files.exists(path)) return Optional.empty();
        StoredItem stored = readStored(path);
        requireStoredIdentity(itemId, stored);
        return Optional.of(stored.item());
    }

    @Override
    public Optional<ItemReservation> findReservation(String reservationId) {
        String normalizedReservationId = requireId("reservationId", reservationId);
        try (DirectoryStream<Path> files = Files.newDirectoryStream(itemsDirectory, "*.bin")) {
            for (Path path : files) {
                StoredItem stored = readStored(path);
                ItemReservation reservation = stored.reservation();
                if (reservation != null && reservation.reservationId().equals(normalizedReservationId)) {
                    return Optional.of(reservation);
                }
            }
            return Optional.empty();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to scan canonical item reservations", error);
        }
    }

    /** Creates a new server-owned item instance only when its canonical ID does not already exist. */
    public boolean createItemIfAbsent(CanonicalItemInstance initialItem) {
        if (initialItem == null) throw new IllegalArgumentException("initialItem is required");
        String itemId = requireId("itemInstanceId", initialItem.itemInstanceId());
        return withItemLock(itemId, () -> {
            Path path = statePath(itemId);
            if (Files.exists(path)) return false;
            writeAtomically(path, new StoredItem(initialItem, null));
            return true;
        });
    }

    /**
     * Replaces unreserved item truth through revision CAS. Reserved item state is frozen until the
     * reservation is committed or released.
     */
    public boolean replaceItemIfRevision(
            String itemInstanceId,
            long expectedRevision,
            CanonicalItemInstance replacement
    ) {
        String itemId = requireId("itemInstanceId", itemInstanceId);
        if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE) {
            throw new IllegalArgumentException("expectedRevision must allow exactly one revision advance");
        }
        if (replacement == null) throw new IllegalArgumentException("replacement is required");
        if (!replacement.itemInstanceId().equals(itemId)) {
            throw new IllegalArgumentException("replacement item identity must match itemInstanceId");
        }
        if (replacement.revision() != expectedRevision + 1) {
            throw new IllegalArgumentException("replacement revision must advance expectedRevision exactly once");
        }

        return withItemLock(itemId, () -> {
            Path path = statePath(itemId);
            if (!Files.exists(path)) return false;
            StoredItem current = readStored(path);
            requireStoredIdentity(itemId, current);
            if (current.reservation() != null) return false;
            if (current.item().revision() != expectedRevision) return false;
            writeAtomically(path, new StoredItem(replacement, null));
            return true;
        });
    }

    @Override
    public boolean tryReserveItem(ItemReservation reservation) {
        if (reservation == null) throw new IllegalArgumentException("reservation is required");
        String itemId = requireId("itemInstanceId", reservation.itemInstanceId());
        return withItemLock(itemId, () -> {
            Path path = statePath(itemId);
            if (!Files.exists(path)) return false;
            StoredItem current = readStored(path);
            requireStoredIdentity(itemId, current);
            CanonicalItemInstance item = current.item();
            if (current.reservation() != null) return false;
            if (!item.ownerPlayerId().equals(reservation.playerId())) return false;
            if (!item.templateId().equals(reservation.itemTemplateId())) return false;
            if (item.revision() != reservation.itemRevision()) return false;
            if (item.quantity() < reservation.quantity()) return false;
            writeAtomically(path, new StoredItem(item, reservation));
            return true;
        });
    }

    @Override
    public boolean commitItemReservation(String reservationId, String playerId) {
        return finishReservation(reservationId, playerId, true);
    }

    @Override
    public boolean releaseItemReservation(String reservationId, String playerId) {
        return finishReservation(reservationId, playerId, false);
    }

    private boolean finishReservation(String reservationId, String playerId, boolean commit) {
        String normalizedReservationId = requireId("reservationId", reservationId);
        String normalizedPlayerId = requireId("playerId", playerId);
        ItemReservation located = findReservation(normalizedReservationId).orElse(null);
        if (located == null || !located.playerId().equals(normalizedPlayerId)) return false;
        String itemId = located.itemInstanceId();

        return withItemLock(itemId, () -> {
            Path path = statePath(itemId);
            if (!Files.exists(path)) return false;
            StoredItem current = readStored(path);
            requireStoredIdentity(itemId, current);
            ItemReservation active = current.reservation();
            if (active == null
                    || !active.reservationId().equals(normalizedReservationId)
                    || !active.playerId().equals(normalizedPlayerId)) {
                return false;
            }
            CanonicalItemInstance item = current.item();
            if (item.revision() != active.itemRevision()) return false;

            if (!commit) {
                writeAtomically(path, new StoredItem(item, null));
                return true;
            }
            if (item.quantity() < active.quantity()) return false;
            CanonicalItemInstance committed = new CanonicalItemInstance(
                    item.itemInstanceId(),
                    item.ownerPlayerId(),
                    item.templateId(),
                    item.quantity() - active.quantity(),
                    item.revision() + 1
            );
            writeAtomically(path, new StoredItem(committed, null));
            return true;
        });
    }

    private StoredItem readStored(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical item file magic");
                int schema = input.readInt();
                if (schema != SCHEMA_VERSION) {
                    throw new IllegalStateException("unsupported canonical item schema version: " + schema);
                }
                CanonicalItemInstance item = new CanonicalItemInstance(
                        input.readUTF(), input.readUTF(), input.readUTF(), input.readInt(), input.readLong());
                ItemReservation reservation = null;
                if (input.readBoolean()) {
                    reservation = new ItemReservation(
                            input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(), input.readInt(), input.readLong());
                }
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical item data");
                return new StoredItem(item, reservation);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical item state", error);
        }
    }

    private void writeAtomically(Path target, StoredItem stored) {
        byte[] encoded = encode(stored);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(itemsDirectory, target.getFileName().toString() + ".", ".tmp");
            try (FileChannel channel = FileChannel.open(
                    temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encoded);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("canonical item store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist canonical item state", error);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best-effort cleanup only. The target is authoritative.
                }
            }
        }
    }

    private byte[] encode(StoredItem stored) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                CanonicalItemInstance item = stored.item();
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeUTF(item.itemInstanceId());
                output.writeUTF(item.ownerPlayerId());
                output.writeUTF(item.templateId());
                output.writeInt(item.quantity());
                output.writeLong(item.revision());
                ItemReservation reservation = stored.reservation();
                output.writeBoolean(reservation != null);
                if (reservation != null) {
                    output.writeUTF(reservation.reservationId());
                    output.writeUTF(reservation.playerId());
                    output.writeUTF(reservation.itemInstanceId());
                    output.writeUTF(reservation.itemTemplateId());
                    output.writeInt(reservation.quantity());
                    output.writeLong(reservation.itemRevision());
                }
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode canonical item state", error);
        }
    }

    private <T> T withItemLock(String itemId, IoSupplier<T> operation) {
        Path lockPath = lockPath(itemId);
        ReentrantLock processLock = PROCESS_LOCKS.computeIfAbsent(lockPath, ignored -> new ReentrantLock());
        processLock.lock();
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            return operation.get();
        } catch (IOException error) {
            throw new UncheckedIOException("canonical item store operation failed", error);
        } finally {
            processLock.unlock();
        }
    }

    private void requireStoredIdentity(String requestedItemId, StoredItem stored) {
        if (!stored.item().itemInstanceId().equals(requestedItemId)) {
            throw new IllegalStateException("canonical item file identity mismatch");
        }
        if (stored.reservation() != null
                && !stored.reservation().itemInstanceId().equals(requestedItemId)) {
            throw new IllegalStateException("canonical reservation item identity mismatch");
        }
    }

    private Path statePath(String itemId) {
        return itemsDirectory.resolve(fileKey(itemId) + ".bin");
    }

    private Path lockPath(String itemId) {
        return itemsDirectory.resolve(fileKey(itemId) + ".lock");
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

    private static String requireId(String field, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }

    private record StoredItem(CanonicalItemInstance item, ItemReservation reservation) {
        private StoredItem {
            if (item == null) throw new IllegalArgumentException("item is required");
            if (reservation != null) {
                if (!reservation.itemInstanceId().equals(item.itemInstanceId())) {
                    throw new IllegalArgumentException("reservation item identity must match item");
                }
                if (!reservation.playerId().equals(item.ownerPlayerId())) {
                    throw new IllegalArgumentException("reservation player identity must match item owner");
                }
                if (!reservation.itemTemplateId().equals(item.templateId())) {
                    throw new IllegalArgumentException("reservation template identity must match item");
                }
            }
        }
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
