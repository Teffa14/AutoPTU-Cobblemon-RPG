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
}
