package io.autoptu.cobblemon.fabric.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalPartyQueryService;
import io.autoptu.cobblemon.authority.CanonicalPartySummary;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Operator-only, read-only inspection of the persistent canonical party aggregate. */
public final class FabricPartyInspectionAdminRuntime {
    private FabricPartyInspectionAdminRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("party")
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

        CanonicalPartySummary party;
        try {
            party = new CanonicalPartyQueryService(
                    FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(source.getServer()),
                    FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(source.getServer()))
                    .findParty(playerId)
                    .orElse(null);
        } catch (RuntimeException inconsistentParty) {
            source.sendError(Text.literal("Canonical party state is inconsistent and cannot be inspected safely: "
                    + safeMessage(inconsistentParty)));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("AutoPTU party inspection — " + target.getGameProfile().getName()), false);
        source.sendFeedback(() -> Text.literal("Canonical player: " + playerId + " | UUID " + target.getUuidAsString()), false);
        if (party == null) {
            source.sendFeedback(() -> Text.literal("Party: unavailable (no persisted encounter profile)"), false);
            source.sendFeedback(() -> Text.literal("Read-only canonical party inspection complete; no RPG or PTU state was mutated."), false);
            return 1;
        }

        source.sendFeedback(() -> Text.literal("Party revision: " + party.partyRevision()
                + " | members " + party.members().size()), false);
        if (party.members().isEmpty()) {
            source.sendFeedback(() -> Text.literal("Party: empty"), false);
        } else {
            for (CanonicalPartySummary.Member member : party.members()) {
                String statuses = member.statuses().isEmpty() ? "clear" : String.join(",", member.statuses());
                String hp = member.hasHealth() ? member.currentHp() + "/" + member.maxHp() : "unavailable";
                source.sendFeedback(() -> Text.literal("  [" + member.slot() + "] " + member.pokemonId()
                        + " | " + member.speciesId()
                        + " | Lv." + member.level()
                        + " | HP " + hp
                        + " | status " + statuses
                        + " | Pokemon revision " + member.pokemonRevision()), false);
            }
        }

        source.sendFeedback(() -> Text.literal(
                "Read-only canonical party inspection complete; Cobblemon gameplay data and PTU legality were not consulted."), false);
        return 1;
    }

    private static String safeMessage(RuntimeException error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
