package io.autoptu.cobblemon.fabric.rpg;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.autoptu.cobblemon.authority.CanonicalNurseryCustodyService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import io.autoptu.cobblemon.fabric.persistence.FabricNurseryStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

/** Minecraft fallback surface for persistent server-owned nursery custody. */
public final class FabricNurseryRuntime {
    private FabricNurseryRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("nursery")
                                .executes(context -> show(context.getSource()))
                                .then(CommandManager.literal("enroll")
                                        .then(CommandManager.argument("boxSlot", IntegerArgumentType.integer(1))
                                                .executes(context -> enroll(context.getSource(), IntegerArgumentType.getInteger(context, "boxSlot")))))
                                .then(CommandManager.literal("release")
                                        .then(CommandManager.argument("nurserySlot", IntegerArgumentType.integer(1))
                                                .executes(context -> release(context.getSource(), IntegerArgumentType.getInteger(context, "nurserySlot"))))))));
    }

    private static int show(ServerCommandSource source) {
        ServerPlayerEntity player = authenticated(source);
        if (player == null) return 0;
        try {
            CanonicalNurseryCustodyService.NurserySummary summary = service(player).inspect(
                    playerId(player), CanonicalNurseryCustodyService.CEDAR_NURSERY);
            player.sendMessage(Text.literal("Cedar Nursery — " + summary.members().size() + "/"
                    + CanonicalNurseryCustodyService.MAX_CUSTODY + " Pokemon in care | rev " + summary.revision()), false);
            if (summary.members().isEmpty()) {
                player.sendMessage(Text.literal("No Pokemon are currently in nursery custody."), false);
            } else {
                int slot = 1;
                for (CanonicalNurseryCustodyService.Member member : summary.members()) {
                    player.sendMessage(Text.literal("[" + slot++ + "] " + displayName(member.speciesId())
                            + " Lv." + member.level() + " | " + member.pokemonId()), false);
                }
            }
            return 1;
        } catch (IllegalArgumentException | IllegalStateException error) {
            source.sendError(Text.literal("AutoPTU nursery state cannot be displayed safely."));
            return 0;
        }
    }

    private static int enroll(ServerCommandSource source, int boxSlot) {
        ServerPlayerEntity player = authenticated(source);
        if (player == null) return 0;
        try {
            CanonicalNurseryCustodyService.NurserySummary summary = service(player).enrollFromBox(
                    playerId(player), CanonicalNurseryCustodyService.CEDAR_NURSERY, boxSlot);
            player.sendMessage(Text.literal("Pokemon entered Cedar Nursery custody. " + summary.members().size()
                    + "/" + CanonicalNurseryCustodyService.MAX_CUSTODY + " slots occupied."), false);
            return 1;
        } catch (IllegalArgumentException error) {
            source.sendError(Text.literal(error.getMessage()));
            return 0;
        } catch (IllegalStateException error) {
            source.sendError(Text.literal("AutoPTU could not safely place that Pokemon in nursery custody."));
            return 0;
        }
    }

    private static int release(ServerCommandSource source, int nurserySlot) {
        ServerPlayerEntity player = authenticated(source);
        if (player == null) return 0;
        try {
            CanonicalNurseryCustodyService.NurserySummary summary = service(player).releaseToBox(
                    playerId(player), CanonicalNurseryCustodyService.CEDAR_NURSERY, nurserySlot);
            player.sendMessage(Text.literal("Pokemon returned to canonical box storage. " + summary.members().size()
                    + " remain in nursery custody."), false);
            return 1;
        } catch (IllegalArgumentException error) {
            source.sendError(Text.literal(error.getMessage()));
            return 0;
        } catch (IllegalStateException error) {
            source.sendError(Text.literal("AutoPTU could not safely release that Pokemon from nursery custody."));
            return 0;
        }
    }

    private static ServerPlayerEntity authenticated(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) source.sendError(Text.literal("Nursery actions require an authenticated player."));
        return player;
    }

    private static CanonicalNurseryCustodyService service(ServerPlayerEntity player) {
        return new CanonicalNurseryCustodyService(
                FabricNurseryStoreRuntime.requireRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonStorageRepository(player.getServer()),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(player.getServer()));
    }

    private static String playerId(ServerPlayerEntity player) {
        return FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
    }

    private static String displayName(String speciesId) {
        String path = speciesId.contains(":") ? speciesId.substring(speciesId.indexOf(':') + 1) : speciesId;
        if (path.isEmpty()) return speciesId;
        return path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
    }
}