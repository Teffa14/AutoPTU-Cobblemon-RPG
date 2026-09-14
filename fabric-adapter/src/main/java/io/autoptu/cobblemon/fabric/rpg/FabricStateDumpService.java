package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartyQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartySummary;
import io.autoptu.cobblemon.authority.CanonicalTrainerSummaryService;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerProgressionRepository;
import io.autoptu.cobblemon.authority.FileCanonicalWalletRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Reusable read-only projection of canonical RPG state with persistent identifiers removed.
 *
 * <p>This service resolves the authenticated Minecraft UUID to server-owned canonical persistence,
 * then copies only operator-safe fields into redacted DTOs. It does not provision missing wallet or
 * progression state, repair persistence, read Cobblemon Pokemon gameplay state, or evaluate PTU
 * legality, RNG, damage, statuses, item effects, progression policy, action economy or outcomes.</p>
 */
public final class FabricStateDumpService {
    private FabricStateDumpService() {
    }

    public static DumpResult inspect(MinecraftServer server, ServerPlayerEntity target) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(target, "target");

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        String playerName = target.getGameProfile().getName();
        try {
            var playerRepository = FabricCanonicalPlayerStoreRuntime.requireRepository(server);
            if (playerRepository.findPlayer(playerId).isEmpty()) {
                return DumpResult.failure(
                        playerName,
                        "CANONICAL_TRAINER_MISSING",
                        "No canonical AutoPTU Trainer state exists for the authenticated player.");
            }

            CanonicalTrainerSummaryService.Summary trainer = new CanonicalTrainerSummaryService(playerRepository)
                    .find(playerId)
                    .orElseThrow(() -> new IllegalStateException("canonical Trainer disappeared during state dump"));
            CanonicalPartySummary party = new CanonicalPartyQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(server),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(server))
                    .findParty(playerId)
                    .orElse(null);
            CanonicalBagQueryService.BagSnapshot bag = new CanonicalBagQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireAssetRepository(server))
                    .inspect(playerId);
            FileCanonicalTrainerProgressionRepository.ProgressionState progression =
                    new FileCanonicalTrainerProgressionRepository(canonicalStateRoot(server))
                            .find(playerId)
                            .orElse(null);
            FileCanonicalWalletRepository.WalletState wallet =
                    FabricCanonicalPlayerStoreRuntime.requireWalletRepository(server)
                            .find(playerId)
                            .orElse(null);

            return DumpResult.success(
                    playerName,
                    trainerDump(trainer),
                    progressionDump(progression),
                    partyDump(party),
                    bagDump(bag),
                    walletDump(wallet));
        } catch (RuntimeException inconsistentState) {
            return DumpResult.failure(
                    playerName,
                    "REPOSITORY_CONSISTENCY_READ_FAILED",
                    safeMessage(inconsistentState));
        }
    }

    private static TrainerDump trainerDump(CanonicalTrainerSummaryService.Summary trainer) {
        return new TrainerDump(
                trainer.actionPoints(),
                trainer.initiativeModifier(),
                trainer.explicitInitiativeSpeed(),
                trainer.teamId(),
                List.copyOf(trainer.trainerClasses()),
                trainer.skills().stream()
                        .map(skill -> new SkillDump(skill.id(), skill.rank()))
                        .toList(),
                List.copyOf(trainer.trainerFeatures()),
                List.copyOf(trainer.availablePokemonCapabilities()),
                trainer.revision());
    }

    private static ProgressionDump progressionDump(
            FileCanonicalTrainerProgressionRepository.ProgressionState progression
    ) {
        return progression == null
                ? null
                : new ProgressionDump(
                        progression.trainerLevel(),
                        progression.trainerXp(),
                        progression.revision());
    }

    private static PartyDump partyDump(CanonicalPartySummary party) {
        if (party == null) return null;
        return new PartyDump(
                party.partyRevision(),
                party.members().stream()
                        .map(member -> new PartyMemberDump(
                                member.slot(),
                                member.speciesId(),
                                member.level(),
                                member.hasHealth() ? member.currentHp() : null,
                                member.hasHealth() ? member.maxHp() : null,
                                List.copyOf(member.statuses()),
                                member.pokemonRevision()))
                        .toList());
    }

    private static BagDump bagDump(CanonicalBagQueryService.BagSnapshot bag) {
        return new BagDump(
                bag.totalQuantity(),
                bag.totalAvailable(),
                bag.totalReserved(),
                bag.transactionLocks(),
                bag.entries().stream()
                        .map(entry -> new BagEntryDump(
                                entry.templateId(),
                                entry.quantity(),
                                entry.availableQuantity(),
                                entry.reservedQuantity(),
                                entry.transactionLocked(),
                                entry.reservationConsumed(),
                                entry.revision()))
                        .toList());
    }

    private static WalletDump walletDump(FileCanonicalWalletRepository.WalletState wallet) {
        return wallet == null
                ? null
                : new WalletDump(wallet.currencyId(), wallet.balance(), wallet.revision());
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

    public record DumpResult(
            String playerName,
            TrainerDump trainer,
            ProgressionDump progression,
            PartyDump party,
            BagDump bag,
            WalletDump wallet,
            String errorCode,
            String errorMessage
    ) {
        static DumpResult success(
                String playerName,
                TrainerDump trainer,
                ProgressionDump progression,
                PartyDump party,
                BagDump bag,
                WalletDump wallet
        ) {
            return new DumpResult(playerName, trainer, progression, party, bag, wallet, null, null);
        }

        static DumpResult failure(String playerName, String errorCode, String errorMessage) {
            return new DumpResult(playerName, null, null, null, null, null, errorCode, errorMessage);
        }

        public boolean success() {
            return errorCode == null;
        }
    }

    public record TrainerDump(
            int actionPoints,
            int initiativeModifier,
            Integer explicitInitiativeSpeed,
            String teamId,
            List<String> trainerClasses,
            List<SkillDump> skills,
            List<String> trainerFeatures,
            List<String> availablePokemonCapabilities,
            long revision
    ) {
    }

    public record SkillDump(String id, String rank) {
    }

    public record ProgressionDump(int trainerLevel, long trainerXp, long revision) {
    }

    public record PartyDump(long revision, List<PartyMemberDump> members) {
    }

    public record PartyMemberDump(
            int slot,
            String speciesId,
            int level,
            Integer currentHp,
            Integer maxHp,
            List<String> statuses,
            long pokemonRevision
    ) {
    }

    public record BagDump(
            int totalQuantity,
            int totalAvailable,
            int totalReserved,
            int transactionLocks,
            List<BagEntryDump> entries
    ) {
    }

    public record BagEntryDump(
            String templateId,
            int quantity,
            int availableQuantity,
            int reservedQuantity,
            boolean transactionLocked,
            boolean reservationConsumed,
            long revision
    ) {
    }

    public record WalletDump(String currencyId, long balance, long revision) {
    }
}
