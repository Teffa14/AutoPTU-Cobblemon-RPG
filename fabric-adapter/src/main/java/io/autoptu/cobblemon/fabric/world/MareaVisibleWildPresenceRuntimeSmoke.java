package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Dedicated-server smoke for normal WORLD-013 habitat population presence and replacement cleanup. */
public final class MareaVisibleWildPresenceRuntimeSmoke {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String ENABLE_PROPERTY = "autoptu.liveMareaWildPresenceSmoke";
    private static final int MAX_RECONCILIATION_CHECKS = 5;
    private static final Map<MinecraftServer, Probe> PROBES = new IdentityHashMap<>();

    private MareaVisibleWildPresenceRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            int projected = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld());
            var encounters = CanonicalWildPopulationCatalogue.DEFAULT.populations().stream()
                    .filter(population -> population.siteId().startsWith("ouros.marea."))
                    .flatMap(population -> CanonicalWildPopulationCatalogue.DEFAULT.members(population).stream())
                    .toList();
            if (projected < 6 || encounters.size() != 6) {
                throw new IllegalStateException("Marea population smoke requires six normal visible wild actors");
            }

            LinkedHashMap<String, PokemonEntity> actors = new LinkedHashMap<>();
            for (var encounter : encounters) {
                PokemonEntity actor = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld(), encounter);
                if (actor == null || !hasExactBinding(actor, encounter.canonicalEncounterId())) {
                    throw new IllegalStateException("Marea population smoke requires exact canonical actor binding for "
                            + encounter.canonicalEncounterId());
                }
                actors.put(encounter.canonicalEncounterId(), actor);
            }
            if (Set.copyOf(actors.values().stream().map(PokemonEntity::getUuid).toList()).size() != encounters.size()) {
                throw new IllegalStateException("Marea population smoke requires six distinct canonical actors");
            }

            var replacedEntry = actors.entrySet().iterator().next();
            String replacedEncounterId = replacedEntry.getKey();
            UUID removedUuid = replacedEntry.getValue().getUuid();
            LinkedHashMap<String, UUID> stableBindings = new LinkedHashMap<>();
            actors.forEach((encounterId, actor) -> {
                if (!encounterId.equals(replacedEncounterId)) stableBindings.put(encounterId, actor.getUuid());
            });
            replacedEntry.getValue().discard();
            int interval = MareaVisibleWildPokemonRuntime.presenceReconcileIntervalTicks();
            long nextCheckTick = nextReconciliationObservationTick(server.getTicks(), interval);
            synchronized (PROBES) {
                PROBES.put(server, new Probe(
                        replacedEncounterId,
                        removedUuid,
                        Map.copyOf(stableBindings),
                        nextCheckTick,
                        0
                ));
            }
            LOGGER.info("AutoPTU live Marea population-policy smoke discarded {} while preserving five canonical bindings",
                    removedUuid);
        });
        ServerTickEvents.END_SERVER_TICK.register(MareaVisibleWildPresenceRuntimeSmoke::verify);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { synchronized (PROBES) { PROBES.remove(server); } });
    }

    private static void verify(MinecraftServer server) {
        Probe probe;
        synchronized (PROBES) { probe = PROBES.get(server); }
        if (probe == null || server.getTicks() < probe.nextCheckTick()) return;

        var replacementUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(probe.replacedEncounterId());
        if (replacementUuid.isPresent()
                && !replacementUuid.get().equals(probe.removedUuid())
                && !VisibleWildPokemonEncounterRuntime.isBound(probe.removedUuid())
                && stableBindingsPreserved(probe)
                && allBindingsDistinct(probe, replacementUuid.get())) {
            synchronized (PROBES) { PROBES.remove(server); }
            LOGGER.info("AutoPTU live Marea population policy reconciliation smoke passed: {} -> {}, five stable bindings preserved",
                    probe.removedUuid(), replacementUuid.get());
            LOGGER.info("AutoPTU live Marea wild presence reconciliation smoke passed");
            return;
        }

        int completedChecks = probe.completedChecks() + 1;
        if (completedChecks >= MAX_RECONCILIATION_CHECKS) {
            synchronized (PROBES) { PROBES.remove(server); }
            throw new IllegalStateException("Marea population policy reconciliation did not restore one member while preserving the other five bindings");
        }
        synchronized (PROBES) {
            PROBES.put(server, new Probe(
                    probe.replacedEncounterId(),
                    probe.removedUuid(),
                    probe.stableBindings(),
                    probe.nextCheckTick() + MareaVisibleWildPokemonRuntime.presenceReconcileIntervalTicks(),
                    completedChecks
            ));
        }
    }

    private static long nextReconciliationObservationTick(long currentTick, int interval) {
        if (interval <= 0) throw new IllegalArgumentException("interval must be positive");
        return ((currentTick / interval) + 1L) * interval + 1L;
    }

    private static boolean stableBindingsPreserved(Probe probe) {
        for (var stable : probe.stableBindings().entrySet()) {
            var current = VisibleWildPokemonEncounterRuntime.boundEntityUuid(stable.getKey());
            if (current.isEmpty() || !current.get().equals(stable.getValue())) return false;
            if (!VisibleWildPokemonEncounterRuntime.binding(stable.getValue())
                    .map(binding -> stable.getKey().equals(binding.canonicalEncounterId()))
                    .orElse(false)) return false;
        }
        return true;
    }

    private static boolean allBindingsDistinct(Probe probe, UUID replacementUuid) {
        var uuids = new java.util.HashSet<UUID>();
        uuids.add(replacementUuid);
        uuids.addAll(probe.stableBindings().values());
        return uuids.size() == probe.stableBindings().size() + 1;
    }

    private static boolean hasExactBinding(PokemonEntity actor, String canonicalEncounterId) {
        return actor != null && VisibleWildPokemonEncounterRuntime.binding(actor.getUuid())
                .map(binding -> canonicalEncounterId.equals(binding.canonicalEncounterId()))
                .orElse(false);
    }

    private record Probe(
            String replacedEncounterId,
            UUID removedUuid,
            Map<String, UUID> stableBindings,
            long nextCheckTick,
            int completedChecks
    ) {}
}
