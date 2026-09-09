package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * Projects explicitly server-authored wild presentation capabilities into Minecraft-only visuals.
 *
 * <p>The ecology descriptor must explicitly request native Alpha presentation and herd-leader presentation.
 * The runtime mirrors only that request into Cobblemon's synchronized entity-only alpha presentation flag while
 * deliberately leaving {@code Pokemon.isAlpha} untouched. Cobblemon persistent alpha gameplay, alpha movesets,
 * stats and herd AI therefore remain outside RPG authority.</p>
 */
public final class WildSocialRolePresentationRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 20;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % UPDATE_INTERVAL_TICKS != 0L) return;
            project(world);
        });
    }

    static int project(ServerWorld world) {
        if (world == null) return 0;
        int projected = 0;
        for (var projection : WildEcologyProjectionRegistry.collect(world)) {
            var actor = projection.actor();
            if (actor.isRemoved()) continue;
            if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;

            var capabilities = projection.presentationCapabilities();
            projectNativeAlphaVisual(actor, capabilities.nativeAlphaVisual());
            if (!capabilities.herdLeaderPresentation() || actor.isInvisible()) continue;

            world.spawnParticles(
                    ParticleTypes.END_ROD,
                    actor.getX(), actor.getY() + actor.getHeight() + 0.35D, actor.getZ(),
                    2, 0.18D, 0.08D, 0.18D, 0.005D);
            projected++;
        }
        return projected;
    }

    /** Mirrors only Cobblemon's synchronized entity presentation bit, never Pokemon#setIsAlpha. */
    static boolean projectNativeAlphaVisual(PokemonEntity actor, boolean alpha) {
        if (actor == null || actor.isRemoved()) return false;
        var alphaData = PokemonEntity.getIS_ALPHA();
        boolean current = actor.getDataTracker().get(alphaData);
        if (current == alpha) return false;
        actor.getDataTracker().set(alphaData, alpha);
        return true;
    }
}
