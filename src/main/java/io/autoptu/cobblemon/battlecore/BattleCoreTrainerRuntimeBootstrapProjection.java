package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Reservation-scoped trainer state for AutoPTU-Java TrainerRuntimeState plus controller binding.
 * Minecraft/client state cannot override Trainer Feature ownership, skill ranks, AP, initiative modifier or combatant controller identity.
 */
public record BattleCoreTrainerRuntimeBootstrapProjection(
        String reservationId,
        BattleTrainerRuntimeProjection trainer
) {
    public BattleCoreTrainerRuntimeBootstrapProjection {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        reservationId = reservationId.strip();
        trainer = Objects.requireNonNull(trainer, "trainer");
    }

    public static BattleCoreTrainerRuntimeBootstrapProjection from(BattleAuthoritySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (!snapshot.playerId().equals(snapshot.trainer().playerId())) {
            throw new IllegalArgumentException("battle trainer must match authoritative playerId");
        }
        if (snapshot.roster().isEmpty()) {
            throw new IllegalArgumentException("battle roster must not be empty");
        }

        LinkedHashSet<String> controlledCombatants = new LinkedHashSet<>();
        for (BattlePokemonSnapshot pokemon : snapshot.roster()) {
            if (!snapshot.playerId().equals(pokemon.ownerPlayerId())) {
                throw new IllegalArgumentException("combatant controller must derive from canonical Pokemon owner: " + pokemon.pokemonId());
            }
            if (!controlledCombatants.add(pokemon.pokemonId())) {
                throw new IllegalArgumentException("duplicate combatant in trainer binding: " + pokemon.pokemonId());
            }
        }

        return new BattleCoreTrainerRuntimeBootstrapProjection(
                snapshot.reservationId(),
                new BattleTrainerRuntimeProjection(
                        snapshot.trainer().playerId(),
                        snapshot.trainer().trainerFeatures(),
                        snapshot.trainer().actionPoints(),
                        snapshot.trainer().initiativeModifier(),
                        snapshot.trainer().skillRanks(),
                        Set.copyOf(controlledCombatants)
                )
        );
    }
}
