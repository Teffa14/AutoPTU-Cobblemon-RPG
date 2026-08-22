package io.autoptu.cobblemon.fabric.network;

import io.autoptu.cobblemon.battlecore.BattleAuthoritativeChoiceExecutor;
import io.autoptu.cobblemon.battlecore.BattleAuthoritativeLegalChoiceSource;
import io.autoptu.cobblemon.battlecore.BattleAuthoritativePreparationSource;
import io.autoptu.cobblemon.battlecore.BattleServerActionPacketHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Objects;

/**
 * Fabric-only C2S registration. The authenticated principal comes exclusively from the Fabric
 * connection context; no client field can replace it.
 */
public final class FabricBattleActionNetworking {
    private FabricBattleActionNetworking() {}

    public static void register(
            BattleAuthoritativePreparationSource preparationSource,
            BattleAuthoritativeLegalChoiceSource legalChoiceSource,
            BattleAuthoritativeChoiceExecutor executor
    ) {
        Objects.requireNonNull(preparationSource, "preparationSource");
        Objects.requireNonNull(legalChoiceSource, "legalChoiceSource");
        Objects.requireNonNull(executor, "executor");

        PayloadTypeRegistry.playC2S().register(FabricBattleActionPayload.ID, FabricBattleActionPayload.CODEC);
        boolean registered = ServerPlayNetworking.registerGlobalReceiver(
                FabricBattleActionPayload.ID,
                (payload, context) -> BattleServerActionPacketHandler.handle(
                        context.player().getUuid().toString(),
                        payload.packet(),
                        preparationSource,
                        legalChoiceSource,
                        executor));
        if (!registered) {
            throw new IllegalStateException("battle action C2S receiver is already registered");
        }
    }
}
