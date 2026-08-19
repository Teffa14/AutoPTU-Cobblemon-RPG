package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;

import java.util.Objects;

/**
 * Reservation-scoped Trainer Feature identities prepared for the upstream perk registry.
 * This projection transports identity only; feature execution remains AutoPTU-Java-owned.
 */
public record BattleCoreTrainerFeatureBootstrapProjection(
        String reservationId,
        BattleTrainerFeatureProjection trainer
) {
    public BattleCoreTrainerFeatureBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        reservationId = reservationId.strip();
        trainer = Objects.requireNonNull(trainer, "trainer");
    }

    public static BattleCoreTrainerFeatureBootstrapProjection from(BattleAuthoritySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (!snapshot.playerId().equals(snapshot.trainer().playerId())) {
            throw new IllegalArgumentException("battle trainer must match authoritative playerId");
        }
        return new BattleCoreTrainerFeatureBootstrapProjection(
                snapshot.reservationId(),
                new BattleTrainerFeatureProjection(
                        snapshot.trainer().playerId(),
                        snapshot.trainer().trainerFeatures()
                )
        );
    }
}
