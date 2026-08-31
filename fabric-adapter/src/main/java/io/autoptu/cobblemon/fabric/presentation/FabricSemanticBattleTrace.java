package io.autoptu.cobblemon.fabric.presentation;

import io.autoptu.cobblemon.battlecore.BattleEventPlaybackEnvelope;
import io.autoptu.cobblemon.battlecore.BattlePlaybackBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded server-side evidence of semantic battle events received from AutoPTU-Java.
 *
 * This registry stores the authoritative envelopes exactly as received for presentation. It never
 * derives HP, fainting, turn state, legality, damage, statuses, winners, rewards or any other PTU
 * outcome. Repeated overlapping batches are idempotent; conflicting content at the same sequence
 * fails closed so presentation evidence cannot silently rewrite history.
 */
public final class FabricSemanticBattleTrace {
    private static final int MAX_EVENTS_PER_RESERVATION = 256;
    private static final ConcurrentHashMap<String, TraceState> TRACES = new ConcurrentHashMap<>();

    private FabricSemanticBattleTrace() {}

    public static void record(BattlePlaybackBatch playback) {
        Objects.requireNonNull(playback, "playback");
        TraceState state = TRACES.computeIfAbsent(playback.reservationId(), ignored -> new TraceState());
        synchronized (state) {
            for (BattleEventPlaybackEnvelope event : playback.events()) {
                BattleEventPlaybackEnvelope existing = state.events.get(event.sequence());
                if (existing != null) {
                    if (!existing.equals(event)) {
                        throw new IllegalStateException(
                                "conflicting authoritative battle event at sequence " + event.sequence());
                    }
                    continue;
                }
                state.events.put(event.sequence(), event);
            }
            while (state.events.size() > MAX_EVENTS_PER_RESERVATION) {
                state.events.pollFirstEntry();
            }
        }
    }

    public static List<BattleEventPlaybackEnvelope> snapshot(String reservationId) {
        String normalized = normalize(reservationId);
        TraceState state = TRACES.get(normalized);
        if (state == null) return List.of();
        synchronized (state) {
            return List.copyOf(new ArrayList<>(state.events.values()));
        }
    }

    public static int eventCount(String reservationId) {
        return snapshot(reservationId).size();
    }

    public static void release(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) return;
        TRACES.remove(reservationId.strip());
    }

    static void clearAllForTests() {
        TRACES.clear();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("reservationId is required");
        return value.strip();
    }

    private static final class TraceState {
        private final NavigableMap<Long, BattleEventPlaybackEnvelope> events = new TreeMap<>();
    }
}
