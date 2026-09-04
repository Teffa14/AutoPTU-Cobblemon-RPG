package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalPlayerEncounterProfile;
import io.autoptu.cobblemon.fabric.network.FabricBattleCameraNetworking;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minecraft camera framing for a server-bound AutoPTU battle.
 *
 * <p>This runtime does not infer combatants, legal tiles, range, movement, turn ownership or any
 * other PTU fact. It frames only the canonical server-owned arena anchor already persisted in the
 * encounter profile. Exact frozen arena dimensions are not present in this contract, so the normal
 * path deliberately sends a one-anchor presentation frame rather than inventing a PTU grid size.
 */
public final class FabricBattleCameraRuntime {
    private static final Set<UUID> AUTO_FRAMED = ConcurrentHashMap.newKeySet();
    private static int tickCounter;

    private FabricBattleCameraRuntime() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("autoptu")
                        .then(CommandManager.literal("battle")
                                .then(CommandManager.literal("camera")
                                        .executes(context -> frameCommand(context.getSource()))))));
        ServerTickEvents.END_SERVER_TICK.register(FabricBattleCameraRuntime::autoFrameBoundBattles);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> AUTO_FRAMED.remove(handler.player.getUuid()));
    }

    private static void autoFrameBoundBattles(MinecraftServer server) {
        if (++tickCounter < 10) return;
        tickCounter = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerUuid = player.getUuid();
            if (!FabricBattleChoiceRuntime.hasBinding(playerUuid)) {
                if (AUTO_FRAMED.remove(playerUuid)) FabricBattleCameraNetworking.clear(player);
                continue;
            }
            if (AUTO_FRAMED.contains(playerUuid)) continue;
            if (frame(player)) AUTO_FRAMED.add(playerUuid);
        }
    }

    private static int frameCommand(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Battle camera must be requested by an authenticated player."));
            return 0;
        }
        if (!FabricBattleChoiceRuntime.hasBinding(player.getUuid())) {
            source.sendError(Text.literal("No active authoritative AutoPTU battle is bound to this player."));
            return 0;
        }
        if (!frame(player)) {
            source.sendError(Text.literal("Battle camera framing is unavailable for the current server-owned arena."));
            return 0;
        }
        AUTO_FRAMED.add(player.getUuid());
        player.sendMessage(Text.literal("AutoPTU battle camera framed on the server-owned arena."), true);
        return 1;
    }

    private static boolean frame(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        String canonicalPlayerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        CanonicalPlayerEncounterProfile profile = FabricCanonicalPlayerStoreRuntime
                .requireEncounterProfileRepository(server)
                .findProfile(canonicalPlayerId)
                .orElse(null);
        if (profile == null || !profile.playerId().equals(canonicalPlayerId)) return false;

        BattleArenaSnapshot arena = profile.arena();
        String playerDimension = player.getServerWorld().getRegistryKey().getValue().toString();
        if (!arena.dimensionId().equals(playerDimension)) return false;

        String battleId = FabricBattleChoiceRuntime.spectateId(player.getUuid());
        if (battleId == null || battleId.isBlank()) return false;

        CameraFocus focus = focusPoint(arena);
        player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, new Vec3d(focus.x(), focus.y(), focus.z()));
        FabricBattleCameraNetworking.sendTacticalAerial(
                player,
                battleId,
                arena.originX(),
                arena.originY(),
                arena.originZ(),
                1,
                1
        );
        return true;
    }

    static CameraFocus focusPoint(BattleArenaSnapshot arena) {
        if (arena == null) throw new IllegalArgumentException("arena is required");
        return new CameraFocus(arena.originX() + 0.5D, arena.originY() + 1.0D, arena.originZ() + 0.5D);
    }

    record CameraFocus(double x, double y, double z) {}
}
