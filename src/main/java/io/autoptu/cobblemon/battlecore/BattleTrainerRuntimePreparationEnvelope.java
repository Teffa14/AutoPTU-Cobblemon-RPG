package io.autoptu.cobblemon.battlecore;

import java.util.Objects;
import java.util.Set;

/**
 * Final integration-owned trainer binding around the reservation-scoped runtime preparation envelope.
 *
 * Trainer Feature ownership, battle-start AP and combatant-controller identity come only from the
 * canonical battle reservation. AutoPTU-Java remains responsible for TrainerRuntimeState mutation,
 * AP spending/restoration, perk execution, combat-stage mutation and all other PTU lifecycle rules.
 */
public record BattleTrainerRuntimePreparationEnvelope(
        BattleRuntimePreparationEnvelope battle,
        BattleTrainerRuntimeProjection trainer
) {
    public BattleTrainerRuntimePreparationEnvelope {
        battle = Objects.requireNonNull(battle, "battle");
        trainer = Objects.requireNonNull(trainer, "trainer");

        Set<String> roster = battle.combatants().keySet();
        if (!trainer.controlledCombatantIds().equals(roster)) {
            throw new IllegalArgumentException("trainer-controlled combatants must exactly match the prepared battle roster");
        }
    }

    public static BattleTrainerRuntimePreparationEnvelope from(
            BattleRuntimePreparationEnvelope battle,
            BattleCoreTrainerRuntimeBootstrapProjection trainerRuntime
    ) {
        Objects.requireNonNull(battle, "battle");
        Objects.requireNonNull(trainerRuntime, "trainerRuntime");
        if (!battle.reservationId().equals(trainerRuntime.reservationId())) {
            throw new IllegalArgumentException("trainer runtime and battle preparation must share one reservation");
        }
        return new BattleTrainerRuntimePreparationEnvelope(battle, trainerRuntime.trainer());
    }

    public String reservationId() {
        return battle.reservationId();
    }

    public long rngSeed() {
        return battle.rngSeed();
    }

    public boolean readyForRuntimeMaterialization() {
        return battle.readyForRuntimeMaterialization();
    }
}
