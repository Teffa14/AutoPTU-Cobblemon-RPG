package io.autoptu.cobblemon.authority;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;

/**
 * Durable server-owned RPG day identity derived from forward Minecraft day transitions.
 *
 * <p>Moving world time backwards cannot grant another daily window. Reconnects and server restarts
 * reload the same day identity. This class only maps world time to an RPG usage window; it does not
 * define PTU frequencies or battle turn/round lifecycle.</p>
 */
public final class FileMonotonicRpgDayClock {
    public static final long MINECRAFT_DAY_TICKS = 24_000L;

    private final Path stateFile;
    private State state;

    public FileMonotonicRpgDayClock(Path stateFile) {
        this.stateFile = Objects.requireNonNull(stateFile, "stateFile").normalize();
        this.state = load();
    }

    public synchronized long observeWorldTime(long worldTimeOfDayTicks) {
        long observedWorldDay = Math.floorDiv(Math.max(0L, worldTimeOfDayTicks), MINECRAFT_DAY_TICKS);
        if (state == null) {
            state = new State(0L, observedWorldDay);
            persist(state);
            return state.rpgDayId();
        }
        if (observedWorldDay <= state.lastAcceptedWorldDay()) {
            return state.rpgDayId();
        }

        long delta = observedWorldDay - state.lastAcceptedWorldDay();
        state = new State(Math.addExact(state.rpgDayId(), delta), observedWorldDay);
        persist(state);
        return state.rpgDayId();
    }

    public synchronized long currentRpgDayId() {
        return state == null ? 0L : state.rpgDayId();
    }

    private State load() {
        if (!Files.isRegularFile(stateFile)) return null;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(stateFile)) {
            properties.load(input);
            long rpgDayId = Long.parseLong(properties.getProperty("rpgDayId"));
            long lastAcceptedWorldDay = Long.parseLong(properties.getProperty("lastAcceptedWorldDay"));
            if (rpgDayId < 0 || lastAcceptedWorldDay < 0) {
                throw new IllegalStateException("invalid persisted RPG day state");
            }
            return new State(rpgDayId, lastAcceptedWorldDay);
        } catch (IOException | NumberFormatException failure) {
            throw new IllegalStateException("failed to read RPG day state", failure);
        }
    }

    private void persist(State value) {
        try {
            Path parent = stateFile.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, stateFile.getFileName().toString(), ".tmp");
            Properties properties = new Properties();
            properties.setProperty("rpgDayId", Long.toString(value.rpgDayId()));
            properties.setProperty("lastAcceptedWorldDay", Long.toString(value.lastAcceptedWorldDay()));
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "AutoPTU server-owned monotonic RPG day");
            }
            try {
                Files.move(temporary, stateFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, stateFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("failed to persist RPG day state", failure);
        }
    }

    private record State(long rpgDayId, long lastAcceptedWorldDay) {}
}
