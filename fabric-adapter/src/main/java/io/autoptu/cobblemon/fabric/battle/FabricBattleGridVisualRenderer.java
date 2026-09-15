package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.battlecore.BattleChoiceVisualPlan;
import io.autoptu.cobblemon.battlecore.BattleGridCoordinate;
import io.autoptu.cobblemon.battlecore.BattleGridTransform;
import io.autoptu.cobblemon.fabric.network.FabricBattleGridPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import java.util.Set;

/** Publishes one compact visual frame; the client draws persistent lines and translucent cells. */
public final class FabricBattleGridVisualRenderer {
    private static boolean registered;
    private FabricBattleGridVisualRenderer() {}

    public static synchronized void register() {
        if (registered) return;
        PayloadTypeRegistry.playS2C().register(FabricBattleGridPayload.ID, FabricBattleGridPayload.CODEC);
        registered = true;
    }

    public static void render(ServerWorld world, BattleGridTransform transform, BattleChoiceVisualPlan plan,
                              boolean committed, long animationTick) {
        render(world, transform, plan, committed, animationTick, null);
    }

    public static void render(ServerWorld world, BattleGridTransform transform, BattleChoiceVisualPlan plan,
                              boolean committed, long animationTick, BattleGridCoordinate actorOrigin) {
        if (!world.getRegistryKey().getValue().toString().equals(transform.origin().dimensionId())) return;
        for (ServerPlayerEntity viewer : world.getPlayers()) {
            if (viewer.squaredDistanceTo(transform.origin().x(), transform.origin().y(), transform.origin().z()) <= 96 * 96) {
                render(viewer, transform, plan, committed, actorOrigin);
            }
        }
    }

    public static void render(ServerPlayerEntity viewer, BattleGridTransform transform, BattleChoiceVisualPlan plan,
                              boolean committed, BattleGridCoordinate actorOrigin) {
        if (viewer.getServerWorld().getRegistryKey().getValue().toString().equals(transform.origin().dimensionId())
                && ServerPlayNetworking.canSend(viewer, FabricBattleGridPayload.ID)) {
            ServerPlayNetworking.send(viewer, new FabricBattleGridPayload(transform.toArenaSnapshot(), plan, committed, actorOrigin));
        }
    }

    public static void clear(ServerWorld world, BattleGridTransform transform) {
        render(world, transform, new BattleChoiceVisualPlan(null, Set.of(), Set.of(), null), false, 0);
    }

}
