package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;

import java.util.List;
import java.util.Objects;

/** Ordered world-space relocations for one frozen battle reservation. */
public record BattleWorldRelocationBatch(
        String reservationId,
        BattleArenaSnapshot arena,
        List<BattleWorldRelocation> relocations
) {
    public BattleWorldRelocationBatch {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        reservationId = reservationId.strip();
        arena = Objects.requireNonNull(arena, "arena");
        relocations = List.copyOf(relocations == null ? List.of() : relocations);

        long priorSequence = -1;
        int priorOrdinal = -1;
        for (BattleWorldRelocation relocation : relocations) {
            if (relocation == null) throw new IllegalArgumentException("relocations cannot contain null");
            if (!arena.dimensionId().equals(relocation.origin().dimensionId())
                    || !arena.dimensionId().equals(relocation.destination().dimensionId())) {
                throw new IllegalArgumentException("relocation must use the frozen battle dimension");
            }
            if (relocation.sequence() < priorSequence) {
                throw new IllegalArgumentException("relocation sequence must not move backward");
            }
            if (relocation.sequence() == priorSequence && relocation.ordinal() <= priorOrdinal) {
                throw new IllegalArgumentException("relocation ordinal must increase within an event");
            }
            if (relocation.sequence() != priorSequence) priorOrdinal = -1;
            priorSequence = relocation.sequence();
            priorOrdinal = relocation.ordinal();
        }
    }
}
