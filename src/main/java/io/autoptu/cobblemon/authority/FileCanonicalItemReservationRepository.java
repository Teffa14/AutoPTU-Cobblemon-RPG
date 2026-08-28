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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Durable canonical item-instance and reservation store.
 *
 * <p>Each item file contains server-owned item state plus at most one reservation. Version 2 adds a
 * consumed-but-retained reservation state used by recoverable multi-item transactions such as
 * crafting. In that state quantity has already been deducted, but the reservation continues to lock
 * the item until the transaction journal reaches its durable commit point.</p>
 */
public final class FileCanonicalItemReservationRepository implements CanonicalAssetRepository {
    static final int SCHEMA_VERSION = 2;
    private static final int LEGACY_SCHEMA_VERSION = 1;
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
        itemsDirectory = rootDirectory.toAbsolutePath().normalize().resolve("items");
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

    /** Returns all canonical item stacks owned by one player, including active transaction locks. */
    public List<InventoryEntry> findOwnedInventory(String playerId) {
        String owner = requireId("playerId", playerId);
        List<InventoryEntry> matches = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(itemsDirectory, "*.bin")) {
            for (Path path : files) {
                StoredItem stored = readStored(path);
                CanonicalItemInstance item = stored.item();
                if (item.ownerPlayerId().equals(owner) && item.quantity() > 0) {
                    matches.add(new InventoryEntry(item, stored.reservation(), stored.reservationConsumed()));
                }
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to scan canonical item inventory", error);
        }
        matches.sort(Comparator.comparing(entry -> entry.item().itemInstanceId()));
        return List.copyOf(matches);
    }

    /** Returns unreserved positive-quantity canonical stacks for one owner/template in stable order. */
    public List<CanonicalItemInstance> findReservableItems(String playerId, String itemTemplateId) {
        String owner = requireId("playerId", playerId);
        String template = requireId("itemTemplateId", itemTemplateId);
        List<CanonicalItemInstance> matches = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(itemsDirectory, "*.bin")) {
            for (Path path : files) {
                StoredItem stored = readStored(path);
                CanonicalItemInstance item = stored.item();
                if (stored.reservation() == null
                        && item.quantity() > 0
                        && item.ownerPlayerId().equals(owner)
                        && item.templateId().equals(template)) {
                    matches.add(item);
                }
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to scan canonical item inventory", error);
        }
        matches.sort(Comparator.comparing(CanonicalItemInstance::itemInstanceId));
        return List.copyOf(matches);
    }

    @Override
    public Optional<ItemReservation> findReservation(String reservationId) {
        String normalizedReservationId = requireId("reservationId", reservationId);
        StoredItem located = findStoredReservation(normalizedReservationId).orElse(null);
        return located == null ? Optional.empty() : Optional.of(located.reservation());
    }

    /** True when the reservation exists and its quantity has already been durably deducted. */
    public boolean isReservationConsumed(String reservationId, String playerId) {
        String id = requireId("reservationId", reservationId);
        String owner = requireId("playerId", playerId);
        StoredItem located = findStoredReservation(id).orElse(null);
        return located != null
                && located.reservation().playerId().equals(owner)
                && located.reservationConsumed();
    }

    /** Creates a new server-owned item instance only when its canonical ID does not already exist. */
    public boolean createItemIfAbsent(CanonicalItemInstance initialItem) {
        if (initialItem == null) throw new IllegalArgumentException("initialItem is required");
        String itemId = requireId("itemInstanceId", initialItem.itemInstanceId());
        return withItemLock(itemId, () -> {
            Path path = statePath(itemId);
            if (Files.exists(path)) return false;
            writeAtomically(path, new StoredItem(initialItem, null, false));
            return true;
        });
    }

    /** Replaces unreserved item truth through revision CAS. */
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
            if (current.reservation() != null || current.item().revision() != expectedRevision) return false;
            writeAtomically(path, new StoredItem(replacement, null, false));
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
            writeAtomically(path, new StoredItem(item, reservation, false));
            return true;
        });
    }

    /**
     * Deducts a reservation exactly once while retaining its lock for a larger recoverable
     * transaction. Repeated calls return true without consuming again.
     */
    public boolean consumeReservationRetainingLock(String reservationId, String playerId) {
        String id = requireId("reservationId", reservationId);
        String owner = requireId("playerId", playerId);
        StoredItem located = findStoredReservation(id).orElse(null);
        if (located == null || !located.reservation().playerId().equals(owner)) return false;
        String itemId = located.reservation().itemInstanceId();
        return withItemLock(itemId, () -> {
            Path path = statePath(itemId);
            if (!Files.exists(path)) return false;
            StoredItem current = readStored(path);
            ItemReservation active = current.reservation();
            if (active == null || !active.reservationId().equals(id) || !active.playerId().equals(owner)) return false;
            if (current.reservationConsumed()) return true;
            CanonicalItemInstance item = current.item();
            if (item.revision() != active.itemRevision() || item.quantity() < active.quantity()) return false;
            CanonicalItemInstance consumed = new CanonicalItemInstance(
                    item.itemInstanceId(), item.ownerPlayerId(), item.templateId(),
                    item.quantity() - active.quantity(), item.revision() + 1);
            ItemReservation retained = new ItemReservation(
                    active.reservationId(), active.playerId(), active.itemInstanceId(), active.itemTemplateId(),
                    active.quantity(), consumed.revision());
            writeAtomically(path, new StoredItem(consumed, retained, true));
            return true;
        });
    }

    /** Removes a consumed transaction lock without changing the already-deducted quantity. */
    public boolean releaseConsumedReservationLock(String reservationId, String playerId) {
        String id = requireId("reservationId", reservationId);
        String owner = requireId("playerId", playerId);
        StoredItem located = findStoredReservation(id).orElse(null);
        if (located == null) return true;
        if (!located.reservation().playerId().equals(owner)) return false;
        String itemId = located.reservation().itemInstanceId();
        return withItemLock(itemId, () -> {
            Path path = statePath(itemId);
            if (!Files.exists(path)) return false;
            StoredItem current = readStored(path);
            if (current.reservation() == null) return true;
            if (!current.reservation().reservationId().equals(id)
                    || !current.reservation().playerId().equals(owner)
                    || !current.reservationConsumed()) return false;
            writeAtomically(path, new StoredItem(current.item(), null, false));
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
        String id = requireId("reservationId", reservationId);
        String owner = requireId("playerId", playerId);
        StoredItem located = findStoredReservation(id).orElse(null);
        if (located == null || !located.reservation().playerId().equals(owner)) return false;
        String itemId = located.reservation().itemInstanceId();
        return withItemLock(itemId, () -> {
            Path path = statePath(itemId);
            if (!Files.exists(path)) return false;
            StoredItem current = readStored(path);
            ItemReservation active = current.reservation();
            if (active == null || !active.reservationId().equals(id) || !active.playerId().equals(owner)) return false;
            if (current.reservationConsumed()) {
                if (!commit) return false;
                writeAtomically(path, new StoredItem(current.item(), null, false));
                return true;
            }
            CanonicalItemInstance item = current.item();
            if (item.revision() != active.itemRevision()) return false;
            if (!commit) {
                writeAtomically(path, new StoredItem(item, null, false));
                return true;
            }
            if (item.quantity() < active.quantity()) return false;
            CanonicalItemInstance committed = new CanonicalItemInstance(
                    item.itemInstanceId(), item.ownerPlayerId(), item.templateId(),
                    item.quantity() - active.quantity(), item.revision() + 1);
            writeAtomically(path, new StoredItem(committed, null, false));
            return true;
        });
    }

    private Optional<StoredItem> findStoredReservation(String reservationId) {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(itemsDirectory, "*.bin")) {
            for (Path path : files) {
                StoredItem stored = readStored(path);
                if (stored.reservation() != null && stored.reservation().reservationId().equals(reservationId)) {
                    return Optional.of(stored);
                }
            }
            return Optional.empty();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to scan canonical item reservations", error);
        }
    }

    private StoredItem readStored(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid canonical item file magic");
                int schema = input.readInt();
                if (schema != LEGACY_SCHEMA_VERSION && schema != SCHEMA_VERSION) {
                    throw new IllegalStateException("unsupported canonical item schema version: " + schema);
                }
                CanonicalItemInstance item = new CanonicalItemInstance(
                        input.readUTF(), input.readUTF(), input.readUTF(), input.readInt(), input.readLong());
                ItemReservation reservation = null;
                if (input.readBoolean()) {
                    reservation = new ItemReservation(
                            input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(), input.readInt(), input.readLong());
                }
                boolean consumed = schema >= 2 && input.readBoolean();
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing canonical item data");
                return new StoredItem(item, reservation, consumed);
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
                output.writeBoolean(stored.reservationConsumed());
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
        if (stored.reservation() != null && !stored.reservation().itemInstanceId().equals(requestedItemId)) {
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
        return value.trim();
    }

    public record InventoryEntry(
            CanonicalItemInstance item,
            ItemReservation reservation,
            boolean reservationConsumed
    ) {
        public InventoryEntry {
            if (item == null) throw new IllegalArgumentException("item is required");
            if (reservation == null && reservationConsumed) {
                throw new IllegalArgumentException("consumed marker requires reservation");
            }
            if (reservation != null) {
                if (!reservation.playerId().equals(item.ownerPlayerId())
                        || !reservation.itemInstanceId().equals(item.itemInstanceId())
                        || !reservation.itemTemplateId().equals(item.templateId())) {
                    throw new IllegalArgumentException("reservation must belong to item");
                }
            }
        }

        public int reservedQuantity() {
            return reservation != null && !reservationConsumed ? reservation.quantity() : 0;
        }

        public int availableQuantity() {
            return Math.max(0, item.quantity() - reservedQuantity());
        }

        public boolean transactionLocked() {
            return reservation != null;
        }
    }

    private record StoredItem(CanonicalItemInstance item, ItemReservation reservation, boolean reservationConsumed) {
        private StoredItem {
            if (item == null) throw new IllegalArgumentException("item is required");
            if (reservation == null && reservationConsumed) {
                throw new IllegalArgumentException("consumed marker requires reservation");
            }
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
                if (reservationConsumed && reservation.itemRevision() != item.revision()) {
                    throw new IllegalArgumentException("consumed reservation revision must match item");
                }
            }
        }
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
