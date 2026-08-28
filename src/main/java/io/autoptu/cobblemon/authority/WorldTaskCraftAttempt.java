package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;

/**
 * Durable server-owned journal entry for one capability-sensitive world-task craft.
 *
 * <p>The attempt freezes ingredient reservations before any outcome roll. Once resolved, the
 * quality and output are immutable so reconnect, restart, or request retry cannot reroll the craft.
 * The output item instance id is deterministic per attempt and can therefore be created exactly
 * once.</p>
 */
public record WorldTaskCraftAttempt(
        String attemptId,
        String playerId,
        String recipeId,
        int quantity,
        Phase phase,
        List<PlannedReservation> ingredientReservations,
        int rollPercent,
        WorldTaskRecipeDefinition.CraftQuality quality,
        String outputItemInstanceId,
        String outputTemplateId,
        int outputQuantity
) {
    public WorldTaskCraftAttempt {
        attemptId = requireText(attemptId, "attemptId");
        playerId = requireText(playerId, "playerId");
        recipeId = requireText(recipeId, "recipeId");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        phase = Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(ingredientReservations, "ingredientReservations");
        ingredientReservations = List.copyOf(ingredientReservations);
        if (ingredientReservations.isEmpty()) {
            throw new IllegalArgumentException("ingredientReservations must not be empty");
        }

        if (phase == Phase.PLANNED) {
            if (rollPercent != 0 || quality != null || outputTemplateId != null || outputQuantity != 0) {
                throw new IllegalArgumentException("planned attempt must not contain a resolved outcome");
            }
            outputItemInstanceId = requireText(outputItemInstanceId, "outputItemInstanceId");
        } else if (phase == Phase.RESOLVED || phase == Phase.COMMITTED) {
            if (rollPercent < 1 || rollPercent > 100) {
                throw new IllegalArgumentException("resolved rollPercent must be 1..100");
            }
            quality = Objects.requireNonNull(quality, "quality");
            outputItemInstanceId = requireText(outputItemInstanceId, "outputItemInstanceId");
            outputTemplateId = requireText(outputTemplateId, "outputTemplateId");
            if (outputQuantity <= 0) throw new IllegalArgumentException("outputQuantity must be > 0");
        } else if (phase == Phase.ABORTED) {
            outputItemInstanceId = requireText(outputItemInstanceId, "outputItemInstanceId");
            if (rollPercent != 0 && (rollPercent < 1 || rollPercent > 100)) {
                throw new IllegalArgumentException("aborted rollPercent must be 0 or 1..100");
            }
        }
    }

    public static WorldTaskCraftAttempt planned(
            String attemptId,
            String playerId,
            String recipeId,
            int quantity,
            List<PlannedReservation> ingredientReservations
    ) {
        return new WorldTaskCraftAttempt(
                attemptId,
                playerId,
                recipeId,
                quantity,
                Phase.PLANNED,
                ingredientReservations,
                0,
                null,
                "craft-output:" + attemptId,
                null,
                0
        );
    }

    public WorldTaskCraftAttempt resolved(
            int rollPercent,
            WorldTaskRecipeDefinition.CraftQuality quality,
            WorldTaskRecipeDefinition.CraftOutput output
    ) {
        if (phase != Phase.PLANNED) throw new IllegalStateException("only planned attempts can resolve");
        Objects.requireNonNull(output, "output");
        return new WorldTaskCraftAttempt(
                attemptId,
                playerId,
                recipeId,
                quantity,
                Phase.RESOLVED,
                ingredientReservations,
                rollPercent,
                quality,
                outputItemInstanceId,
                output.itemTemplateId(),
                Math.multiplyExact(output.quantity(), quantity)
        );
    }

    public WorldTaskCraftAttempt committed() {
        if (phase != Phase.RESOLVED && phase != Phase.COMMITTED) {
            throw new IllegalStateException("only resolved attempts can commit");
        }
        if (phase == Phase.COMMITTED) return this;
        return new WorldTaskCraftAttempt(
                attemptId,
                playerId,
                recipeId,
                quantity,
                Phase.COMMITTED,
                ingredientReservations,
                rollPercent,
                quality,
                outputItemInstanceId,
                outputTemplateId,
                outputQuantity
        );
    }

    public WorldTaskCraftAttempt aborted() {
        if (phase == Phase.COMMITTED) throw new IllegalStateException("committed attempt cannot abort");
        return new WorldTaskCraftAttempt(
                attemptId,
                playerId,
                recipeId,
                quantity,
                Phase.ABORTED,
                ingredientReservations,
                rollPercent,
                quality,
                outputItemInstanceId,
                outputTemplateId,
                outputQuantity
        );
    }

    public enum Phase {
        PLANNED,
        RESOLVED,
        COMMITTED,
        ABORTED
    }

    public record PlannedReservation(
            String reservationId,
            String itemInstanceId,
            String itemTemplateId,
            int quantity,
            long itemRevision
    ) {
        public PlannedReservation {
            reservationId = requireText(reservationId, "reservationId");
            itemInstanceId = requireText(itemInstanceId, "itemInstanceId");
            itemTemplateId = requireText(itemTemplateId, "itemTemplateId");
            if (quantity <= 0) throw new IllegalArgumentException("reservation quantity must be > 0");
            if (itemRevision < 0) throw new IllegalArgumentException("itemRevision must be >= 0");
        }

        public ItemReservation asItemReservation(String playerId) {
            return new ItemReservation(
                    reservationId,
                    requireText(playerId, "playerId"),
                    itemInstanceId,
                    itemTemplateId,
                    quantity,
                    itemRevision
            );
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
