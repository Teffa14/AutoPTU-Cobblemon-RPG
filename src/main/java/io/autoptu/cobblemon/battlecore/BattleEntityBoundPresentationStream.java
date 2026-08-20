package io.autoptu.cobblemon.battlecore;

import java.util.List;

/**
 * One reservation-scoped presentation stream in authoritative event/ordinal order.
 *
 * Every included output has already been bound to the exact opaque presentation entity registered
 * for its combatant. Trainer-owned and global/field presentation remain outside this combatant stream
 * until they have their own identity contracts.
 */
public record BattleEntityBoundPresentationStream(
        String reservationId,
        List<EntityBoundPresentationOutput> outputs
) {
    public BattleEntityBoundPresentationStream {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        reservationId = reservationId.strip();
        outputs = List.copyOf(outputs == null ? List.of() : outputs);

        long priorSequence = -1;
        int priorOrdinal = -1;
        for (EntityBoundPresentationOutput output : outputs) {
            if (output == null) throw new IllegalArgumentException("outputs cannot contain null");
            if (output.sequence() < priorSequence) {
                throw new IllegalArgumentException("output sequence must not move backward");
            }
            if (output.sequence() == priorSequence && output.ordinal() <= priorOrdinal) {
                throw new IllegalArgumentException("output ordinal must increase within an event");
            }
            if (output.sequence() != priorSequence) priorOrdinal = -1;
            priorSequence = output.sequence();
            priorOrdinal = output.ordinal();
        }
    }
}
