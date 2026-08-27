package io.autoptu.cobblemon.fabric.persistence;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Objects;

/** Reads Minecraft Overworld time only as the presentation world's RPG day clock input. */
public final class FabricRpgDayClock {
    static final long TICKS_PER_DAY = 24_000L;

    private FabricRpgDayClock() {}

    public static long observedOverworldDay(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        ServerWorld overworld = Objects.requireNonNull(
                server.getWorld(World.OVERWORLD),
                "Minecraft Overworld is unavailable"
        );
        return dayIndex(overworld.getTimeOfDay());
    }

    static long dayIndex(long timeOfDay) {
        return Math.max(0L, Math.floorDiv(timeOfDay, TICKS_PER_DAY));
    }
}
