package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartyQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartySummary;
import io.autoptu.cobblemon.authority.CanonicalTrainerSummaryService;
import io.autoptu.cobblemon.authority.FileCanonicalTrainerProgressionRepository;
import io.autoptu.cobblemon.authority.FileCanonicalWalletRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.util.List;

/** Operator-only, read-only and identifier-redacted dump of canonical RPG state. */
public final class FabricStateDumpAdminRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("state")
                                        .then(CommandManager.literal("dump")
                                                .then(CommandManager.argument("player", StringArgumentType.word())
                                                        .executes(context -> dump(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "player")))))))));
    }

    private static int dump(ServerCommandSource source, String playerName) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("That Minecraft player must be online for canonical identity resolution."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        MinecraftServer server = source.getServer();
        try {
            var playerRepository = FabricCanonicalPlayerStoreRuntime.requireRepository(server);
            if (playerRepository.findPlayer(playerId).isEmpty()) {
                source.sendError(Text.literal("No canonical AutoPTU Trainer state exists for "
                        + target.getGameProfile().getName() + "."));
                return 0;
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

            emit(source, target.getGameProfile().getName(), trainer, progression, party, bag, wallet);
            return 1;
        } catch (RuntimeException inconsistentState) {
            source.sendError(Text.literal("Canonical state dump failed safely: " + safeMessage(inconsistentState)));
            return 0;
        }
    }

    private static void emit(
            ServerCommandSource source,
            String playerName,
            CanonicalTrainerSummaryService.Summary trainer,
            FileCanonicalTrainerProgressionRepository.ProgressionState progression,
            CanonicalPartySummary party,
            CanonicalBagQueryService.BagSnapshot bag,
            FileCanonicalWalletRepository.WalletState wallet
    ) {
        source.sendFeedback(() -> Text.literal("AutoPTU canonical state dump — " + playerName), false);
        source.sendFeedback(() -> Text.literal("Identity: authenticated online player; persistent identifiers redacted."), false);

        source.sendFeedback(() -> Text.literal("Trainer: AP " + trainer.actionPoints()
                + " | initiative " + signed(trainer.initiativeModifier())
                + " | explicit speed " + valueOrUnavailable(trainer.explicitInitiativeSpeed())
                + " | team " + valueOrNone(trainer.teamId())
                + " | revision " + trainer.revision()), false);
        source.sendFeedback(() -> Text.literal("Trainer classes: " + joinOrNone(trainer.trainerClasses())), false);
        source.sendFeedback(() -> Text.literal("Trainer skills: " + (trainer.skills().isEmpty()
                ? "none"
                : String.join(", ", trainer.skills().stream()
                        .map(skill -> skill.id() + "=" + skill.rank())
                        .toList()))), false);
        source.sendFeedback(() -> Text.literal("Trainer features: " + joinOrNone(trainer.trainerFeatures())), false);
        source.sendFeedback(() -> Text.literal("Pokemon capabilities: "
                + joinOrNone(trainer.availablePokemonCapabilities())), false);

        if (progression == null) {
            source.sendFeedback(() -> Text.literal("Progression: unavailable (no persisted record)"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Progression: level " + progression.trainerLevel()
                    + " | XP " + progression.trainerXp()
                    + " | revision " + progression.revision()), false);
        }

        if (party == null) {
            source.sendFeedback(() -> Text.literal("Party: unavailable (no persisted party record)"), false);
        } else if (party.members().isEmpty()) {
            source.sendFeedback(() -> Text.literal("Party: empty | revision " + party.partyRevision()), false);
        } else {
            source.sendFeedback(() -> Text.literal("Party: " + party.members().size()
                    + " member(s) | revision " + party.partyRevision()), false);
            for (CanonicalPartySummary.Member member : party.members()) {
                String hp = member.hasHealth() ? member.currentHp() + "/" + member.maxHp() : "unavailable";
                String statuses = member.statuses().isEmpty() ? "clear" : String.join(",", member.statuses());
                source.sendFeedback(() -> Text.literal("  slot " + member.slot()
                        + " | " + member.speciesId()
                        + " | Lv." + member.level()
                        + " | HP " + hp
                        + " | status " + statuses
                        + " | Pokemon revision " + member.pokemonRevision()
                        + " | Pokemon ID redacted"), false);
            }
        }

        source.sendFeedback(() -> Text.literal("Bag: " + bag.entries().size() + " stack(s)"
                + " | quantity " + bag.totalQuantity()
                + " | available " + bag.totalAvailable()
                + " | reserved " + bag.totalReserved()
                + " | locks " + bag.transactionLocks()), false);
        int stackIndex = 1;
        for (CanonicalBagQueryService.BagEntry entry : bag.entries()) {
            int outputIndex = stackIndex++;
            source.sendFeedback(() -> Text.literal("  stack " + outputIndex
                    + " | template " + entry.templateId()
                    + " | qty " + entry.quantity()
                    + " | available " + entry.availableQuantity()
                    + " | reserved " + entry.reservedQuantity()
                    + " | locked " + entry.transactionLocked()
                    + " | consumed " + entry.reservationConsumed()
                    + " | revision " + entry.revision()
                    + " | instance/reservation IDs redacted"), false);
        }

        if (wallet == null) {
            source.sendFeedback(() -> Text.literal("Wallet: unavailable (no persisted record; dump did not create one)"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Wallet: " + wallet.currencyId()
                    + " " + wallet.balance()
                    + " | revision " + wallet.revision()), false);
        }
        source.sendFeedback(() -> Text.literal(
                "Read-only redacted dump complete; no RPG/PTU state was mutated and no hidden identifiers were printed."), false);
    }

    private static Path canonicalStateRoot(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }

    private static String joinOrNone(List<String> values) {
        return values == null || values.isEmpty() ? "none" : String.join(", ", values);
    }

    private static String valueOrNone(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private static String valueOrUnavailable(Integer value) {
        return value == null ? "unavailable" : value.toString();
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
