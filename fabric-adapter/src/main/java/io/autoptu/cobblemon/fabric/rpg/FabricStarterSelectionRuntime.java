package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalStarterSelectionDecision;
import io.autoptu.cobblemon.authority.CanonicalStarterSelectionService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Minecraft entrypoint for a one-time, server-authoritative persistent starter choice. */
public final class FabricStarterSelectionRuntime {
    private FabricStarterSelectionRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("starter")
                                .then(CommandManager.literal("bulbasaur")
                                        .executes(context -> choose(context.getSource(), "bulbasaur")))
                                .then(CommandManager.literal("charmander")
                                        .executes(context -> choose(context.getSource(), "charmander")))
                                .then(CommandManager.literal("squirtle")
                                        .executes(context -> choose(context.getSource(), "squirtle"))))));
    }

    private static int choose(ServerCommandSource source, String speciesId) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Starter selection must be requested by a player."));
            return 0;
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        BlockPos pos = player.getBlockPos();
        BattleArenaSnapshot arena = new BattleArenaSnapshot(
                player.getServerWorld().getRegistryKey().getValue().toString(),
                pos.getX(), pos.getY(), pos.getZ(),
                1, 0,
                0, 1
        );
        CanonicalStarterSelectionService service = new CanonicalStarterSelectionService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );
        CanonicalStarterSelectionDecision decision = service.choose(playerId, speciesId, arena);
        if (decision.chosen()) {
            player.sendMessage(Text.literal(
                    "AutoPTU starter chosen: " + displayName(decision.speciesId())
                            + ". Your canonical party is now persistent in this world."), false);
            return 1;
        }
        if (decision.outcome() == CanonicalStarterSelectionDecision.Outcome.ALREADY_CHOSEN) {
            source.sendError(Text.literal("You already have an AutoPTU party"
                    + (decision.speciesId().isBlank() ? "." : ": " + displayName(decision.speciesId()) + ".")));
            return 0;
        }
        source.sendError(Text.literal("Starter selection failed: " + decision.detail()));
        return 0;
    }

    private static String displayName(String speciesId) {
        if (speciesId == null || speciesId.isBlank()) return "Pokemon";
        return Character.toUpperCase(speciesId.charAt(0)) + speciesId.substring(1);
    }
}
