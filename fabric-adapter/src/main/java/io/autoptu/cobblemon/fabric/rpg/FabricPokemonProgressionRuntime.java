package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.autoptu.cobblemon.authority.CanonicalPokemonDetail;
import io.autoptu.cobblemon.authority.CanonicalPokemonDetailService;
import io.autoptu.cobblemon.authority.CanonicalPokemonProgressionQueryService;
import io.autoptu.cobblemon.authority.FileCanonicalPokemonProgressionRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

/** Authenticated read-only fallback for durable server-owned Pokemon progression. */
public final class FabricPokemonProgressionRuntime {
    private FabricPokemonProgressionRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("pokemon")
                                .then(CommandManager.literal("progression")
                                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(1))
                                                .executes(context -> show(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "slot")
                                                )))))));
    }

    private static int show(ServerCommandSource source, int slot) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Pokemon progression must be requested by an authenticated player."));
            return 0;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPokemonDetailService detailService = new CanonicalPokemonDetailService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer()));
        CanonicalPokemonDetail detail;
        try {
            detail = detailService.findPokemon(playerId, slot).orElse(null);
        } catch (IllegalStateException inconsistent) {
            source.sendError(Text.literal("Canonical Pokemon state is inconsistent."));
            return 0;
        }
        if (detail == null) {
            source.sendError(Text.literal("No canonical Pokemon exists in party slot " + slot + "."));
            return 0;
        }

        var service = new CanonicalPokemonProgressionQueryService(
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer()),
                new FileCanonicalPokemonProgressionRepository(canonicalStateRoot(player)));
        var snapshot = service.inspect(playerId, detail.pokemonId()).orElse(null);
        if (snapshot == null) {
            source.sendError(Text.literal("Pokemon progression is unavailable for this Trainer."));
            return 0;
        }
        String evolution = snapshot.pendingEvolutionChoiceId() == null
                ? "none"
                : snapshot.pendingEvolutionChoiceId();
        player.sendMessage(Text.literal("Pokemon progression — " + snapshot.speciesId()
                + " — Level " + snapshot.canonicalLevel()
                + " — XP " + snapshot.pokemonXp()
                + " — pending evolution " + evolution
                + " — progression revision " + snapshot.progressionRevision()), false);
        return 1;
    }

    private static Path canonicalStateRoot(ServerPlayerEntity player) {
        return player.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu")
                .resolve("canonical-state")
                .normalize();
    }
}
