package io.autoptu.cobblemon.authority;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Establishes one physical Ouros field camp exactly once from server-owned world identity and
 * persistent canonical Trainer capability state.
 *
 * <p>This is Ouros world simulation. It does not consume a PTU action/frequency, heal, grant a
 * reward, create an encounter, or mutate battle state.</p>
 */
public final class FieldCampSetupService {
    private final FileFieldCampSetupAttemptRepository attempts;
    private final WorldTaskCompetenceService competence;
    private final RollSource rollSource;

    public FieldCampSetupService(
            FileFieldCampSetupAttemptRepository attempts,
            WorldTaskCompetenceService competence,
            RollSource rollSource
    ) {
        this.attempts = Objects.requireNonNull(attempts, "attempts");
        this.competence = Objects.requireNonNull(competence, "competence");
        this.rollSource = Objects.requireNonNull(rollSource, "rollSource");
    }

    public FieldCampSetupService(FileFieldCampSetupAttemptRepository attempts) {
        this(attempts, new WorldTaskCompetenceService(),
                () -> ThreadLocalRandom.current().nextInt(1, 101));
    }

    public SetupResult establish(
            String attemptId,
            String campId,
            CanonicalPlayerState player,
            WorldTaskDefinition task
    ) {
        requireText(attemptId, "attemptId");
        requireText(campId, "campId");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(task, "task");

        FieldCampSetupAttempt attempt = attempts.find(attemptId).orElse(null);
        boolean createdNow = false;
        if (attempt == null) {
            WorldTaskCompetenceService.Assessment assessment = competence.assess(player, task);
            if (!assessment.understood()) {
                return SetupResult.rejected(Status.KNOWLEDGE_LOCKED, assessment.detail());
            }
            FieldCampSetupAttempt planned = FieldCampSetupAttempt.planned(
                    attemptId,
                    campId,
                    player.playerId(),
                    task.taskId(),
                    assessment.canonicalSkillRank(),
                    assessment.distribution()
            );
            createdNow = attempts.createIfAbsent(planned);
            attempt = attempts.find(attemptId).orElseThrow();
        }

        if (!attempt.campId().equals(campId) || !attempt.taskId().equals(task.taskId())) {
            return SetupResult.of(Status.ATTEMPT_CONFLICT, attempt,
                    "field camp attempt id is already bound to a different world task");
        }

        if (attempt.phase() == FieldCampSetupAttempt.Phase.COMMITTED) {
            return SetupResult.of(
                    createdNow ? Status.COMMITTED : Status.ALREADY_ESTABLISHED,
                    attempt,
                    createdNow ? "field camp established exactly once" : "field camp was already established"
            );
        }

        int roll = rollSource.rollPercent();
        if (roll < 1 || roll > 100) throw new IllegalStateException("roll source must return 1..100");
        FieldCampSetupAttempt.Quality quality = qualityFor(roll, attempt.frozenDistribution());
        FieldCampSetupAttempt committed = attempt.committed(roll, quality);
        attempts.replaceIfPhase(attemptId, FieldCampSetupAttempt.Phase.PLANNED, committed);
        FieldCampSetupAttempt current = attempts.find(attemptId).orElseThrow();
        if (current.phase() != FieldCampSetupAttempt.Phase.COMMITTED) {
            return SetupResult.of(Status.RECOVERY_REQUIRED, current,
                    "field camp setup remains planned; no result was exposed");
        }
        return SetupResult.of(
                Status.COMMITTED,
                current,
                "field camp established exactly once"
        );
    }

    private static FieldCampSetupAttempt.Quality qualityFor(
            int roll,
            WorldTaskDefinition.QualityDistribution distribution
    ) {
        if (roll <= distribution.improvisedPercent()) return FieldCampSetupAttempt.Quality.IMPROVISED;
        if (roll <= distribution.improvisedPercent() + distribution.standardPercent()) {
            return FieldCampSetupAttempt.Quality.STANDARD;
        }
        return FieldCampSetupAttempt.Quality.EXCELLENT;
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
        ALREADY_ESTABLISHED,
        KNOWLEDGE_LOCKED,
        ATTEMPT_CONFLICT,
        RECOVERY_REQUIRED
    }

    public record SetupResult(Status status, FieldCampSetupAttempt attempt, String detail) {
        public SetupResult {
            status = Objects.requireNonNull(status, "status");
            detail = detail == null ? "" : detail;
        }

        public static SetupResult rejected(Status status, String detail) {
            return new SetupResult(status, null, detail);
        }

        public static SetupResult of(Status status, FieldCampSetupAttempt attempt, String detail) {
            return new SetupResult(status, attempt, detail);
        }

        public boolean established() {
            return status == Status.COMMITTED || status == Status.ALREADY_ESTABLISHED;
        }
    }
}
