package io.autoptu.cobblemon.fabric.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Registers the S2C codec used by the native Cobblemon Summary PTU projection. */
public final class FabricCanonicalPokemonSummaryNetworking {
    private static boolean registered;

    private FabricCanonicalPokemonSummaryNetworking() {}

    public static synchronized void registerPayloadType() {
        if (registered) return;
        PayloadTypeRegistry.playS2C().register(
                FabricCanonicalPokemonSummaryPayload.ID,
                FabricCanonicalPokemonSummaryPayload.CODEC);
        registered = true;
    }
}
