package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * Projects explicitly server-authored wild social roles into Minecraft-only ambient visuals.
 *
 * <p>Alpha designation comes only from the registered ecology descriptor. For canonical projected
 * WILD actors the runtime mirrors that authored role into Cobblemon's synchronized entity-only
 * alpha presentation flag while deliberately leaving {@code Pokemon.isAlpha} untouched. That keeps
 * Cobblemon's persistent alpha gameplay, moveset, stats and herd AI outside the RPG authority path.
 * This runtime never reads species, level, stats, moves, abilities, HP, Cobblemon BattleState,
 * encounter priority or PTU mechanics.</p>
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

            boolean alpha = projection.socialRole() == WildSocialRole.ALPHA;
            projectNativeAlphaVisual(actor, alpha);
            if (!alpha || actor.isInvisible()) continue;

            world.spawnParticles(
                    ParticleTypes.END_ROD,
                    actor.getX(),
                    actor.getY() + actor.getHeight() + 0.35D,
                    actor.getZ(),
                    2,
                    0.18D,
                    0.08D,
                    0.18D,
                    0.005D);
            projected++;
        }
        return projected;
    }

    /**
     * Mirrors only the synchronized entity presentation bit. Never call Pokemon#setIsAlpha here:
     * that Cobblemon model property participates in persistent/gameplay alpha behavior.
     */
    static boolean projectNativeAlphaVisual(PokemonEntity actor, boolean alpha) {
        if (actor == null || actor.isRemoved()) return false;
        var alphaData = PokemonEntity.getIS_ALPHA();
        boolean current = actor.getDataTracker().get(alphaData);
        if (current == alpha) return false;
        actor.getDataTracker().set(alphaData, alpha);
        return true;
    }
}
