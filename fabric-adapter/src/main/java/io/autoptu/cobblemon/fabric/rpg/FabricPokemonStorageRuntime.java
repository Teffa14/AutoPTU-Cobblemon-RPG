package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalPokemonStorageQueryService;
import io.autoptu.cobblemon.authority.CanonicalPokemonStorageSummary;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

/** Minecraft fallback surface for durable server-owned boxed Pokemon. */
public final class FabricPokemonStorageRuntime {
    private FabricPokemonStorageRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("box")
                                .executes(context -> show(context.getSource())))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Pokemon storage must be requested by an authenticated player."));
            return 0;
        }
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPokemonStorageQueryService service = new CanonicalPokemonStorageQueryService(
                FabricCanonicalPlayerStoreRuntime.requirePokemonStorageRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer())
        );
        CanonicalPokemonStorageSummary storage;
        try {
            storage = service.inspect(playerId);
        } catch (IllegalStateException invalidState) {
            source.sendError(Text.literal("AutoPTU Pokemon storage is inconsistent and cannot be displayed safely."));
            return 0;
        }
        player.sendMessage(Text.literal("AutoPTU box — " + storage.members().size() + " stored Pokemon | rev " + storage.storageRevision()), false);
        if (storage.members().isEmpty()) {
            player.sendMessage(Text.literal("Box is empty."), false);
            return 1;
        }
        for (CanonicalPokemonStorageSummary.Member member : storage.members()) {
            player.sendMessage(Text.literal("[" + member.boxSlot() + "] " + displayName(member.speciesId())
                    + " Lv." + member.level() + " | " + member.pokemonId()), false);
        }
        return 1;
    }

    private static String displayName(String speciesId) {
        String path = speciesId.contains(":") ? speciesId.substring(speciesId.indexOf(':') + 1) : speciesId;
        if (path.isEmpty()) return speciesId;
        return path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
    }
}
