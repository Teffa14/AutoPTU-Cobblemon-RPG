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

/** Dedicated-server smoke for normal WORLD-013 habitat population presence and replacement cleanup. */
public final class MareaVisibleWildPresenceRuntimeSmoke {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String ENABLE_PROPERTY = "autoptu.liveMareaWildPresenceSmoke";
    private static final Map<MinecraftServer, Probe> PROBES = new IdentityHashMap<>();

    private MareaVisibleWildPresenceRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            int projected = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld());
            if (projected < 3) {
                throw new IllegalStateException("Marea population smoke requires at least three normal visible wild actors");
            }

            var firstEncounter = CanonicalWildEncounterCatalogue.DEFAULT
                    .encounter(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID).orElseThrow();
            var secondEncounter = CanonicalWildEncounterCatalogue.DEFAULT
                    .encounter(CanonicalWildEncounterCatalogue.MAREA_SECOND_FLETCHLING_ID).orElseThrow();
            var crossingEncounter = CanonicalWildEncounterCatalogue.DEFAULT
                    .encounter(CanonicalWildEncounterCatalogue.MAREA_CROSSING_FLETCHLING_ID).orElseThrow();
            if (firstEncounter.populationId().equals(crossingEncounter.populationId())) {
                throw new IllegalStateException("Marea smoke requires the crossing actor to belong to a separate canonical population");
            }

            PokemonEntity firstActor = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), firstEncounter);
            PokemonEntity secondActor = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), secondEncounter);
            PokemonEntity crossingActor = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), crossingEncounter);
            if (firstActor == null || secondActor == null || crossingActor == null
                    || firstActor.getUuid().equals(secondActor.getUuid())
                    || firstActor.getUuid().equals(crossingActor.getUuid())
                    || secondActor.getUuid().equals(crossingActor.getUuid())
                    || !VisibleWildPokemonEncounterRuntime.isBound(firstActor.getUuid())
                    || !VisibleWildPokemonEncounterRuntime.isBound(secondActor.getUuid())
                    || !VisibleWildPokemonEncounterRuntime.isBound(crossingActor.getUuid())) {
                throw new IllegalStateException("Marea population smoke requires three independently bound canonical actors");
            }

            UUID originalFirstUuid = firstActor.getUuid();
            UUID stableSecondUuid = secondActor.getUuid();
            UUID stableCrossingUuid = crossingActor.getUuid();
            firstActor.requestTeleport(firstActor.getX() + 20.0D, firstActor.getY(), firstActor.getZ() + 20.0D);
            PokemonEntity reused = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), firstEncounter);
            if (reused == null || !originalFirstUuid.equals(reused.getUuid())) {
                throw new IllegalStateException("Marea presence reconciliation duplicated a roaming population member");
            }

            // Deliberately discard one lower-shelf member without unbinding. Production reconciliation
            // must evict only that stale UUID while preserving the sibling and the separate crossing population.
            firstActor.discard();
            synchronized (PROBES) {
                PROBES.put(server, new Probe(
                        firstEncounter.canonicalEncounterId(),
                        secondEncounter.canonicalEncounterId(),
                        crossingEncounter.canonicalEncounterId(),
                        originalFirstUuid,
                        stableSecondUuid,
                        stableCrossingUuid,
                        server.getTicks() + MareaVisibleWildPokemonRuntime.presenceReconcileIntervalTicks() * 3L
                ));
            }
            LOGGER.info("AutoPTU live Marea wild population smoke discarded lower-shelf member {} while preserving sibling {} and crossing population {}",
                    originalFirstUuid, stableSecondUuid, stableCrossingUuid);
        });
        ServerTickEvents.END_SERVER_TICK.register(MareaVisibleWildPresenceRuntimeSmoke::verify);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (PROBES) { PROBES.remove(server); }
        });
    }

    private static void verify(MinecraftServer server) {
        Probe probe;
        synchronized (PROBES) { probe = PROBES.get(server); }
        if (probe == null) return;

        PokemonEntity replacement = MareaVisibleWildPokemonRuntime.actorForEncounter(
                server.getOverworld(), probe.replacedEncounterId());
        PokemonEntity stable = MareaVisibleWildPokemonRuntime.actorForEncounter(
                server.getOverworld(), probe.stableEncounterId());
        PokemonEntity crossing = MareaVisibleWildPokemonRuntime.actorForEncounter(
                server.getOverworld(), probe.crossingEncounterId());
        if (replacement != null && stable != null && crossing != null
                && !replacement.getUuid().equals(probe.removedUuid())
                && stable.getUuid().equals(probe.stableUuid())
                && crossing.getUuid().equals(probe.crossingUuid())
                && !replacement.getUuid().equals(stable.getUuid())
                && !replacement.getUuid().equals(crossing.getUuid())
                && VisibleWildPokemonEncounterRuntime.isBound(replacement.getUuid())
                && VisibleWildPokemonEncounterRuntime.isBound(stable.getUuid())
                && VisibleWildPokemonEncounterRuntime.isBound(crossing.getUuid())
                && !VisibleWildPokemonEncounterRuntime.isBound(probe.removedUuid())) {
            synchronized (PROBES) { PROBES.remove(server); }
            LOGGER.info("AutoPTU live Marea multi-population reconciliation smoke passed: lower shelf {} -> {}, sibling {}, crossing {}",
                    probe.removedUuid(), replacement.getUuid(), stable.getUuid(), crossing.getUuid());
            // Preserve the historical gate marker while the stronger multi-population smoke remains
            // the source of truth for the behavior validated above.
            LOGGER.info("AutoPTU live Marea wild presence reconciliation smoke passed");
            return;
        }

        if (server.getTicks() > probe.deadlineTick()) {
            synchronized (PROBES) { PROBES.remove(server); }
            throw new IllegalStateException("Marea population reconciliation did not restore one lower-shelf member while preserving sibling and crossing population");
        }
    }

    private record Probe(
            String replacedEncounterId,
            String stableEncounterId,
            String crossingEncounterId,
            UUID removedUuid,
            UUID stableUuid,
            UUID crossingUuid,
            long deadlineTick
    ) {}
}
