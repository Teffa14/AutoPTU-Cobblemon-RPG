package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the server-authored identity of the currently focused visible WILD readable while the
 * player remains in interaction range. Initial acquisition and authored identity changes are
 * announced by {@link WildHabitatCueRuntime}; this runtime only repeats the same canonical context
 * at a quiet cadence. It never reads Cobblemon Pokemon gameplay state or supplies encounter/PTU
 * legality or outcomes.
 */
public final class WildInteractionFocusContextRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    static final int REPEAT_CONTEXT_INTERVAL_TICKS = 80;
    private static final Map<MinecraftServer, Map<UUID, FocusContextState>> FOCUSED_CONTEXT = new IdentityHashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS == 0) reconcile(server.getOverworld());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (FOCUSED_CONTEXT) {
                FOCUSED_CONTEXT.remove(server);
            }
        });
    }

    static void reconcile(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return;

        MinecraftServer server = world.getServer();
        long currentTick = server.getTicks();
        List<WildEcologyProjectionRegistry.ProjectedActor> projections = WildEcologyProjectionRegistry.collect(world);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUuid();
            online.add(playerId);
            if (player.isSpectator()) {
                remember(server, playerId, null);
                continue;
            }

            WildHabitatCueRuntime.NearbyInteractionSnapshot current =
                    WildHabitatCueRuntime.nearestInteractionActor(player, projections);
            FocusContextState previous = remembered(server, playerId);
            boolean repeatNow = shouldRepeat(previous, current, currentTick);
            remember(server, playerId, nextState(previous, current, currentTick, repeatNow));
            if (repeatNow) {
                player.sendMessage(Text.literal(WildHabitatCueRuntime.nearbyInteractionText(current)), true);
            }
        }
        forgetOffline(server, online);
    }

    static boolean shouldRepeat(
            FocusContextState previous,
            WildHabitatCueRuntime.NearbyInteractionSnapshot current,
            long currentTick) {
        if (previous == null || current == null || currentTick < 0L) return false;
        if (!current.equals(previous.snapshot())) return false;
        return currentTick - previous.lastContextCueTick() >= REPEAT_CONTEXT_INTERVAL_TICKS;
    }

    static FocusContextState nextState(
            FocusContextState previous,
            WildHabitatCueRuntime.NearbyInteractionSnapshot current,
            long currentTick,
            boolean repeatedNow) {
        if (current == null) return null;
        if (currentTick < 0L) throw new IllegalArgumentException("currentTick must not be negative");
        if (previous == null || !current.equals(previous.snapshot()) || repeatedNow) {
            return new FocusContextState(current, currentTick);
        }
        return previous;
    }

    private static FocusContextState remembered(MinecraftServer server, UUID playerId) {
        synchronized (FOCUSED_CONTEXT) {
            Map<UUID, FocusContextState> players = FOCUSED_CONTEXT.get(server);
            return players == null ? null : players.get(playerId);
        }
    }

    private static void remember(MinecraftServer server, UUID playerId, FocusContextState state) {
        synchronized (FOCUSED_CONTEXT) {
            Map<UUID, FocusContextState> players = FOCUSED_CONTEXT.computeIfAbsent(server, ignored -> new HashMap<>());
            if (state == null) players.remove(playerId);
            else players.put(playerId, state);
            if (players.isEmpty()) FOCUSED_CONTEXT.remove(server);
        }
    }

    private static void forgetOffline(MinecraftServer server, Set<UUID> online) {
        synchronized (FOCUSED_CONTEXT) {
            Map<UUID, FocusContextState> players = FOCUSED_CONTEXT.get(server);
            if (players == null) return;
            players.keySet().removeIf(playerId -> !online.contains(playerId));
            if (players.isEmpty()) FOCUSED_CONTEXT.remove(server);
        }
    }

    record FocusContextState(
            WildHabitatCueRuntime.NearbyInteractionSnapshot snapshot,
            long lastContextCueTick) {
        FocusContextState {
            if (snapshot == null) throw new IllegalArgumentException("snapshot is required");
            if (lastContextCueTick < 0L) throw new IllegalArgumentException("lastContextCueTick must not be negative");
        }
    }
}
