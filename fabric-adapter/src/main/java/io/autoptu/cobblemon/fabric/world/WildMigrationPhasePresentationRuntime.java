package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.ecology.MigrationPhase;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * Projects explicitly authored WILD migration transitions into Minecraft-only ambient visuals.
 *
 * <p>The migration phase comes only from the server-owned ecology descriptor and its projection
 * profile. Cobblemon remains the presentation body; this runtime never reads Pokemon species,
 * level, stats, HP, moves, abilities, statuses, BattleState, encounter legality or PTU outcomes.</p>
 */
public final class WildMigrationPhasePresentationRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 20;

    enum PresentationStyle {
        GATHERING_CLOUD,
        DEPARTURE_POOF,
        ARRIVAL_SPARKLE,
        RETURN_TRAIL
    }

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
            if (actor.isRemoved() || actor.isInvisible()) continue;
            if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;

            var binding = VisibleWildPokemonEncounterRuntime.binding(actor.getUuid()).orElse(null);
            if (binding == null) continue;
            var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(binding.canonicalEncounterId()).orElse(null);
            if (encounter == null) continue;
            var population = CanonicalWildPopulationCatalogue.DEFAULT.population(encounter.populationId()).orElse(null);
            if (population == null) continue;
            var descriptor = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
            if (descriptor == null || !descriptor.worldEligibility().accepts(world)) continue;
            var phase = descriptor.projectionPhase(population, world.getTime()).orElse(null);
            if (!shouldProject(phase)) continue;

            world.spawnParticles(
                    particleEffect(presentationStyle(phase)),
                    actor.getX(),
                    actor.getY() + actor.getHeight() * 0.65D,
                    actor.getZ(),
                    particleCount(phase),
                    0.22D,
                    0.12D,
                    0.22D,
                    0.012D);
            projected++;
        }
        return projected;
    }

    static boolean shouldProject(MigrationPhase phase) {
        return phase == MigrationPhase.PREPARING
                || phase == MigrationPhase.DEPARTING
                || phase == MigrationPhase.ARRIVING
                || phase == MigrationPhase.RETURNING;
    }

    static PresentationStyle presentationStyle(MigrationPhase phase) {
        if (!shouldProject(phase)) {
            throw new IllegalArgumentException("migration phase does not have ambient presentation: " + phase);
        }
        return switch (phase) {
            case PREPARING -> PresentationStyle.GATHERING_CLOUD;
            case DEPARTING -> PresentationStyle.DEPARTURE_POOF;
            case ARRIVING -> PresentationStyle.ARRIVAL_SPARKLE;
            case RETURNING -> PresentationStyle.RETURN_TRAIL;
            default -> throw new IllegalArgumentException("migration phase does not have ambient presentation: " + phase);
        };
    }

    private static ParticleEffect particleEffect(PresentationStyle style) {
        return switch (style) {
            case GATHERING_CLOUD -> ParticleTypes.CLOUD;
            case DEPARTURE_POOF -> ParticleTypes.POOF;
            case ARRIVAL_SPARKLE -> ParticleTypes.HAPPY_VILLAGER;
            case RETURN_TRAIL -> ParticleTypes.END_ROD;
        };
    }

    static int particleCount(MigrationPhase phase) {
        if (!shouldProject(phase)) return 0;
        return phase == MigrationPhase.PREPARING ? 1 : 3;
    }
}
