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
     * Atomically reserves one use in the durable ledger. The supplied limit must come from a
     * trusted server-side action definition. No PTU effect is calculated here.
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

    private static int readUsedForDay(
            Properties properties,
            String actionId,
            long currentDay,
            int currentLimit
    ) {
        String raw = properties.getProperty(actionId);
        if (raw == null || raw.isBlank()) return 0;
        String[] parts = raw.split(":", -1);
        if (parts.length != 2) return 0;
        try {
            long storedDay = Long.parseLong(parts[0]);
            int storedUsed = Integer.parseInt(parts[1]);
            if (storedDay != currentDay || storedUsed < 0) return 0;
            return Math.min(storedUsed, currentLimit);
        } catch (NumberFormatException invalidStoredUsage) {
            return 0;
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

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void validate(String playerId, String actionId, int limit, long dayId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("canonicalPlayerId must not be blank");
        }
        if (actionId == null || actionId.isBlank()) {
            throw new IllegalArgumentException("canonicalActionId must not be blank");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("maxUsesPerDay must be >= 1");
        }
        if (dayId < 0) {
            throw new IllegalArgumentException("rpgDayId must be >= 0");
        }
    }
}
