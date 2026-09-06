package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Projects server-authored Alpha presentation onto Cobblemon 1.8 visible WILD actors.
 *
 * <p>This runtime writes only Cobblemon's synchronized entity presentation flag. It deliberately
 * does not mutate the server-side Pokemon {@code isAlpha} property, level, moves, stats, abilities,
 * aggression, herd memory or battle state. Native Cobblemon Alpha rules therefore remain outside
 * AutoPTU's RPG authority boundary.</p>
 */
public final class WildAlphaPresentationRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 20;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
            update(server);
        });
    }

    static void update(MinecraftServer server) {
        if (server == null) return;
        for (var world : server.getWorlds()) {
            for (var projection : WildEcologyProjectionRegistry.collect(world)) {
                applyVisualState(projection.actor(), projection.presentationProfile());
            }
        }
    }

    static void applyVisualState(PokemonEntity actor, WildPresentationProfile profile) {
        if (actor == null) throw new IllegalArgumentException("actor is required");
        if (profile == null) throw new IllegalArgumentException("presentation profile is required");
        actor.getDataTracker().set(PokemonEntity.getIS_ALPHA(), profile.alphaVisual());
    }

    static boolean projectedAlphaVisual(PokemonEntity actor) {
        if (actor == null) throw new IllegalArgumentException("actor is required");
        return actor.getDataTracker().get(PokemonEntity.getIS_ALPHA());
    }
}
