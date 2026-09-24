package io.autoptu.cobblemon.fabric.client;

import com.cobblemon.mod.common.client.gui.summary.Summary;
import io.autoptu.cobblemon.fabric.network.FabricCanonicalPokemonSummaryNetworking;
import io.autoptu.cobblemon.fabric.network.FabricCanonicalPokemonSummaryPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client-only cache for disposable Pokemon used by Cobblemon's native Summary screen. */
public final class FabricCanonicalPokemonSummaryClient implements ClientModInitializer {
    private static final Map<UUID, FabricCanonicalPokemonSummaryPayload.Projection> PROJECTIONS =
            new ConcurrentHashMap<>();
    private static final long UNOPENED_PROJECTION_TTL_NANOS = 10_000_000_000L;
    private static boolean nativeSummaryObserved;
    private static long projectionReceivedAtNanos;

    @Override
    public void onInitializeClient() {
        FabricCanonicalPokemonSummaryNetworking.registerPayloadType();
        ClientPlayNetworking.registerGlobalReceiver(
                FabricCanonicalPokemonSummaryPayload.ID,
                (payload, context) -> context.client().execute(() -> replace(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen instanceof Summary) {
                nativeSummaryObserved = true;
            } else if (nativeSummaryObserved || unopenedProjectionExpired()) {
                clear();
            }
        });
    }

    public static Optional<FabricCanonicalPokemonSummaryPayload.Projection> projection(UUID presentationPokemonId) {
        if (presentationPokemonId == null) return Optional.empty();
        return Optional.ofNullable(PROJECTIONS.get(presentationPokemonId));
    }

    static void replace(FabricCanonicalPokemonSummaryPayload payload) {
        PROJECTIONS.clear();
        for (FabricCanonicalPokemonSummaryPayload.Projection projection : payload.projections()) {
            PROJECTIONS.put(projection.presentationPokemonId(), projection);
        }
        nativeSummaryObserved = false;
        projectionReceivedAtNanos = System.nanoTime();
    }

    static void clear() {
        PROJECTIONS.clear();
        nativeSummaryObserved = false;
        projectionReceivedAtNanos = 0L;
    }

    private static boolean unopenedProjectionExpired() {
        return !PROJECTIONS.isEmpty()
                && projectionReceivedAtNanos != 0L
                && System.nanoTime() - projectionReceivedAtNanos >= UNOPENED_PROJECTION_TTL_NANOS;
    }
}
