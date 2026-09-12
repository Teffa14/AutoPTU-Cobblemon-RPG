package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartyQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartySummary;
import io.autoptu.cobblemon.authority.CanonicalTrainerProgressionQueryService;
import io.autoptu.cobblemon.authority.CanonicalTrainerSummaryService;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerProgressionRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

/** Operator-only, read-only inspection of one player's persistent canonical RPG state. */
public final class FabricPlayerInspectionAdminRuntime {
    private FabricPlayerInspectionAdminRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("player")
                                        .then(CommandManager.literal("inspect")
                                                .then(CommandManager.argument("player", StringArgumentType.word())
                                                        .executes(context -> inspect(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "player")))))))));
    }

    private static int inspect(ServerCommandSource source, String playerName) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        var playerRepository = FabricCanonicalPlayerStoreRuntime.requireRepository(source.getServer());
        if (playerRepository.findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("No canonical AutoPTU Trainer state exists for " + target.getGameProfile().getName() + "."));
            return 0;
        }

        CanonicalTrainerSummaryService.Summary trainer = new CanonicalTrainerSummaryService(playerRepository)
                .find(playerId)
                .orElseThrow(() -> new IllegalStateException("canonical Trainer disappeared during read-only inspection"));

        CanonicalPartySummary party;
        try {
            party = new CanonicalPartyQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(source.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(source.getServer()))
                    .findParty(playerId)
                    .orElse(null);
        } catch (IllegalStateException inconsistentParty) {
            source.sendError(Text.literal("Canonical party state is inconsistent and cannot be inspected safely."));
            return 0;
        }

        CanonicalBagQueryService.BagSnapshot bag = new CanonicalBagQueryService(
                FabricCanonicalPlayerStoreRuntime.requireAssetRepository(source.getServer()))
                .inspect(playerId);

        var progression = new CanonicalTrainerProgressionQueryService(
                new FileCanonicalTrainerProgressionRepository(canonicalStateRoot(source)))
                .inspect(playerId);

        source.sendFeedback(() -> Text.literal("AutoPTU player inspection — " + target.getGameProfile().getName()), false);
        source.sendFeedback(() -> Text.literal("Canonical player: " + playerId + " | UUID " + target.getUuidAsString()), false);
        source.sendFeedback(() -> Text.literal("Trainer: AP " + trainer.actionPoints()
                + " | initiative " + signed(trainer.initiativeModifier())
                + " | classes " + (trainer.trainerClasses().isEmpty() ? "none" : String.join(", ", trainer.trainerClasses()))
                + " | revision " + trainer.revision()), false);
        source.sendFeedback(() -> Text.literal("Progression: level " + progression.trainerLevel()
                + " | XP " + progression.trainerXp()
                + " | revision " + progression.revision()), false);

        if (party == null || party.members().isEmpty()) {
            source.sendFeedback(() -> Text.literal("Party: empty"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Party: " + party.members().size() + " member(s)"), false);
            for (CanonicalPartySummary.Member member : party.members()) {
                String statuses = member.statuses().isEmpty() ? "clear" : String.join(",", member.statuses());
                String hp = member.hasHealth() ? member.currentHp() + "/" + member.maxHp() : "unavailable";
                source.sendFeedback(() -> Text.literal("  [" + member.slot() + "] " + member.speciesId()
                        + " | Lv." + member.level() + " | HP " + hp + " | status " + statuses), false);
            }
        }

        source.sendFeedback(() -> Text.literal("Bag: " + bag.entries().size() + " stack(s)"
                + " | quantity " + bag.totalQuantity()
                + " | available " + bag.totalAvailable()
                + " | reserved " + bag.totalReserved()
                + " | locks " + bag.transactionLocks()), false);
        source.sendFeedback(() -> Text.literal("Read-only canonical inspection complete; no RPG or PTU state was mutated."), false);
        return 1;
    }

    private static Path canonicalStateRoot(ServerCommandSource source) {
        return source.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }
}
