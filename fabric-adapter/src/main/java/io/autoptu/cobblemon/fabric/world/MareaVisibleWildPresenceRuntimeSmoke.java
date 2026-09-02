package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** Dedicated-server smoke proving that the normal WORLD-013 presence policy repairs a lost actor. */
public final class MareaVisibleWildPresenceRuntimeSmoke {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String ENABLE_PROPERTY = "autoptu.liveMareaWildPresenceSmoke";
    private static final Map<MinecraftServer, Probe> PROBES = new IdentityHashMap<>();

    private MareaVisibleWildPresenceRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var encounter = firstMareaEncounter();
            // Re-enter the same production reconciliation path to obtain the actor under test. This
            // avoids coupling the smoke to entity-index visibility timing immediately after startup;
            // the subsequent replacement still has to come from the periodic normal-world policy.
            PokemonEntity actor = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), encounter);
            if (actor == null || !VisibleWildPokemonEncounterRuntime.isBound(actor.getUuid())) {
                throw new IllegalStateException("Marea presence smoke requires the normal bound actor at startup");
            }
            UUID removedUuid = actor.getUuid();
            VisibleWildPokemonEncounterRuntime.unbind(removedUuid);
            actor.discard();
            synchronized (PROBES) {
                PROBES.put(server, new Probe(
                        encounter.canonicalEncounterId(),
                        removedUuid,
                        server.getTicks() + MareaVisibleWildPokemonRuntime.presenceReconcileIntervalTicks() * 3L
                ));
            }
            LOGGER.info("AutoPTU live Marea wild presence smoke removed actor {}", removedUuid);
        });
        ServerTickEvents.END_SERVER_TICK.register(MareaVisibleWildPresenceRuntimeSmoke::verify);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (PROBES) {
                PROBES.remove(server);
            }
        });
    }

    private static void verify(MinecraftServer server) {
        Probe probe;
        synchronized (PROBES) {
            probe = PROBES.get(server);
        }
        if (probe == null) return;

        PokemonEntity actor = MareaVisibleWildPokemonRuntime.actorForEncounter(
                server.getOverworld(), probe.canonicalEncounterId());
        if (actor != null
                && !actor.getUuid().equals(probe.removedUuid())
                && VisibleWildPokemonEncounterRuntime.isBound(actor.getUuid())) {
            synchronized (PROBES) {
                PROBES.remove(server);
            }
            LOGGER.info("AutoPTU live Marea wild presence reconciliation smoke passed: {} -> {}",
                    probe.removedUuid(), actor.getUuid());
            return;
        }

        if (server.getTicks() > probe.deadlineTick()) {
            synchronized (PROBES) {
                PROBES.remove(server);
            }
            throw new IllegalStateException("Marea presence reconciliation did not restore a bound actor before deadline");
        }
    }

    private static CanonicalWildEncounterCatalogue.EncounterDefinition firstMareaEncounter() {
        for (var encounter : CanonicalWildEncounterCatalogue.DEFAULT.encounters()) {
            if (encounter.siteId().startsWith("ouros.marea.")) return encounter;
        }
        throw new IllegalStateException("Marea presence smoke requires an authored canonical encounter");
    }

    private record Probe(String canonicalEncounterId, UUID removedUuid, long deadlineTick) {}
}
