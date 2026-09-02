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
            if (projected < 2) {
                throw new IllegalStateException("Marea population smoke requires at least two normal visible wild actors");
            }

            var firstEncounter = CanonicalWildEncounterCatalogue.DEFAULT
                    .encounter(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID).orElseThrow();
            var secondEncounter = CanonicalWildEncounterCatalogue.DEFAULT
                    .encounter(CanonicalWildEncounterCatalogue.MAREA_SECOND_FLETCHLING_ID).orElseThrow();
            PokemonEntity firstActor = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), firstEncounter);
            PokemonEntity secondActor = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), secondEncounter);
            if (firstActor == null || secondActor == null
                    || firstActor.getUuid().equals(secondActor.getUuid())
                    || !VisibleWildPokemonEncounterRuntime.isBound(firstActor.getUuid())
                    || !VisibleWildPokemonEncounterRuntime.isBound(secondActor.getUuid())) {
                throw new IllegalStateException("Marea population smoke requires two independently bound canonical actors");
            }

            UUID originalFirstUuid = firstActor.getUuid();
            UUID stableSecondUuid = secondActor.getUuid();
            firstActor.requestTeleport(firstActor.getX() + 20.0D, firstActor.getY(), firstActor.getZ() + 20.0D);
            PokemonEntity reused = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), firstEncounter);
            if (reused == null || !originalFirstUuid.equals(reused.getUuid())) {
                throw new IllegalStateException("Marea presence reconciliation duplicated a roaming population member");
            }

            // Deliberately discard one population member without unbinding. Production reconciliation
            // must evict only that stale UUID and preserve the independently-authored second member.
            firstActor.discard();
            synchronized (PROBES) {
                PROBES.put(server, new Probe(
                        firstEncounter.canonicalEncounterId(),
                        secondEncounter.canonicalEncounterId(),
                        originalFirstUuid,
                        stableSecondUuid,
                        server.getTicks() + MareaVisibleWildPokemonRuntime.presenceReconcileIntervalTicks() * 3L
                ));
            }
            LOGGER.info("AutoPTU live Marea wild population smoke discarded member {} while preserving {}",
                    originalFirstUuid, stableSecondUuid);
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
        if (replacement != null && stable != null
                && !replacement.getUuid().equals(probe.removedUuid())
                && stable.getUuid().equals(probe.stableUuid())
                && !replacement.getUuid().equals(stable.getUuid())
                && VisibleWildPokemonEncounterRuntime.isBound(replacement.getUuid())
                && VisibleWildPokemonEncounterRuntime.isBound(stable.getUuid())
                && !VisibleWildPokemonEncounterRuntime.isBound(probe.removedUuid())) {
            synchronized (PROBES) { PROBES.remove(server); }
            LOGGER.info("AutoPTU live Marea wild presence reconciliation smoke passed: population member {} -> {}, stable {}",
                    probe.removedUuid(), replacement.getUuid(), stable.getUuid());
            return;
        }

        if (server.getTicks() > probe.deadlineTick()) {
            synchronized (PROBES) { PROBES.remove(server); }
            throw new IllegalStateException("Marea population reconciliation did not restore one member while preserving the other");
        }
    }

    private record Probe(
            String replacedEncounterId,
            String stableEncounterId,
            UUID removedUuid,
            UUID stableUuid,
            long deadlineTick
    ) {}
}
