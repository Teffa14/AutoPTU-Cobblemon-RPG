package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Server-authoritative, restart-safe world-task craft transaction.
 *
 * <p>The service freezes a material plan before rolling, durably persists the quality before any
 * material consumption, consumes ingredients while retaining their locks, creates one deterministic
 * output item, durably commits the attempt, then releases the consumed locks. Reconnect/restart and
 * retry with the same attempt id therefore reuse the same outcome and cannot duplicate output.</p>
 */
public final class WorldTaskCraftService {
    private final FileCanonicalItemReservationRepository items;
    private final FileWorldTaskCraftAttemptRepository attempts;
    private final WorldTaskCompetenceService competence;
    private final RollSource rollSource;

    public WorldTaskCraftService(
            FileCanonicalItemReservationRepository items,
            FileWorldTaskCraftAttemptRepository attempts,
            WorldTaskCompetenceService competence,
            RollSource rollSource
    ) {
        this.items = Objects.requireNonNull(items, "items");
        this.attempts = Objects.requireNonNull(attempts, "attempts");
        this.competence = Objects.requireNonNull(competence, "competence");
        this.rollSource = Objects.requireNonNull(rollSource, "rollSource");
    }

    public WorldTaskCraftService(
            FileCanonicalItemReservationRepository items,
            FileWorldTaskCraftAttemptRepository attempts
    ) {
        this(items, attempts, new WorldTaskCompetenceService(),
                () -> ThreadLocalRandom.current().nextInt(1, 101));
    }

    public CraftResult craft(
            String attemptId,
            CanonicalPlayerState player,
            WorldTaskRecipeDefinition recipe,
            int quantity
    ) {
        requireText(attemptId, "attemptId");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(recipe, "recipe");
        if (quantity <= 0) return CraftResult.rejected(Status.INVALID_QUANTITY, "quantity must be > 0");

        WorldTaskCraftAttempt attempt = attempts.find(attemptId).orElse(null);
        if (attempt == null) {
            WorldTaskCompetenceService.Assessment assessment = competence.assess(player, recipe.task());
            if (!assessment.understood()) {
                return CraftResult.rejected(Status.KNOWLEDGE_LOCKED, assessment.detail());
            }
            List<WorldTaskCraftAttempt.PlannedReservation> plan = planReservations(
                    attemptId, player.playerId(), recipe, quantity);
            if (plan == null) {
                return CraftResult.rejected(Status.INSUFFICIENT_INGREDIENTS,
                        "canonical ingredient quantities are insufficient");
            }
            WorldTaskCraftAttempt planned = WorldTaskCraftAttempt.planned(
                    attemptId, player.playerId(), recipe.taskId(), quantity, plan);
            attempts.createIfAbsent(planned);
            attempt = attempts.find(attemptId).orElseThrow();
        }

        if (!sameRequest(attempt, player, recipe, quantity)) {
            return CraftResult.rejected(Status.ATTEMPT_CONFLICT,
                    "attempt id is already bound to a different craft request");
        }
        if (attempt.phase() == WorldTaskCraftAttempt.Phase.ABORTED) {
            return CraftResult.of(Status.ABORTED, attempt, "attempt was aborted before outcome commit");
        }

        if (attempt.phase() == WorldTaskCraftAttempt.Phase.PLANNED) {
            if (!ensureReservations(attempt)) {
                releaseUnconsumedReservations(attempt);
                WorldTaskCraftAttempt aborted = attempt.aborted();
                attempts.replaceIfPhase(attempt.attemptId(), WorldTaskCraftAttempt.Phase.PLANNED, aborted);
                WorldTaskCraftAttempt current = attempts.find(attempt.attemptId()).orElse(aborted);
                return CraftResult.of(Status.INGREDIENT_CONFLICT, current,
                        "ingredient reservation changed before the craft outcome was rolled");
            }
            int roll = rollSource.rollPercent();
            if (roll < 1 || roll > 100) throw new IllegalStateException("roll source must return 1..100");
            WorldTaskDefinition.QualityDistribution distribution = competence.assess(player, recipe.task()).distribution();
            WorldTaskRecipeDefinition.CraftQuality quality = qualityFor(roll, distribution);
            WorldTaskCraftAttempt resolved = attempt.resolved(roll, quality, recipe.outputFor(quality));
            attempts.replaceIfPhase(attempt.attemptId(), WorldTaskCraftAttempt.Phase.PLANNED, resolved);
            attempt = attempts.find(attempt.attemptId()).orElseThrow();
        }

        if (attempt.phase() == WorldTaskCraftAttempt.Phase.RESOLVED) {
            if (!ensureResolvedReservations(attempt)) {
                return CraftResult.of(Status.RECOVERY_REQUIRED, attempt,
                        "resolved craft lost an ingredient lock; no reroll or output was issued");
            }
            for (WorldTaskCraftAttempt.PlannedReservation reservation : attempt.ingredientReservations()) {
                if (!items.consumeReservationRetainingLock(reservation.reservationId(), attempt.playerId())) {
                    return CraftResult.of(Status.RECOVERY_REQUIRED, attempt,
                            "ingredient consumption could not be recovered safely");
                }
            }
            if (!ensureOutput(attempt)) {
                return CraftResult.of(Status.RECOVERY_REQUIRED, attempt,
                        "deterministic output id conflicts with canonical item state");
            }
            attempts.replaceIfPhase(attempt.attemptId(), WorldTaskCraftAttempt.Phase.RESOLVED, attempt.committed());
            attempt = attempts.find(attempt.attemptId()).orElseThrow();
        }

        if (attempt.phase() == WorldTaskCraftAttempt.Phase.COMMITTED) {
            if (!ensureOutput(attempt)) {
                return CraftResult.of(Status.RECOVERY_REQUIRED, attempt,
                        "committed craft output is not canonical");
            }
            boolean cleaned = true;
            for (WorldTaskCraftAttempt.PlannedReservation reservation : attempt.ingredientReservations()) {
                cleaned &= items.releaseConsumedReservationLock(reservation.reservationId(), attempt.playerId());
            }
            return CraftResult.of(
                    cleaned ? Status.COMMITTED : Status.COMMITTED_CLEANUP_PENDING,
                    attempt,
                    cleaned ? "craft committed exactly once" : "craft committed; ingredient lock cleanup remains pending"
            );
        }

        return CraftResult.of(Status.ABORTED, attempt, "craft did not reach a committed phase");
    }

    private List<WorldTaskCraftAttempt.PlannedReservation> planReservations(
            String attemptId,
            String playerId,
            WorldTaskRecipeDefinition recipe,
            int craftQuantity
    ) {
        List<WorldTaskCraftAttempt.PlannedReservation> plan = new ArrayList<>();
        int ingredientIndex = 0;
        for (WorldTaskRecipeDefinition.IngredientRequirement requirement : recipe.ingredients()) {
            int remaining = Math.multiplyExact(requirement.quantity(), craftQuantity);
            List<CanonicalItemInstance> stacks = items.findReservableItems(playerId, requirement.itemTemplateId());
            int part = 0;
            for (CanonicalItemInstance stack : stacks) {
                if (remaining == 0) break;
                int reserved = Math.min(remaining, stack.quantity());
                plan.add(new WorldTaskCraftAttempt.PlannedReservation(
                        "craft:" + attemptId + ":ingredient:" + ingredientIndex + ":part:" + part,
                        stack.itemInstanceId(),
                        stack.templateId(),
                        reserved,
                        stack.revision()
                ));
                remaining -= reserved;
                part++;
            }
            if (remaining != 0) return null;
            ingredientIndex++;
        }
        return List.copyOf(plan);
    }

    private boolean ensureReservations(WorldTaskCraftAttempt attempt) {
        for (WorldTaskCraftAttempt.PlannedReservation planned : attempt.ingredientReservations()) {
            ItemReservation expected = planned.asItemReservation(attempt.playerId());
            Optional<ItemReservation> existing = items.findReservation(planned.reservationId());
            if (existing.isPresent()) {
                if (!existing.get().equals(expected)) return false;
                continue;
            }
            if (!items.tryReserveItem(expected)) return false;
        }
        return true;
    }

    private boolean ensureResolvedReservations(WorldTaskCraftAttempt attempt) {
        for (WorldTaskCraftAttempt.PlannedReservation planned : attempt.ingredientReservations()) {
            ItemReservation active = items.findReservation(planned.reservationId()).orElse(null);
            if (active == null || !active.playerId().equals(attempt.playerId())) return false;
            if (!active.itemInstanceId().equals(planned.itemInstanceId())
                    || !active.itemTemplateId().equals(planned.itemTemplateId())
                    || active.quantity() != planned.quantity()) return false;
            if (!items.isReservationConsumed(planned.reservationId(), attempt.playerId())
                    && active.itemRevision() != planned.itemRevision()) return false;
        }
        return true;
    }

    private void releaseUnconsumedReservations(WorldTaskCraftAttempt attempt) {
        for (WorldTaskCraftAttempt.PlannedReservation planned : attempt.ingredientReservations()) {
            if (items.findReservation(planned.reservationId()).isPresent()
                    && !items.isReservationConsumed(planned.reservationId(), attempt.playerId())) {
                items.releaseItemReservation(planned.reservationId(), attempt.playerId());
            }
        }
    }

    private boolean ensureOutput(WorldTaskCraftAttempt attempt) {
        CanonicalItemInstance expected = new CanonicalItemInstance(
                attempt.outputItemInstanceId(),
                attempt.playerId(),
                attempt.outputTemplateId(),
                attempt.outputQuantity(),
                0
        );
        CanonicalItemInstance existing = items.findItem(expected.itemInstanceId()).orElse(null);
        if (existing != null) return existing.equals(expected);
        if (items.createItemIfAbsent(expected)) return true;
        return items.findItem(expected.itemInstanceId()).map(expected::equals).orElse(false);
    }

    private static WorldTaskRecipeDefinition.CraftQuality qualityFor(
            int roll,
            WorldTaskDefinition.QualityDistribution distribution
    ) {
        if (roll <= distribution.improvisedPercent()) return WorldTaskRecipeDefinition.CraftQuality.IMPROVISED;
        if (roll <= distribution.improvisedPercent() + distribution.standardPercent()) {
            return WorldTaskRecipeDefinition.CraftQuality.STANDARD;
        }
        return WorldTaskRecipeDefinition.CraftQuality.EXCELLENT;
    }

    private static boolean sameRequest(
            WorldTaskCraftAttempt attempt,
            CanonicalPlayerState player,
            WorldTaskRecipeDefinition recipe,
            int quantity
    ) {
        return attempt.playerId().equals(player.playerId())
                && attempt.recipeId().equals(recipe.taskId())
                && attempt.quantity() == quantity;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    @FunctionalInterface
    public interface RollSource {
        int rollPercent();
    }

    public enum Status {
        COMMITTED,
        COMMITTED_CLEANUP_PENDING,
        KNOWLEDGE_LOCKED,
        INSUFFICIENT_INGREDIENTS,
        INGREDIENT_CONFLICT,
        ATTEMPT_CONFLICT,
        INVALID_QUANTITY,
        RECOVERY_REQUIRED,
        ABORTED
    }

    public record CraftResult(Status status, WorldTaskCraftAttempt attempt, String detail) {
        public CraftResult {
            status = Objects.requireNonNull(status, "status");
            detail = detail == null ? "" : detail;
        }

        public static CraftResult rejected(Status status, String detail) {
            return new CraftResult(status, null, detail);
        }

        public static CraftResult of(Status status, WorldTaskCraftAttempt attempt, String detail) {
            return new CraftResult(status, attempt, detail);
        }

        public boolean committed() {
            return status == Status.COMMITTED || status == Status.COMMITTED_CLEANUP_PENDING;
        }
    }
}
