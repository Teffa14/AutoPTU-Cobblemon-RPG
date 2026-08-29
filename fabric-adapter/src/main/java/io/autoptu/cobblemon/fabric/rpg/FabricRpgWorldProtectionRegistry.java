package io.autoptu.cobblemon.fabric.rpg;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Explicit temporary protection scopes for authored RPG interactions.
 *
 * Minecraft remains fully mutable outside registered scopes. Battle, quest or scripted world
 * systems may register only the region whose mutation would invalidate that interaction, then
 * remove it as soon as the interaction ends. The registry carries no PTU legality or outcome.
 */
public final class FabricRpgWorldProtectionRegistry {
    private static final Map<String, ProtectedRegion> REGIONS = new ConcurrentHashMap<>();

    private FabricRpgWorldProtectionRegistry() {}

    public static void protect(
            String scopeId,
            RegistryKey<World> dimension,
            BlockPos first,
            BlockPos second,
            String reason
    ) {
        if (scopeId == null || scopeId.isBlank()) throw new IllegalArgumentException("scopeId is required");
        if (dimension == null) throw new IllegalArgumentException("dimension is required");
        if (first == null || second == null) throw new IllegalArgumentException("region bounds are required");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason is required");

        BlockPos min = new BlockPos(
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ())
        );
        REGIONS.put(scopeId, new ProtectedRegion(scopeId, dimension, min, max, reason));
    }

    public static void clear(String scopeId) {
        if (scopeId != null) REGIONS.remove(scopeId);
    }

    public static Optional<ProtectedRegion> protectionAt(World world, BlockPos pos) {
        if (world == null || pos == null) return Optional.empty();
        return REGIONS.values().stream()
                .filter(region -> region.dimension().equals(world.getRegistryKey()))
                .filter(region -> region.contains(pos))
                .findFirst();
    }

    public record ProtectedRegion(
            String scopeId,
            RegistryKey<World> dimension,
            BlockPos min,
            BlockPos max,
            String reason
    ) {
        public boolean contains(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }
    }
}
