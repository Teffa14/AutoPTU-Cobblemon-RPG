package io.autoptu.cobblemon.battlecore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * Binds both combatant endpoints of an already-authoritative move animation.
     * Target legality, move resolution and damage remain upstream-owned; this only prevents
     * a correct move event from being redirected to unrelated presentation entities.
     */
    public List<EntityBoundMoveAnimation> bindMoveAnimations(
            BattlePresentationBatch batch,
            BattlePresentationEntityBindings bindings) {
        if (batch == null) throw new IllegalArgumentException("batch is required");
        if (bindings == null) throw new IllegalArgumentException("bindings are required");

        ArrayList<EntityBoundMoveAnimation> result = new ArrayList<>();
        for (BattlePresentationCommand command : batch.commands()) {
            if (command.kind() != BattlePresentationCommand.Kind.MOVE_ANIMATION) continue;
            PresentationEntityBinding attacker = bindings.requireBinding(
                    batch.reservationId(), command.subjectId());
            String targetCombatantId = command.data().get("targetId");
            if (targetCombatantId == null || targetCombatantId.isBlank()) {
                throw new IllegalArgumentException("MOVE_ANIMATION targetId is required");
            }
            PresentationEntityBinding target = bindings.requireBinding(
                    batch.reservationId(), targetCombatantId.strip());
            result.add(new EntityBoundMoveAnimation(
                    command, attacker.presentationEntityId(), target.presentationEntityId()));
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

    /**
     * Produces one ordered combatant presentation stream from the separately typed playback projections.
     * The batches must represent the same reservation and HP/relocation projections must exactly cover
     * their corresponding semantic commands. Trainer-owned and global/field cues remain unbound.
     */
    public BattleEntityBoundPresentationStream bindCombatantStream(
            BattlePresentationBatch presentationBatch,
            BattleHealthProjectionBatch healthBatch,
            BattleWorldRelocationBatch relocationBatch,
            BattlePresentationEntityBindings bindings) {
        if (presentationBatch == null) throw new IllegalArgumentException("presentationBatch is required");
        if (healthBatch == null) throw new IllegalArgumentException("healthBatch is required");
        if (relocationBatch == null) throw new IllegalArgumentException("relocationBatch is required");
        if (bindings == null) throw new IllegalArgumentException("bindings are required");

        String reservationId = presentationBatch.reservationId();
        requireReservation(reservationId, healthBatch.reservationId(), "healthBatch");
        requireReservation(reservationId, relocationBatch.reservationId(), "relocationBatch");
        validateProjectionCoverage(presentationBatch, healthBatch, relocationBatch);

        ArrayList<EntityBoundPresentationOutput> outputs = new ArrayList<>();
        outputs.addAll(bindMoveAnimations(presentationBatch, bindings));
        outputs.addAll(bindCombatantSemanticCues(presentationBatch, bindings));
        outputs.addAll(bindHealth(healthBatch, bindings));
        outputs.addAll(bindRelocations(relocationBatch, bindings));
        outputs.sort(Comparator.comparingLong(EntityBoundPresentationOutput::sequence)
                .thenComparingInt(EntityBoundPresentationOutput::ordinal));
        return new BattleEntityBoundPresentationStream(reservationId, outputs);
    }

    private static void validateProjectionCoverage(
            BattlePresentationBatch presentationBatch,
            BattleHealthProjectionBatch healthBatch,
            BattleWorldRelocationBatch relocationBatch) {
        Set<String> expectedHealth = new HashSet<>();
        Set<String> expectedRelocations = new HashSet<>();
        for (BattlePresentationCommand command : presentationBatch.commands()) {
            if (command.kind() == BattlePresentationCommand.Kind.HP_PROJECTION) {
                expectedHealth.add(outputKey(command.sequence(), command.ordinal(), command.subjectId()));
            } else if (command.kind() == BattlePresentationCommand.Kind.ENTITY_RELOCATION) {
                expectedRelocations.add(outputKey(command.sequence(), command.ordinal(), command.subjectId()));
            }
        }

        Set<String> actualHealth = new HashSet<>();
        for (BattleHealthProjection update : healthBatch.healthUpdates()) {
            actualHealth.add(outputKey(update.sequence(), update.ordinal(), update.combatantId()));
        }
        Set<String> actualRelocations = new HashSet<>();
        for (BattleWorldRelocation relocation : relocationBatch.relocations()) {
            actualRelocations.add(outputKey(relocation.sequence(), relocation.ordinal(), relocation.combatantId()));
        }

        if (!expectedHealth.equals(actualHealth)) {
            throw new IllegalArgumentException("health projection coverage must match HP_PROJECTION commands");
        }
        if (!expectedRelocations.equals(actualRelocations)) {
            throw new IllegalArgumentException("relocation coverage must match ENTITY_RELOCATION commands");
        }
    }

    private static String outputKey(long sequence, int ordinal, String combatantId) {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId is required");
        }
        return sequence + ":" + ordinal + ":" + combatantId.strip();
    }

    private static void requireReservation(String expected, String actual, String field) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(field + " reservation must match presentation batch");
        }
    }

    private static boolean isCombatantSemanticCue(BattlePresentationCommand.Kind kind) {
        return switch (kind) {
            case STATUS_SKIP_CUE, RULE_EFFECT_CUE, PHASE_CUE, TURN_START_CUE, TURN_END_CUE -> true;
            case MOVE_ANIMATION, HP_PROJECTION, ENTITY_RELOCATION, TRAINER_FEATURE_CUE -> false;
        };
    }
}
