package io.autoptu.cobblemon.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Server-owned transport for presentation-only detached battle camera frames. */
public final class FabricBattleCameraNetworking {
    private static boolean payloadTypeRegistered;

    private FabricBattleCameraNetworking() {}

    public static synchronized void registerPayloadType() {
        if (payloadTypeRegistered) return;
        PayloadTypeRegistry.playS2C().register(FabricBattleCameraPayload.ID, FabricBattleCameraPayload.CODEC);
        payloadTypeRegistered = true;
    }

    public static void sendTacticalAerial(
            ServerPlayerEntity player,
            String battleId,
            int originX,
            int originY,
            int originZ,
            int width,
            int height
    ) {
        send(
                player,
                battleId,
                FabricBattleCameraMode.TACTICAL_AERIAL,
                originX,
                originY,
                originZ,
                width,
                height,
                originX + width / 2.0D,
                originY + 1.0D,
                originZ + height / 2.0D,
                225.0F
        );
    }

    public static void sendTrainerExternal(
            ServerPlayerEntity player,
            String battleId,
            int originX,
            int originY,
            int originZ,
            int width,
            int height
    ) {
        send(
                player,
                battleId,
                FabricBattleCameraMode.TRAINER_EXTERNAL,
                originX,
                originY,
                originZ,
                width,
                height,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                player.getYaw()
        );
    }

    public static void sendActionCinematic(
            ServerPlayerEntity player,
            String battleId,
            int originX,
            int originY,
            int originZ,
            int width,
            int height,
            Vec3d attacker,
            Vec3d target
    ) {
        if (attacker == null || target == null) throw new IllegalArgumentException("action camera points are required");
        double focusX = (attacker.x + target.x) * 0.5D;
        double focusY = Math.max(attacker.y, target.y) + 1.0D;
        double focusZ = (attacker.z + target.z) * 0.5D;
        double dx = target.x - attacker.x;
        double dz = target.z - attacker.z;
        float yaw = dx == 0.0D && dz == 0.0D
                ? 225.0F
                : (float) Math.toDegrees(Math.atan2(-dx, dz));
        send(
                player,
                battleId,
                FabricBattleCameraMode.ACTION_CINEMATIC,
                originX,
                originY,
                originZ,
                width,
                height,
                focusX,
                focusY,
                focusZ,
                yaw
        );
    }

    private static void send(
            ServerPlayerEntity player,
            String battleId,
            FabricBattleCameraMode mode,
            int originX,
            int originY,
            int originZ,
            int width,
            int height,
            double focusX,
            double focusY,
            double focusZ,
            float yaw
    ) {
        registerPayloadType();
        ServerPlayNetworking.send(player, new FabricBattleCameraPayload(
                true,
                battleId,
                mode,
                originX,
                originY,
                originZ,
                width,
                height,
                focusX,
                focusY,
                focusZ,
                yaw
        ));
    }

    public static void clear(ServerPlayerEntity player) {
        registerPayloadType();
        ServerPlayNetworking.send(player, FabricBattleCameraPayload.clear());
    }
}
