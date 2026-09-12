package io.autoptu.cobblemon.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;

/** Server-to-client transport for presentation-only AutoPTU battle HUD state. */
public final class FabricBattleHudNetworking {
    private static boolean payloadTypeRegistered;

    private FabricBattleHudNetworking() {}

    public static synchronized void registerPayloadType() {
        if (payloadTypeRegistered) return;
        PayloadTypeRegistry.playS2C().register(FabricBattleHudPayload.ID, FabricBattleHudPayload.CODEC);
        payloadTypeRegistered = true;
    }

    public static void send(ServerPlayerEntity player, FabricBattleHudPayload payload) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(payload, "payload");
        registerPayloadType();
        if (ServerPlayNetworking.canSend(player, FabricBattleHudPayload.ID)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void clear(ServerPlayerEntity player) {
        if (player != null) send(player, FabricBattleHudPayload.hidden());
    }
}
