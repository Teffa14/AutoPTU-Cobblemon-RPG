package io.autoptu.cobblemon.fabric.network;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.world.ServerWorld;

/** Server-side presentation transport for authoritative PTU current HP. */
public final class FabricBattleHpPresentationNetworking {
    private static boolean payloadTypeRegistered;

    private FabricBattleHpPresentationNetworking() {}

    public static synchronized void registerPayloadType() {
        if (payloadTypeRegistered) return;
        PayloadTypeRegistry.playS2C().register(
                FabricBattleHpPresentationPayload.ID,
                FabricBattleHpPresentationPayload.CODEC);
        payloadTypeRegistered = true;
    }

    public static void broadcast(PokemonEntity entity, int currentHp) {
        if (!(entity.getWorld() instanceof ServerWorld world)) return;
        FabricBattleHpPresentationPayload payload =
                new FabricBattleHpPresentationPayload(entity.getUuid(), currentHp);
        for (var player : world.getPlayers()) {
            if (player.squaredDistanceTo(entity) <= 4096.0D
                    && ServerPlayNetworking.canSend(player, FabricBattleHpPresentationPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}
