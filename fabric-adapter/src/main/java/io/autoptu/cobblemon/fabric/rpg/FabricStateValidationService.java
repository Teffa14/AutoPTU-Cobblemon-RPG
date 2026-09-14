package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartyQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartySummary;
import io.autoptu.cobblemon.authority.CanonicalPlayerStateValidationService;
import io.autoptu.cobblemon.authority.CanonicalTrainerSummaryService;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerProgressionRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Reusable read-only structural validation boundary for one authenticated Minecraft player.
 *
 * <p>The service resolves canonical identity and server-owned RPG persistence only. It never repairs
 * state, reads Cobblemon Pokemon gameplay data, or evaluates PTU legality, combat outcomes, item
 * effects, progression thresholds, statuses, abilities, or action economy.</p>
 */
public final class FabricStateValidationService {
    private FabricStateValidationService() {
    }

    public static ValidationResult validate(MinecraftServer server, ServerPlayerEntity target) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(target, "target");

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        String playerName = target.getGameProfile().getName();
        try {
            var playerRepository = FabricCanonicalPlayerStoreRuntime.requireRepository(server);
            if (playerRepository.findPlayer(playerId).isEmpty()) {
                return ValidationResult.repositoryFailure(
                        playerName,
                        playerId,
                        "CANONICAL_TRAINER_MISSING",
                        "No canonical AutoPTU Trainer state exists for the authenticated player.");
            }

            CanonicalTrainerSummaryService.Summary trainer = new CanonicalTrainerSummaryService(playerRepository)
                    .find(playerId)
                    .orElseThrow(() -> new IllegalStateException("canonical Trainer disappeared during validation"));
            CanonicalPartySummary party = new CanonicalPartyQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(server),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(server))
                    .findParty(playerId)
                    .orElse(null);
            CanonicalBagQueryService.BagSnapshot bag = new CanonicalBagQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(server))
                    .inspect(playerId);
            Optional<FileCanonicalTrainerProgressionRepository.ProgressionState> progression =
                    new FileCanonicalTrainerProgressionRepository(canonicalStateRoot(server)).find(playerId);

            CanonicalPlayerStateValidationService.ValidationReport report =
                    new CanonicalPlayerStateValidationService().validate(playerId, trainer, party, bag, progression);
            return ValidationResult.fromReport(playerName, report);
        } catch (RuntimeException inconsistentState) {
            return ValidationResult.repositoryFailure(
                    playerName,
                    playerId,
                    "REPOSITORY_CONSISTENCY_READ_FAILED",
                    safeMessage(inconsistentState));
        }
    }

    private static Path canonicalStateRoot(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    public record ValidationResult(
            String playerName,
            String playerId,
            CanonicalPlayerStateValidationService.ValidationReport report,
            String repositoryErrorCode,
            String repositoryErrorMessage
    ) {
        static ValidationResult fromReport(
                String playerName,
                CanonicalPlayerStateValidationService.ValidationReport report
        ) {
            return new ValidationResult(playerName, report.playerId(), report, null, null);
        }

        static ValidationResult repositoryFailure(
                String playerName,
                String playerId,
                String code,
                String message
        ) {
            return new ValidationResult(playerName, playerId, null, code, message);
        }

        public boolean valid() {
            return repositoryErrorCode == null && report != null && report.valid();
        }

        public long warningCount() {
            return report == null ? 0L : report.warningCount();
        }
    }
}
