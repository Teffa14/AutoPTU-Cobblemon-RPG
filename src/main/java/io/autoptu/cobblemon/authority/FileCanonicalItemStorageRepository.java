package io.autoptu.cobblemon.authority;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/** Durable world-save item storage. Stored quantities are not part of the active bag repository. */
public final class FileCanonicalItemStorageRepository {
    private static final int MAGIC = 0x41505354; // APST
    private static final int SCHEMA_VERSION = 1;
    private static final ConcurrentMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();
    private final Path directory;

    public FileCanonicalItemStorageRepository(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("item-storage");
        try { Files.createDirectories(directory); }
        catch (IOException error) { throw new UncheckedIOException("failed to create canonical item storage", error); }
    }

    public CanonicalItemStorageState findOrCreate(String playerId) {
        String owner = requireId(playerId);
        Path path = path(owner);
        return withLock(path, () -> {
            if (Files.exists(path)) return read(path);
            CanonicalItemStorageState initial = new CanonicalItemStorageState(owner, Map.of(), Set.of(), 0L);
            write(path, initial);
            return initial;
        });
    }

    /** Applies a signed template quantity delta once per immutable transfer id. */
    public CanonicalItemStorageState applyDeltaOnce(String playerId, String transferId, String templateId, int delta) {
        String owner = requireId(playerId);
        String transfer = requireId(transferId);
        String template = requireId(templateId);
        if (delta == 0) throw new IllegalArgumentException("delta must not be zero");
        Path path = path(owner);
        return withLock(path, () -> {
            CanonicalItemStorageState current = Files.exists(path)
                    ? read(path)
                    : new CanonicalItemStorageState(owner, Map.of(), Set.of(), 0L);
            if (current.appliedTransferIds().contains(transfer)) return current;
            int before = current.quantity(template);
            long afterLong = (long) before + delta;
            if (afterLong < 0 || afterLong > Integer.MAX_VALUE) {
                throw new IllegalStateException("canonical item storage quantity would be invalid");
            }
            LinkedHashMap<String, Integer> quantities = new LinkedHashMap<>(current.quantities());
            int after = (int) afterLong;
            if (after == 0) quantities.remove(template); else quantities.put(template, after);
            LinkedHashSet<String> receipts = new LinkedHashSet<>(current.appliedTransferIds());
            receipts.add(transfer);
            CanonicalItemStorageState replacement = new CanonicalItemStorageState(
                    owner, quantities, receipts, current.revision() + 1);
            write(path, replacement);
            return replacement;
        });
    }

    private CanonicalItemStorageState read(Path path) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            if (in.readInt() != MAGIC) throw new IllegalStateException("invalid canonical item storage magic");
            if (in.readInt() != SCHEMA_VERSION) throw new IllegalStateException("unsupported canonical item storage schema");
            String playerId = in.readUTF();
            long revision = in.readLong();
            int count = in.readInt();
            LinkedHashMap<String, Integer> quantities = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) quantities.put(in.readUTF(), in.readInt());
            int receiptCount = in.readInt();
            LinkedHashSet<String> receipts = new LinkedHashSet<>();
            for (int i = 0; i < receiptCount; i++) receipts.add(in.readUTF());
            return new CanonicalItemStorageState(playerId, quantities, receipts, revision);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read canonical item storage", error);
        }
    }

    private void write(Path path, CanonicalItemStorageState state) {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(temp))) {
            out.writeInt(MAGIC);
            out.writeInt(SCHEMA_VERSION);
            out.writeUTF(state.playerId());
            out.writeLong(state.revision());
            out.writeInt(state.quantities().size());
            for (Map.Entry<String, Integer> entry : state.quantities().entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeInt(entry.getValue());
            }
            out.writeInt(state.appliedTransferIds().size());
            for (String id : state.appliedTransferIds()) out.writeUTF(id);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to write canonical item storage", error);
        }
        try {
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to replace canonical item storage", error);
        }
    }

    private Path path(String playerId) {
        return directory.resolve(Integer.toHexString(playerId.hashCode()) + "-" + playerId.replaceAll("[^A-Za-z0-9._-]", "_") + ".bin");
    }

    private static String requireId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("identity must not be blank");
        return value.strip();
    }

    private <T> T withLock(Path path, java.util.concurrent.Callable<T> action) {
        ReentrantLock lock = LOCKS.computeIfAbsent(path.toAbsolutePath().normalize(), ignored -> new ReentrantLock());
        lock.lock();
        try { return action.call(); }
        catch (RuntimeException error) { throw error; }
        catch (Exception error) { throw new IllegalStateException(error); }
        finally { lock.unlock(); }
    }
}
