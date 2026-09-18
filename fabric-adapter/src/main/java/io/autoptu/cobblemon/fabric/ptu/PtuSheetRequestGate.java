package io.autoptu.cobblemon.fabric.ptu;

import java.util.*;

/** Small server-side rate gate, separate from membership authorization performed on every request. */
public final class PtuSheetRequestGate {
    private final Map<UUID, Long> nextAllowed = new HashMap<>();
    public boolean allow(UUID player, long monotonicMillis) {
        if (monotonicMillis < nextAllowed.getOrDefault(player, Long.MIN_VALUE)) return false;
        nextAllowed.put(player, monotonicMillis + 200L);
        return true;
    }
    public void remove(UUID player) { nextAllowed.remove(player); }
    public void clear() { nextAllowed.clear(); }
}
