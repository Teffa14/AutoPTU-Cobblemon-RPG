package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.autoptu.cobblemon.authority.CanonicalPokemonNicknameService;
import io.autoptu.cobblemon.authority.FileCanonicalPokemonNicknameRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

/** Fallback command for durable server-authoritative Pokemon display nicknames. */
public final class FabricPokemonNicknameRuntime {
    private FabricPokemonNicknameRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("pokemon")
                                .then(CommandManager.literal("nickname")
                                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 6))
                                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                                        .executes(context -> setNickname(
                                                                context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "slot"),
                                                                StringArgumentType.getString(context, "name")
                                                        ))))))));
    }

    private static int setNickname(ServerCommandSource source, int slot, String requestedName) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || player.getServer() == null) {
            source.sendError(Text.literal("Pokemon nickname must be changed by an authenticated player."));
            return 0;
        }

        Path canonicalRoot = player.getServer().getSavePath(WorldSavePath.ROOT)
                .resolve("autoptu").resolve("canonical-state").normalize();
        CanonicalPokemonNicknameService service = new CanonicalPokemonNicknameService(
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer()),
                new FileCanonicalPokemonNicknameRepository(canonicalRoot)
        );
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPokemonNicknameService.Decision decision = service.setNickname(playerId, slot, requestedName);
        return switch (decision.outcome()) {
            case APPLIED -> {
                source.sendFeedback(() -> Text.literal("Pokemon in canonical party slot " + slot + " is now named " + decision.nickname() + "."), false);
                yield 1;
            }
            case ALREADY_SET -> {
                source.sendFeedback(() -> Text.literal("That Pokemon is already named " + decision.nickname() + "."), false);
                yield 1;
            }
            case INVALID_NAME, NO_PARTY, INVALID_SLOT, POKEMON_MISSING, NOT_OWNER, CONCURRENT_WRITE -> {
                source.sendError(Text.literal("Pokemon nickname rejected: " + decision.reason()));
                yield 0;
            }
        };
    }
}
