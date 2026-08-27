package io.autoptu.cobblemon.authority;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Properties;

/**
 * Server-owned durable usage ledger for PTU actions whose canonical policy allows a bounded number
 * of uses per RPG day.
 *
 * <p>This class does not decide PTU frequency, action cost, legality, or effects. Its callers must
 * resolve those values from trusted server-owned PTU data. Clients must never supply
 * {@code maxUsesPerDay} as trusted input.</p>
 */
public final class FileTrainerPtuDailyActionLedger {
    private static final String OPERATION_PREFIX = "__autoptu_daily_operation__";
    private static final String OPERATION_SCHEMA = "op1";

    public record UsageView(long rpgDayId, int used, int limit) {
        public UsageView {
            if (rpgDayId < 0) throw new IllegalArgumentException("rpgDayId must be >= 0");
            if (used < 0) throw new IllegalArgumentException("used must be >= 0");
            if (limit < 1) throw new IllegalArgumentException("limit must be >= 1");
            if (used > limit) throw new IllegalArgumentException("used must not exceed limit");
        }

        public int remaining() {
            return limit - used;
        }

        public boolean available() {
            return used < limit;
        }
    }

    public record ConsumeResult(boolean consumed, UsageView usage) {
        public ConsumeResult {
            Objects.requireNonNull(usage, "usage");
        }
    }

    public enum ReservationStatus {
        RESERVED,
        ALREADY_RESERVED,
        ALREADY_COMMITTED,
        LIMIT_REACHED,
        OPERATION_CONFLICT
    }

    public record ReservationResult(ReservationStatus status, String operationId, UsageView usage) {
        public ReservationResult {
            Objects.requireNonNull(status, "status");
            operationId = requireId("operationId", operationId);
            Objects.requireNonNull(usage, "usage");
        }

        public boolean allowed() {
            return status == ReservationStatus.RESERVED
                    || status == ReservationStatus.ALREADY_RESERVED
                    || status == ReservationStatus.ALREADY_COMMITTED;
        }
    }

    private enum OperationState {
        RESERVED,
        COMMITTED
    }

    private record OperationRecord(
            String operationId,
            String canonicalActionId,
            long rpgDayId,
            OperationState state
    ) {
        private OperationRecord {
            operationId = requireId("operationId", operationId);
            canonicalActionId = requireId("canonicalActionId", canonicalActionId);
            if (rpgDayId < 0) throw new IllegalArgumentException("rpgDayId must be >= 0");
            Objects.requireNonNull(state, "state");
        }
    }

    private record StoredUsage(long rpgDayId, int used) {}

    private final Path root;

    public FileTrainerPtuDailyActionLedger(Path root) {
        this.root = Objects.requireNonNull(root, "root").normalize();
    }

    public synchronized UsageView view(
            String canonicalPlayerId,
            String canonicalActionId,
            int maxUsesPerDay,
            long rpgDayId
    ) {
        validate(canonicalPlayerId, canonicalActionId, maxUsesPerDay, rpgDayId);
        Properties properties = load(canonicalPlayerId);
        int used = readUsedForDay(properties, canonicalActionId, rpgDayId, maxUsesPerDay);
        return new UsageView(rpgDayId, used, maxUsesPerDay);
    }

    /**
     * Atomically consumes one use in the durable ledger. This immediate form is retained for
     * mechanics whose effect has already completed inside the same trusted server boundary.
     */
    public synchronized ConsumeResult tryConsume(
            String canonicalPlayerId,
            String canonicalActionId,
            int maxUsesPerDay,
            long rpgDayId
    ) {
        validate(canonicalPlayerId, canonicalActionId, maxUsesPerDay, rpgDayId);
        Properties properties = load(canonicalPlayerId);
        int used = readUsedForDay(properties, canonicalActionId, rpgDayId, maxUsesPerDay);
        if (used >= maxUsesPerDay) {
            return new ConsumeResult(false, new UsageView(rpgDayId, used, maxUsesPerDay));
        }

        int nextUsed = used + 1;
        properties.setProperty(canonicalActionId, rpgDayId + ":" + nextUsed);
        persist(canonicalPlayerId, properties);
        return new ConsumeResult(true, new UsageView(rpgDayId, nextUsed, maxUsesPerDay));
    }

    /**
     * Reserves one use before a multi-step PTU world action executes.
     *
     * <p>The reservation counts against the cap immediately and survives restart. Retrying the
     * same server-generated operation ID is idempotent. On effect success call
     * {@link #commitReservation(String, String)}. If the effect fails before producing its RPG
     * consequence, call {@link #releaseReservation(String, String)}.</p>
     */
    public synchronized ReservationResult tryReserve(
            String canonicalPlayerId,
            String canonicalActionId,
            int maxUsesPerDay,
            long rpgDayId,
            String operationId
    ) {
        validate(canonicalPlayerId, canonicalActionId, maxUsesPerDay, rpgDayId);
        String normalizedOperationId = requireId("operationId", operationId);
        Properties properties = load(canonicalPlayerId);
        String operationKey = operationKey(normalizedOperationId);
        String rawExisting = properties.getProperty(operationKey);
        if (rawExisting != null) {
            OperationRecord existing = decodeOperation(rawExisting);
            if (!existing.operationId().equals(normalizedOperationId)
                    || !existing.canonicalActionId().equals(canonicalActionId)) {
                return new ReservationResult(
                        ReservationStatus.OPERATION_CONFLICT,
                        normalizedOperationId,
                        usageFor(properties, canonicalActionId, maxUsesPerDay, rpgDayId)
                );
            }
            ReservationStatus status = existing.state() == OperationState.COMMITTED
                    ? ReservationStatus.ALREADY_COMMITTED
                    : ReservationStatus.ALREADY_RESERVED;
            return new ReservationResult(
                    status,
                    normalizedOperationId,
                    usageFor(properties, canonicalActionId, maxUsesPerDay, rpgDayId)
            );
        }

        UsageView usage = usageFor(properties, canonicalActionId, maxUsesPerDay, rpgDayId);
        if (!usage.available()) {
            return new ReservationResult(ReservationStatus.LIMIT_REACHED, normalizedOperationId, usage);
        }

        int nextUsed = usage.used() + 1;
        properties.setProperty(canonicalActionId, rpgDayId + ":" + nextUsed);
        properties.setProperty(
                operationKey,
                encodeOperation(new OperationRecord(
                        normalizedOperationId,
                        canonicalActionId,
                        rpgDayId,
                        OperationState.RESERVED
                ))
        );
        persist(canonicalPlayerId, properties);
        return new ReservationResult(
                ReservationStatus.RESERVED,
                normalizedOperationId,
                new UsageView(rpgDayId, nextUsed, maxUsesPerDay)
        );
    }

    public synchronized boolean commitReservation(String canonicalPlayerId, String operationId) {
        String normalizedPlayerId = requireId("canonicalPlayerId", canonicalPlayerId);
        String normalizedOperationId = requireId("operationId", operationId);
        Properties properties = load(normalizedPlayerId);
        String key = operationKey(normalizedOperationId);
        String raw = properties.getProperty(key);
        if (raw == null) return false;
        OperationRecord operation = decodeOperation(raw);
        if (!operation.operationId().equals(normalizedOperationId)) {
            throw new IllegalStateException("trainer PTU action operation hash collision");
        }
        if (operation.state() == OperationState.COMMITTED) return true;
        properties.setProperty(key, encodeOperation(new OperationRecord(
                operation.operationId(),
                operation.canonicalActionId(),
                operation.rpgDayId(),
                OperationState.COMMITTED
        )));
        persist(normalizedPlayerId, properties);
        return true;
    }

    public synchronized boolean releaseReservation(String canonicalPlayerId, String operationId) {
        String normalizedPlayerId = requireId("canonicalPlayerId", canonicalPlayerId);
        String normalizedOperationId = requireId("operationId", operationId);
        Properties properties = load(normalizedPlayerId);
        String key = operationKey(normalizedOperationId);
        String raw = properties.getProperty(key);
        if (raw == null) return false;
        OperationRecord operation = decodeOperation(raw);
        if (!operation.operationId().equals(normalizedOperationId)) {
            throw new IllegalStateException("trainer PTU action operation hash collision");
        }
        if (operation.state() == OperationState.COMMITTED) return false;

        StoredUsage stored = readStoredUsage(properties, operation.canonicalActionId());
        if (stored != null && stored.rpgDayId() == operation.rpgDayId() && stored.used() > 0) {
            int remaining = stored.used() - 1;
            if (remaining == 0) {
                properties.remove(operation.canonicalActionId());
            } else {
                properties.setProperty(
                        operation.canonicalActionId(),
                        stored.rpgDayId() + ":" + remaining
                );
            }
        }
        properties.remove(key);
        persist(normalizedPlayerId, properties);
        return true;
    }

    private static UsageView usageFor(
            Properties properties,
            String canonicalActionId,
            int maxUsesPerDay,
            long rpgDayId
    ) {
        return new UsageView(
                rpgDayId,
                readUsedForDay(properties, canonicalActionId, rpgDayId, maxUsesPerDay),
                maxUsesPerDay
        );
    }

    private static int readUsedForDay(
            Properties properties,
            String actionId,
            long currentDay,
            int currentLimit
    ) {
        StoredUsage stored = readStoredUsage(properties, actionId);
        if (stored == null || stored.rpgDayId() != currentDay) return 0;
        return Math.min(stored.used(), currentLimit);
    }

    private static StoredUsage readStoredUsage(Properties properties, String actionId) {
        String raw = properties.getProperty(actionId);
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.split(":", -1);
        if (parts.length != 2) return null;
        try {
            long storedDay = Long.parseLong(parts[0]);
            int storedUsed = Integer.parseInt(parts[1]);
            if (storedDay < 0 || storedUsed < 0) return null;
            return new StoredUsage(storedDay, storedUsed);
        } catch (NumberFormatException invalidStoredUsage) {
            return null;
        }
    }

    private Properties load(String playerId) {
        Path file = playerFile(playerId);
        Properties properties = new Properties();
        if (!Files.isRegularFile(file)) return properties;
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            return properties;
        } catch (IOException failure) {
            throw new IllegalStateException("failed to read trainer PTU action ledger", failure);
        }
    }

    private void persist(String playerId, Properties properties) {
        try {
            Files.createDirectories(root);
            Path target = playerFile(playerId);
            Path temporary = Files.createTempFile(root, target.getFileName().toString(), ".tmp");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "AutoPTU server-owned trainer PTU daily action usage");
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("failed to persist trainer PTU action ledger", failure);
        }
    }

    private Path playerFile(String playerId) {
        return root.resolve(sha256(playerId) + ".properties").normalize();
    }

    private static String operationKey(String operationId) {
        return OPERATION_PREFIX + sha256(operationId);
    }

    private static String encodeOperation(OperationRecord operation) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return String.join(
                "|",
                OPERATION_SCHEMA,
                operation.state().name(),
                Long.toString(operation.rpgDayId()),
                encoder.encodeToString(operation.operationId().getBytes(StandardCharsets.UTF_8)),
                encoder.encodeToString(operation.canonicalActionId().getBytes(StandardCharsets.UTF_8))
        );
    }

    private static OperationRecord decodeOperation(String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 5 || !OPERATION_SCHEMA.equals(parts[0])) {
            throw new IllegalStateException("invalid trainer PTU action operation record");
        }
        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            return new OperationRecord(
                    new String(decoder.decode(parts[3]), StandardCharsets.UTF_8),
                    new String(decoder.decode(parts[4]), StandardCharsets.UTF_8),
                    Long.parseLong(parts[2]),
                    OperationState.valueOf(parts[1])
            );
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException("invalid trainer PTU action operation record", failure);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void validate(String playerId, String actionId, int limit, long dayId) {
        requireId("canonicalPlayerId", playerId);
        requireId("canonicalActionId", actionId);
        if (actionId.startsWith(OPERATION_PREFIX)) {
            throw new IllegalArgumentException("canonicalActionId uses reserved internal prefix");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("maxUsesPerDay must be >= 1");
        }
        if (dayId < 0) {
            throw new IllegalArgumentException("rpgDayId must be >= 0");
        }
    }

    private static String requireId(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }
}
