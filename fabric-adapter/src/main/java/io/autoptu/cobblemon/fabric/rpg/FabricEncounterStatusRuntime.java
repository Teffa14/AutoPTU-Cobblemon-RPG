package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPlayerEncounterProfile;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;

/** `/autoptu encounter status` read-only projection of server-owned encounter preparation state. */
public final class FabricEncounterStatusRuntime {
    private FabricEncounterStatusRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("encounter")
                                .then(CommandManager.literal("status")
                                        .executes(context -> show(context.getSource()))))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || player.getServer() == null) {
            source.sendError(Text.literal("Encounter status must be requested by an authenticated player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(player.getServer()).findPlayer(playerId).isEmpty()) {
            source.sendError(Text.literal("No persistent canonical Trainer is configured."));
            return 0;
        }

        CanonicalPlayerEncounterProfile profile = FabricCanonicalPlayerStoreRuntime
                .requireEncounterProfileRepository(player.getServer())
                .findProfile(playerId)
                .orElse(null);
        if (profile == null || !profile.playerId().equals(playerId)) {
            source.sendError(Text.literal("No persistent canonical encounter profile is configured."));
            return 0;
        }

        player.sendMessage(Text.literal("AutoPTU encounter status"), false);
        player.sendMessage(Text.literal("Party: " + formatParty(profile)), false);
        player.sendMessage(Text.literal("Consumables: " + formatConsumables(profile.consumableQuantities())), false);
        player.sendMessage(Text.literal("Arena: " + profile.arena()), false);
        player.sendMessage(Text.literal(
                "Reservation/start readiness: authoritative battle reservation state is not exposed by this read surface"
        ), false);
        player.sendMessage(Text.literal(
                "Encounter legality, combatants and outcomes remain server-owned by the canonical battle authority"
        ), false);
        return 1;
    }

    private static String formatParty(CanonicalPlayerEncounterProfile profile) {
        return profile.pokemonIds().isEmpty() ? "empty" : String.join(", ", profile.pokemonIds());
    }

    private static String formatConsumables(Map<String, Integer> consumables) {
        if (consumables.isEmpty()) return "none";
        return consumables.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + " x" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
    }
}
