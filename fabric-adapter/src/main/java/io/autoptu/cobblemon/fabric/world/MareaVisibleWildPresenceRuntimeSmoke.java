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
            if (projected < 4) throw new IllegalStateException("Marea population smoke requires four normal visible wild actors");

            var first = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID).orElseThrow();
            var second = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_SECOND_FLETCHLING_ID).orElseThrow();
            var crossing = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_CROSSING_FLETCHLING_ID).orElseThrow();
            var crossingSecond = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_CROSSING_SECOND_FLETCHLING_ID).orElseThrow();

            PokemonEntity firstActor = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), first);
            PokemonEntity secondActor = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), second);
            PokemonEntity crossingActor = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), crossing);
            PokemonEntity crossingSecondActor = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), crossingSecond);
            if (firstActor == null || secondActor == null || crossingActor == null || crossingSecondActor == null
                    || !VisibleWildPokemonEncounterRuntime.isBound(firstActor.getUuid())
                    || !VisibleWildPokemonEncounterRuntime.isBound(secondActor.getUuid())
                    || !VisibleWildPokemonEncounterRuntime.isBound(crossingActor.getUuid())
                    || !VisibleWildPokemonEncounterRuntime.isBound(crossingSecondActor.getUuid())) {
                throw new IllegalStateException("Marea population smoke requires four independently bound canonical actors");
            }

            UUID originalFirstUuid = firstActor.getUuid();
            UUID stableSecondUuid = secondActor.getUuid();
            UUID stableCrossingUuid = crossingActor.getUuid();
            UUID stableCrossingSecondUuid = crossingSecondActor.getUuid();
            firstActor.discard();
            synchronized (PROBES) {
                PROBES.put(server, new Probe(first.canonicalEncounterId(), second.canonicalEncounterId(), crossing.canonicalEncounterId(),
                        crossingSecond.canonicalEncounterId(), originalFirstUuid, stableSecondUuid, stableCrossingUuid,
                        stableCrossingSecondUuid, server.getTicks() + MareaVisibleWildPokemonRuntime.presenceReconcileIntervalTicks() * 3L));
            }
            LOGGER.info("AutoPTU live Marea population-policy smoke discarded {} while preserving {}, {}, {}",
                    originalFirstUuid, stableSecondUuid, stableCrossingUuid, stableCrossingSecondUuid);
        });
        ServerTickEvents.END_SERVER_TICK.register(MareaVisibleWildPresenceRuntimeSmoke::verify);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { synchronized (PROBES) { PROBES.remove(server); } });
    }

    private static void verify(MinecraftServer server) {
        Probe probe;
        synchronized (PROBES) { probe = PROBES.get(server); }
        if (probe == null) return;
        PokemonEntity replacement = MareaVisibleWildPokemonRuntime.actorForEncounter(server.getOverworld(), probe.replacedEncounterId());
        PokemonEntity stable = MareaVisibleWildPokemonRuntime.actorForEncounter(server.getOverworld(), probe.stableEncounterId());
        PokemonEntity crossing = MareaVisibleWildPokemonRuntime.actorForEncounter(server.getOverworld(), probe.crossingEncounterId());
        PokemonEntity crossingSecond = MareaVisibleWildPokemonRuntime.actorForEncounter(server.getOverworld(), probe.crossingSecondEncounterId());
        if (replacement != null && stable != null && crossing != null && crossingSecond != null
                && !replacement.getUuid().equals(probe.removedUuid())
                && stable.getUuid().equals(probe.stableUuid())
                && crossing.getUuid().equals(probe.crossingUuid())
                && crossingSecond.getUuid().equals(probe.crossingSecondUuid())
                && VisibleWildPokemonEncounterRuntime.isBound(replacement.getUuid())
                && !VisibleWildPokemonEncounterRuntime.isBound(probe.removedUuid())) {
            synchronized (PROBES) { PROBES.remove(server); }
            LOGGER.info("AutoPTU live Marea population policy reconciliation smoke passed: {} -> {}, stable {}, crossing {}, crossing sibling {}",
                    probe.removedUuid(), replacement.getUuid(), stable.getUuid(), crossing.getUuid(), crossingSecond.getUuid());
            LOGGER.info("AutoPTU live Marea wild presence reconciliation smoke passed");
            return;
        }
        if (server.getTicks() > probe.deadlineTick()) {
            synchronized (PROBES) { PROBES.remove(server); }
            throw new IllegalStateException("Marea population policy reconciliation did not restore one member while preserving the other three");
        }
    }

    private record Probe(String replacedEncounterId, String stableEncounterId, String crossingEncounterId,
                         String crossingSecondEncounterId, UUID removedUuid, UUID stableUuid, UUID crossingUuid,
                         UUID crossingSecondUuid, long deadlineTick) {}
}
