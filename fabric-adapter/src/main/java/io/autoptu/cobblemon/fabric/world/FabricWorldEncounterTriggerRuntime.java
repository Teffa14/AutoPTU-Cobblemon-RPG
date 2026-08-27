package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.fabric.battle.WorldEncounterTriggerRequestService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minecraft movement trigger for the first world-driven wild encounter request.
 *
 * The trigger reads only server-owned Minecraft position/block context plus authenticated player
 * identity. It never consults Cobblemon Pokemon, BattleState, HP, moves, statuses, battle actors or
 * results. A request stops at the P0-007 boundary; later stages must resolve canonical party and wild
 * blueprints before any battle can start.
 */
public final class FabricWorldEncounterTriggerRuntime {
    public static final String ZONE_ID = "overworld_wilds";
    public static final String CONTEXT_ID = "grass_walk";
    private static final long MIN_TRIGGER_INTERVAL_TICKS = 100L;

    private static final WorldEncounterTriggerRequestService REQUESTS = new WorldEncounterTriggerRequestService();
    private static final Map<UUID, BlockPos> LAST_POSITION = new HashMap<>();
    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new HashMap<>();

    private FabricWorldEncounterTriggerRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long tick = server.getTicks();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                observe(player, tick);
            }
        });
    }

    static void observe(ServerPlayerEntity player, long serverTick) {
        BlockPos current = player.getBlockPos();
        BlockPos previous = LAST_POSITION.put(player.getUuid(), current.toImmutable());
        if (previous != null && previous.equals(current)) return;
        if (!player.getServerWorld().getBlockState(current.down()).isOf(Blocks.GRASS_BLOCK)) return;

        long lastTrigger = LAST_TRIGGER_TICK.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2L);
        if (serverTick - lastTrigger < MIN_TRIGGER_INTERVAL_TICKS) return;

        String canonicalPlayerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        String dimensionId = player.getServerWorld().getRegistryKey().getValue().toString();
        WorldEncounterTriggerRequestService.Decision decision = REQUESTS.request(
                canonicalPlayerId,
                ZONE_ID,
                CONTEXT_ID,
                dimensionId,
                current.getX(),
                current.getY(),
                current.getZ(),
                serverTick
        );
        if (decision.outcome() == WorldEncounterTriggerRequestService.Outcome.CREATED) {
            LAST_TRIGGER_TICK.put(player.getUuid(), serverTick);
            player.sendMessage(Text.literal("A wild encounter is stirring nearby..."), true);
        }
    }

    public static WorldEncounterTriggerRequestService requests() {
        return REQUESTS;
    }
}
