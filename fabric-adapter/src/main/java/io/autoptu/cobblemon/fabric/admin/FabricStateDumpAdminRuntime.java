package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.fabric.rpg.FabricStateDumpService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/** Operator-only command surface for a read-only, identifier-redacted canonical RPG state dump. */
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

        FabricStateDumpService.DumpResult result =
                FabricStateDumpService.inspect(source.getServer(), target);
        if (!result.success()) {
            source.sendError(Text.literal("Canonical state dump failed safely [" + result.errorCode()
                    + "]; persistent identifiers and repository details remain redacted."));
            return 0;
        }

        emit(source, result);
        return 1;
    }

    private static void emit(ServerCommandSource source, FabricStateDumpService.DumpResult result) {
        source.sendFeedback(() -> Text.literal("AutoPTU canonical state dump — " + result.playerName()), false);
        source.sendFeedback(() -> Text.literal(
                "Identity: authenticated online player; UUID and canonical persistent identifiers redacted."), false);

        FabricStateDumpService.TrainerDump trainer = result.trainer();
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

        FabricStateDumpService.ProgressionDump progression = result.progression();
        if (progression == null) {
            source.sendFeedback(() -> Text.literal("Progression: unavailable (no persisted record)"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Progression: level " + progression.trainerLevel()
                    + " | XP " + progression.trainerXp()
                    + " | revision " + progression.revision()), false);
        }

        FabricStateDumpService.PartyDump party = result.party();
        if (party == null) {
            source.sendFeedback(() -> Text.literal("Party: unavailable (no persisted party record)"), false);
        } else if (party.members().isEmpty()) {
            source.sendFeedback(() -> Text.literal("Party: empty | revision " + party.revision()), false);
        } else {
            source.sendFeedback(() -> Text.literal("Party: " + party.members().size()
                    + " member(s) | revision " + party.revision()), false);
            for (FabricStateDumpService.PartyMemberDump member : party.members()) {
                String hp = member.currentHp() == null ? "unavailable" : member.currentHp() + "/" + member.maxHp();
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

        FabricStateDumpService.BagDump bag = result.bag();
        source.sendFeedback(() -> Text.literal("Bag: " + bag.entries().size() + " stack(s)"
                + " | quantity " + bag.totalQuantity()
                + " | available " + bag.totalAvailable()
                + " | reserved " + bag.totalReserved()
                + " | locks " + bag.transactionLocks()), false);
        int stackIndex = 1;
        for (FabricStateDumpService.BagEntryDump entry : bag.entries()) {
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

        FabricStateDumpService.WalletDump wallet = result.wallet();
        if (wallet == null) {
            source.sendFeedback(() -> Text.literal(
                    "Wallet: unavailable (no persisted record; dump did not create one)"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Wallet: " + wallet.currencyId()
                    + " " + wallet.balance()
                    + " | revision " + wallet.revision()), false);
        }

        source.sendFeedback(() -> Text.literal(
                "Read-only redacted dump complete; no RPG/PTU state was mutated and no hidden identifiers were printed."), false);
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
}
