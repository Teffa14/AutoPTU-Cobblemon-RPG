package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;

/**
 * Durable server-owned journal entry for one capability-sensitive world-task craft.
 *
 * <p>The attempt freezes ingredient reservations and the server-authored quality distribution before
 * any outcome roll. Once resolved, quality and output are immutable so reconnect, restart, skill
 * changes, or request retry cannot improve the same attempt.</p>
 */
public record WorldTaskCraftAttempt(
        String attemptId,
        String playerId,
        String recipeId,
        int quantity,
        Phase phase,
        List<PlannedReservation> ingredientReservations,
        int improvisedPercent,
        int standardPercent,
        int excellentPercent,
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
        if (improvisedPercent < 0 || standardPercent < 0 || excellentPercent < 0
                || improvisedPercent + standardPercent + excellentPercent != 100) {
            throw new IllegalArgumentException("frozen quality percentages must be nonnegative and sum to 100");
        }

        outputItemInstanceId = requireText(outputItemInstanceId, "outputItemInstanceId");
        if (phase == Phase.PLANNED) {
            if (rollPercent != 0 || quality != null || outputTemplateId != null || outputQuantity != 0) {
                throw new IllegalArgumentException("planned attempt must not contain a resolved outcome");
            }
        } else if (phase == Phase.RESOLVED || phase == Phase.COMMITTED) {
            if (rollPercent < 1 || rollPercent > 100) {
                throw new IllegalArgumentException("resolved rollPercent must be 1..100");
            }
            quality = Objects.requireNonNull(quality, "quality");
            outputTemplateId = requireText(outputTemplateId, "outputTemplateId");
            if (outputQuantity <= 0) throw new IllegalArgumentException("outputQuantity must be > 0");
        } else if (phase == Phase.ABORTED) {
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
            List<PlannedReservation> ingredientReservations,
            WorldTaskDefinition.QualityDistribution distribution
    ) {
        Objects.requireNonNull(distribution, "distribution");
        return new WorldTaskCraftAttempt(
                attemptId,
                playerId,
                recipeId,
                quantity,
                Phase.PLANNED,
                ingredientReservations,
                distribution.improvisedPercent(),
                distribution.standardPercent(),
                distribution.excellentPercent(),
                0,
                null,
                "craft-output:" + attemptId,
                null,
                0
        );
    }

    public WorldTaskDefinition.QualityDistribution frozenDistribution() {
        return new WorldTaskDefinition.QualityDistribution(
                improvisedPercent, standardPercent, excellentPercent);
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
                improvisedPercent,
                standardPercent,
                excellentPercent,
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
                attemptId, playerId, recipeId, quantity, Phase.COMMITTED, ingredientReservations,
                improvisedPercent, standardPercent, excellentPercent,
                rollPercent, quality, outputItemInstanceId, outputTemplateId, outputQuantity);
    }

    public WorldTaskCraftAttempt aborted() {
        if (phase == Phase.COMMITTED) throw new IllegalStateException("committed attempt cannot abort");
        return new WorldTaskCraftAttempt(
                attemptId, playerId, recipeId, quantity, Phase.ABORTED, ingredientReservations,
                improvisedPercent, standardPercent, excellentPercent,
                rollPercent, quality, outputItemInstanceId, outputTemplateId, outputQuantity);
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
