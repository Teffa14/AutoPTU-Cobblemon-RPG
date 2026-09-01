package io.autoptu.cobblemon.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/** Server-owned transport for presentation-only tactical camera frames. */
public final class FabricBattleCameraNetworking {
    private static boolean payloadTypeRegistered;

    private FabricBattleCameraNetworking() {}

    public static synchronized void registerPayloadType() {
        if (payloadTypeRegistered) return;
        PayloadTypeRegistry.playS2C().register(FabricBattleCameraPayload.ID, FabricBattleCameraPayload.CODEC);
        payloadTypeRegistered = true;
    }

    public static void sendFrame(
            ServerPlayerEntity player,
            String battleId,
            int originX,
            int originY,
            int originZ,
            int width,
            int height
    ) {
        registerPayloadType();
        ServerPlayNetworking.send(player, new FabricBattleCameraPayload(
                true, battleId, originX, originY, originZ, width, height));
    }

    public static void clear(ServerPlayerEntity player) {
        registerPayloadType();
        ServerPlayNetworking.send(player, FabricBattleCameraPayload.clear());
    }
}
