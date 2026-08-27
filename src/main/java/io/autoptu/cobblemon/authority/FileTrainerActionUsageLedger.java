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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Durable world-scoped PTU Trainer action usage ledger.
 *
 * <p>The highest observed Overworld day is monotonic and persists across process restart. Moving
 * Minecraft time backwards, reconnecting, or restarting the server therefore cannot restore a
 * Daily use. Pending reservations count against the active frequency cap until explicitly committed
 * or released, preventing crash/retry overbooking.</p>
 */
public final class FileTrainerActionUsageLedger implements TrainerActionUsageLedger {
    static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x4154554C; // ATUL
    private static final ConcurrentMap<Path, ReentrantLock> PROCESS_LOCKS = new ConcurrentHashMap<>();

    private final Path directory;
    private final Path statePath;
    private final Path lockPath;
    private volatile long cachedHighestObservedDay;

    public FileTrainerActionUsageLedger(Path rootDirectory) {
        if (rootDirectory == null) throw new IllegalArgumentException("rootDirectory is required");
        directory = rootDirectory.toAbsolutePath().normalize().resolve("trainer-action-usage");
        statePath = directory.resolve("ledger.bin");
        lockPath = directory.resolve("ledger.lock");
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new UncheckedIOException("failed to create Trainer action usage store", error);
        }
        cachedHighestObservedDay = withLedgerLock(() -> readState().highestObservedDay());
    }

    @Override
    public long observeOverworldDay(long observedOverworldDay) {
        requireDay(observedOverworldDay);
        if (observedOverworldDay <= cachedHighestObservedDay) return cachedHighestObservedDay;
        return withLedgerLock(() -> {
            LedgerState current = readState();
            long effective = Math.max(current.highestObservedDay(), observedOverworldDay);
            if (effective != current.highestObservedDay()) {
                writeState(new LedgerState(effective, current.reservations()));
            }
            cachedHighestObservedDay = effective;
            return effective;
        });
    }

    @Override
    public TrainerActionUsageDecision reserve(TrainerActionUsageAttempt attempt) {
        if (attempt == null) throw new IllegalArgumentException("attempt is required");
        TrainerActionFrequency frequency = attempt.rule().frequency();
        if (frequency == TrainerActionFrequency.AT_WILL || frequency.battleCoreOwned()) {
            throw new IllegalArgumentException("ledger accepts only DAILY, SCENE and ENCOUNTER reservations");
        }
        if (frequency.requiresCanonicalContext()
                && (attempt.canonicalContextId() == null || attempt.canonicalContextId().isBlank())) {
            return TrainerActionUsageDecision.withoutReservation(
                    TrainerActionUsageDecision.Status.CONTEXT_REQUIRED,
                    0,
                    attempt.rule().maxUses(),
                    ""
            );
        }

        return withLedgerLock(() -> {
            LedgerState current = readState();
            long highestDay = Math.max(current.highestObservedDay(), attempt.observedOverworldDay());
            cachedHighestObservedDay = Math.max(cachedHighestObservedDay, highestDay);

            Optional<TrainerActionUsageReservation> previous = findByOperationId(
                    current.reservations(), attempt.operationId());
            if (previous.isPresent()) {
                TrainerActionUsageReservation reservation = previous.orElseThrow();
                if (!sameOperation(reservation, attempt)) {
                    if (highestDay != current.highestObservedDay()) {
                        writeState(new LedgerState(highestDay, current.reservations()));
                    }
                    return TrainerActionUsageDecision.withoutReservation(
                            TrainerActionUsageDecision.Status.OPERATION_CONFLICT,
                            0,
                            attempt.rule().maxUses(),
                            reservation.windowId()
                    );
                }
                int used = countUsed(current.reservations(), reservation);
                TrainerActionUsageDecision.Status status = reservation.status() == TrainerActionUsageReservation.Status.COMMITTED
                        ? TrainerActionUsageDecision.Status.ALREADY_COMMITTED
                        : TrainerActionUsageDecision.Status.ALREADY_RESERVED;
                if (highestDay != current.highestObservedDay()) {
                    writeState(new LedgerState(highestDay, current.reservations()));
                }
                return TrainerActionUsageDecision.withReservation(
                        status,
                        reservation,
                        used,
                        attempt.rule().maxUses()
                );
            }

            String windowId = windowId(frequency, attempt.canonicalContextId(), highestDay);
            int used = countUsed(
                    current.reservations(),
                    attempt.playerId(),
                    attempt.rule().actionId(),
                    frequency,
                    windowId
            );
            if (used >= attempt.rule().maxUses()) {
                if (highestDay != current.highestObservedDay()) {
                    writeState(new LedgerState(highestDay, current.reservations()));
                }
                return TrainerActionUsageDecision.withoutReservation(
                        TrainerActionUsageDecision.Status.LIMIT_REACHED,
                        used,
                        attempt.rule().maxUses(),
                        windowId
                );
            }

            TrainerActionUsageReservation reservation = new TrainerActionUsageReservation(
                    java.util.UUID.randomUUID().toString(),
                    attempt.operationId(),
                    attempt.playerId(),
                    attempt.rule().actionId(),
                    frequency,
                    attempt.rule().maxUses(),
                    windowId,
                    TrainerActionUsageReservation.Status.RESERVED,
                    attempt.createdAtEpochMs()
            );
            List<TrainerActionUsageReservation> updated = new ArrayList<>(current.reservations());
            updated.add(reservation);
            writeState(new LedgerState(highestDay, List.copyOf(updated)));
            return TrainerActionUsageDecision.withReservation(
                    TrainerActionUsageDecision.Status.RESERVED,
                    reservation,
                    used + 1,
                    attempt.rule().maxUses()
            );
        });
    }

    @Override
    public boolean commit(String reservationId, String playerId) {
        return finish(reservationId, playerId, true);
    }

    @Override
    public boolean release(String reservationId, String playerId) {
        return finish(reservationId, playerId, false);
    }

    @Override
    public Optional<TrainerActionUsageReservation> findByOperationId(String operationId) {
        String normalizedOperationId = requireId("operationId", operationId);
        return withLedgerLock(() -> findByOperationId(readState().reservations(), normalizedOperationId));
    }

    @Override
    public long highestObservedOverworldDay() {
        return Math.max(cachedHighestObservedDay, withLedgerLock(() -> {
            long persisted = readState().highestObservedDay();
            cachedHighestObservedDay = Math.max(cachedHighestObservedDay, persisted);
            return persisted;
        }));
    }

    private boolean finish(String reservationId, String playerId, boolean commit) {
        String rid = requireId("reservationId", reservationId);
        String pid = requireId("playerId", playerId);
        return withLedgerLock(() -> {
            LedgerState current = readState();
            int index = -1;
            TrainerActionUsageReservation active = null;
            for (int i = 0; i < current.reservations().size(); i++) {
                TrainerActionUsageReservation candidate = current.reservations().get(i);
                if (candidate.reservationId().equals(rid)) {
                    index = i;
                    active = candidate;
                    break;
                }
            }
            if (active == null || !active.playerId().equals(pid)) return false;
            if (active.status() == TrainerActionUsageReservation.Status.COMMITTED) return commit;

            List<TrainerActionUsageReservation> updated = new ArrayList<>(current.reservations());
            if (commit) {
                updated.set(index, active.committed());
            } else {
                updated.remove(index);
            }
            writeState(new LedgerState(current.highestObservedDay(), List.copyOf(updated)));
            return true;
        });
    }

    private LedgerState readState() {
        if (!Files.exists(statePath)) return new LedgerState(-1L, List.of());
        try {
            byte[] bytes = Files.readAllBytes(statePath);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != MAGIC) throw new IllegalStateException("invalid Trainer action usage file magic");
                int schema = input.readInt();
                if (schema != SCHEMA_VERSION) {
                    throw new IllegalStateException("unsupported Trainer action usage schema version: " + schema);
                }
                long highestDay = input.readLong();
                int size = input.readInt();
                if (size < 0) throw new IllegalStateException("invalid Trainer action usage reservation count");
                List<TrainerActionUsageReservation> reservations = new ArrayList<>(size);
                for (int i = 0; i < size; i++) reservations.add(readReservation(input));
                if (input.available() != 0) throw new IllegalStateException("unexpected trailing Trainer action usage data");
                return new LedgerState(highestDay, List.copyOf(reservations));
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read Trainer action usage state", error);
        }
    }

    private TrainerActionUsageReservation readReservation(DataInputStream input) throws IOException {
        return new TrainerActionUsageReservation(
                input.readUTF(),
                input.readUTF(),
                input.readUTF(),
                input.readUTF(),
                TrainerActionFrequency.valueOf(input.readUTF()),
                input.readInt(),
                input.readUTF(),
                TrainerActionUsageReservation.Status.valueOf(input.readUTF()),
                input.readLong()
        );
    }

    private void writeState(LedgerState state) {
        byte[] encoded = encode(state);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(directory, "ledger.", ".tmp");
            try (FileChannel channel = FileChannel.open(
                    temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(encoded);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try {
                Files.move(temporary, statePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                throw new IllegalStateException("Trainer action usage store requires atomic file replacement", error);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("failed to persist Trainer action usage state", error);
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

    private byte[] encode(LedgerState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA_VERSION);
                output.writeLong(state.highestObservedDay());
                output.writeInt(state.reservations().size());
                for (TrainerActionUsageReservation reservation : state.reservations()) {
                    output.writeUTF(reservation.reservationId());
                    output.writeUTF(reservation.operationId());
                    output.writeUTF(reservation.playerId());
                    output.writeUTF(reservation.actionId());
                    output.writeUTF(reservation.frequency().name());
                    output.writeInt(reservation.maxUses());
                    output.writeUTF(reservation.windowId());
                    output.writeUTF(reservation.status().name());
                    output.writeLong(reservation.createdAtEpochMs());
                }
                output.flush();
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("failed to encode Trainer action usage state", error);
        }
    }

    private <T> T withLedgerLock(IoSupplier<T> operation) {
        ReentrantLock processLock = PROCESS_LOCKS.computeIfAbsent(lockPath, ignored -> new ReentrantLock());
        processLock.lock();
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            return operation.get();
        } catch (IOException error) {
            throw new UncheckedIOException("Trainer action usage store operation failed", error);
        } finally {
            processLock.unlock();
        }
    }

    private static Optional<TrainerActionUsageReservation> findByOperationId(
            List<TrainerActionUsageReservation> reservations,
            String operationId
    ) {
        return reservations.stream().filter(value -> value.operationId().equals(operationId)).findFirst();
    }

    private static boolean sameOperation(TrainerActionUsageReservation previous, TrainerActionUsageAttempt attempt) {
        if (!previous.playerId().equals(attempt.playerId())
                || !previous.actionId().equals(attempt.rule().actionId())
                || previous.frequency() != attempt.rule().frequency()
                || previous.maxUses() != attempt.rule().maxUses()) {
            return false;
        }
        return switch (previous.frequency()) {
            case SCENE -> previous.windowId().equals("scene:" + attempt.canonicalContextId());
            case ENCOUNTER -> previous.windowId().equals("encounter:" + attempt.canonicalContextId());
            case DAILY -> true;
            default -> false;
        };
    }

    private static int countUsed(List<TrainerActionUsageReservation> reservations, TrainerActionUsageReservation key) {
        return countUsed(
                reservations,
                key.playerId(),
                key.actionId(),
                key.frequency(),
                key.windowId()
        );
    }

    private static int countUsed(
            List<TrainerActionUsageReservation> reservations,
            String playerId,
            String actionId,
            TrainerActionFrequency frequency,
            String windowId
    ) {
        int used = 0;
        for (TrainerActionUsageReservation reservation : reservations) {
            if (reservation.playerId().equals(playerId)
                    && reservation.actionId().equals(actionId)
                    && reservation.frequency() == frequency
                    && reservation.windowId().equals(windowId)) {
                used++;
            }
        }
        return used;
    }

    private static String windowId(TrainerActionFrequency frequency, String contextId, long highestDay) {
        return switch (frequency) {
            case DAILY -> "day:" + highestDay;
            case SCENE -> "scene:" + contextId;
            case ENCOUNTER -> "encounter:" + contextId;
            default -> throw new IllegalArgumentException("unsupported limited overworld frequency: " + frequency);
        };
    }

    private static long requireDay(long value) {
        if (value < 0) throw new IllegalArgumentException("observedOverworldDay must not be negative");
        return value;
    }

    private static String requireId(String field, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private record LedgerState(long highestObservedDay, List<TrainerActionUsageReservation> reservations) {
        private LedgerState {
            if (highestObservedDay < -1) throw new IllegalArgumentException("highestObservedDay must be >= -1");
            if (reservations == null) throw new IllegalArgumentException("reservations are required");
        }
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
