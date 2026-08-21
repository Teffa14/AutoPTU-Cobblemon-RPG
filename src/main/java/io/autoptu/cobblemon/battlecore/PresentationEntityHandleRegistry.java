package io.autoptu.cobblemon.battlecore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Server-side adapter registry that binds opaque presentation entity IDs to live platform handles.
 *
 * This registry contains presentation identity only. Platform handles never become PTU combatant
 * state and cannot provide stats, HP, movement, targeting, initiative, items, abilities or results.
 */
public final class PresentationEntityHandleRegistry<T> {
    private final Map<String, Map<String, T>> handlesByReservation = new HashMap<>();

    public synchronized void register(String reservationId, String presentationEntityId, T handle) {
        String reservation = requireIdentifier(reservationId, "reservationId");
        String entityId = requireIdentifier(presentationEntityId, "presentationEntityId");
        T entityHandle = Objects.requireNonNull(handle, "handle");

        Map<String, T> handles = handlesByReservation.computeIfAbsent(reservation, ignored -> new HashMap<>());
        T existing = handles.get(entityId);
        if (existing != null && !Objects.equals(existing, entityHandle)) {
            throw new IllegalStateException("presentation entity id is already bound in reservation");
        }
        for (Map.Entry<String, T> entry : handles.entrySet()) {
            if (!entry.getKey().equals(entityId) && Objects.equals(entry.getValue(), entityHandle)) {
                throw new IllegalStateException("platform entity handle is already bound in reservation");
            }
        }
        handles.put(entityId, entityHandle);
    }

    public synchronized T require(String reservationId, String presentationEntityId) {
        String reservation = requireIdentifier(reservationId, "reservationId");
        String entityId = requireIdentifier(presentationEntityId, "presentationEntityId");
        Map<String, T> handles = handlesByReservation.get(reservation);
        if (handles == null || !handles.containsKey(entityId)) {
            throw new IllegalStateException("presentation entity is not registered for reservation");
        }
        return handles.get(entityId);
    }

    public synchronized void releaseReservation(String reservationId) {
        handlesByReservation.remove(requireIdentifier(reservationId, "reservationId"));
    }

    public synchronized int registeredCount(String reservationId) {
        Map<String, T> handles = handlesByReservation.get(requireIdentifier(reservationId, "reservationId"));
        return handles == null ? 0 : handles.size();
    }

    private static String requireIdentifier(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
