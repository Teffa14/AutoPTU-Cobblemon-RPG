package io.autoptu.cobblemon.battlecore;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves reservation-bound authoritative presentation outputs to the exact opaque entity identity
 * registered for each combatant. This class never looks up entities or executes PTU rules.
 */
public final class BattlePresentationEntityProjector {
    public List<EntityBoundBattleHealthProjection> bindHealth(
            BattleHealthProjectionBatch batch,
            BattlePresentationEntityBindings bindings) {
        if (batch == null) throw new IllegalArgumentException("batch is required");
        if (bindings == null) throw new IllegalArgumentException("bindings are required");

        ArrayList<EntityBoundBattleHealthProjection> result = new ArrayList<>();
        for (BattleHealthProjection update : batch.healthUpdates()) {
            PresentationEntityBinding binding = bindings.requireBinding(batch.reservationId(), update.combatantId());
            result.add(new EntityBoundBattleHealthProjection(
                    update.sequence(), update.ordinal(), update.combatantId(), binding.presentationEntityId(),
                    update.damage(), update.targetHp()));
        }
        return List.copyOf(result);
    }

    public List<EntityBoundBattleWorldRelocation> bindRelocations(
            BattleWorldRelocationBatch batch,
            BattlePresentationEntityBindings bindings) {
        if (batch == null) throw new IllegalArgumentException("batch is required");
        if (bindings == null) throw new IllegalArgumentException("bindings are required");

        ArrayList<EntityBoundBattleWorldRelocation> result = new ArrayList<>();
        for (BattleWorldRelocation relocation : batch.relocations()) {
            PresentationEntityBinding binding = bindings.requireBinding(batch.reservationId(), relocation.combatantId());
            result.add(new EntityBoundBattleWorldRelocation(
                    relocation.sequence(), relocation.ordinal(), relocation.combatantId(), binding.presentationEntityId(),
                    relocation.origin(), relocation.destination()));
        }
        return List.copyOf(result);
    }

    /**
     * Binds only semantic cues whose public event contract defines the command subject as a combatant.
     * Trainer-owned and future field/global cues intentionally remain outside this method.
     */
    public List<EntityBoundBattlePresentationCommand> bindCombatantSemanticCues(
            BattlePresentationBatch batch,
            BattlePresentationEntityBindings bindings) {
        if (batch == null) throw new IllegalArgumentException("batch is required");
        if (bindings == null) throw new IllegalArgumentException("bindings are required");

        ArrayList<EntityBoundBattlePresentationCommand> result = new ArrayList<>();
        for (BattlePresentationCommand command : batch.commands()) {
            if (!isCombatantSemanticCue(command.kind())) continue;
            PresentationEntityBinding binding = bindings.requireBinding(batch.reservationId(), command.subjectId());
            result.add(new EntityBoundBattlePresentationCommand(command, binding.presentationEntityId()));
        }
        return List.copyOf(result);
    }

    private static boolean isCombatantSemanticCue(BattlePresentationCommand.Kind kind) {
        return switch (kind) {
            case STATUS_SKIP_CUE, RULE_EFFECT_CUE, PHASE_CUE, TURN_START_CUE, TURN_END_CUE -> true;
            case MOVE_ANIMATION, HP_PROJECTION, ENTITY_RELOCATION, TRAINER_FEATURE_CUE -> false;
        };
    }
}
