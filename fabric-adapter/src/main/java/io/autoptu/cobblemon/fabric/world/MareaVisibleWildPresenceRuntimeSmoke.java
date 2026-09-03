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
    private static final Map<MinecraftServer, Probe> PROBES = new IdentityHashMap<>();

    private MareaVisibleWildPresenceRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            verifyAuthoredPresenceFootprints();
            verifyAuthoredRoamingFootprints();
            int proximityProjected = MareaVisibleWildPokemonRuntime.reconcileActivePopulations(server.getOverworld());
            if (proximityProjected != 0) {
                throw new IllegalStateException("Marea presence policy must keep authored habitats dormant without players");
            }
            LOGGER.info("AutoPTU live Marea authored habitat-policy smoke verified four presence and roaming footprints plus dormant habitats without players");

            int projected = MareaVisibleWildPokemonRuntime.ensureProjected(server.getOverworld());
            var encounters = CanonicalWildPopulationCatalogue.DEFAULT.populations().stream()
                    .filter(population -> population.siteId().startsWith("ouros.marea."))
                    .flatMap(population -> CanonicalWildPopulationCatalogue.DEFAULT.members(population).stream())
                    .toList();
            if (projected != 8 || encounters.size() != 8) {
                throw new IllegalStateException("Marea population smoke requires eight normal visible wild actors");
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
                throw new IllegalStateException("Marea population smoke requires eight distinct canonical actors");
            }

            verifyCrossingRoamingEnforcement(server, actors);

            var replacedEntry = actors.entrySet().iterator().next();
            String replacedEncounterId = replacedEntry.getKey();
            UUID removedUuid = replacedEntry.getValue().getUuid();
            LinkedHashMap<String, UUID> stableBindings = new LinkedHashMap<>();
            actors.forEach((encounterId, actor) -> {
                if (!encounterId.equals(replacedEncounterId)) stableBindings.put(encounterId, actor.getUuid());
            });
            replacedEntry.getValue().discard();
            synchronized (PROBES) {
                PROBES.put(server, new Probe(
                        replacedEncounterId,
                        removedUuid,
                        Map.copyOf(stableBindings),
                        server.getTicks() + MareaVisibleWildPokemonRuntime.presenceReconcileIntervalTicks() * 3L
                ));
            }
            LOGGER.info("AutoPTU live Marea population-policy smoke discarded {} while preserving seven canonical bindings",
                    removedUuid);
        });
        ServerTickEvents.END_SERVER_TICK.register(MareaVisibleWildPresenceRuntimeSmoke::verify);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { synchronized (PROBES) { PROBES.remove(server); } });
    }

    private static void verifyAuthoredPresenceFootprints() {
        var catalogue = CanonicalWildPopulationCatalogue.DEFAULT;
        assertPresenceFootprint(catalogue, CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID, 48, 24, 56);
        assertPresenceFootprint(catalogue, CanonicalWildPopulationCatalogue.MAREA_CROSSING_POPULATION_ID, 40, 24, 40);
        assertPresenceFootprint(catalogue, CanonicalWildPopulationCatalogue.MAREA_MIRADOR_TRANSECT_POPULATION_ID, 48, 28, 48);
        assertPresenceFootprint(catalogue, CanonicalWildPopulationCatalogue.MAREA_LOMA_WINDBREAK_POPULATION_ID, 40, 24, 44);
    }

    private static void verifyAuthoredRoamingFootprints() {
        var catalogue = CanonicalWildPopulationCatalogue.DEFAULT;
        assertRoamingFootprint(catalogue, CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID, 28, 10, 34);
        assertRoamingFootprint(catalogue, CanonicalWildPopulationCatalogue.MAREA_CROSSING_POPULATION_ID, 20, 8, 20);
        assertRoamingFootprint(catalogue, CanonicalWildPopulationCatalogue.MAREA_MIRADOR_TRANSECT_POPULATION_ID, 26, 12, 26);
        assertRoamingFootprint(catalogue, CanonicalWildPopulationCatalogue.MAREA_LOMA_WINDBREAK_POPULATION_ID, 22, 10, 26);
    }

    private static void assertPresenceFootprint(
            CanonicalWildPopulationCatalogue catalogue,
            String populationId,
            int x,
            int y,
            int z
    ) {
        var population = catalogue.population(populationId)
                .orElseThrow(() -> new IllegalStateException("missing Marea population policy: " + populationId));
        var footprint = population.presenceFootprint();
        if (footprint.halfExtentXBlocks() != x || footprint.halfExtentYBlocks() != y || footprint.halfExtentZBlocks() != z) {
            throw new IllegalStateException("unexpected authored Marea presence footprint for " + populationId);
        }
        if (!footprint.containsOffset(x, y, z) || footprint.containsOffset(x + 1.0D, 0.0D, 0.0D)) {
            throw new IllegalStateException("Marea presence footprint boundary semantics failed for " + populationId);
        }
    }

    private static void assertRoamingFootprint(
            CanonicalWildPopulationCatalogue catalogue,
            String populationId,
            int x,
            int y,
            int z
    ) {
        var population = catalogue.population(populationId)
                .orElseThrow(() -> new IllegalStateException("missing Marea population policy: " + populationId));
        var footprint = population.roamingFootprint();
        if (footprint.halfExtentXBlocks() != x || footprint.halfExtentYBlocks() != y || footprint.halfExtentZBlocks() != z) {
            throw new IllegalStateException("unexpected authored Marea roaming footprint for " + populationId);
        }
        if (!footprint.containsOffset(x, y, z) || footprint.containsOffset(x + 1.0D, 0.0D, 0.0D)) {
            throw new IllegalStateException("Marea roaming footprint boundary semantics failed for " + populationId);
        }
    }

    private static void verifyCrossingRoamingEnforcement(
            MinecraftServer server,
            Map<String, PokemonEntity> actors
    ) {
        var crossing = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_CROSSING_POPULATION_ID)
                .orElseThrow(() -> new IllegalStateException("missing Marea crossing population"));
        var encounter = CanonicalWildPopulationCatalogue.DEFAULT.members(crossing).getFirst();
        PokemonEntity actor = actors.get(encounter.canonicalEncounterId());
        if (actor == null) throw new IllegalStateException("missing Marea crossing actor for roaming smoke");
        UUID originalUuid = actor.getUuid();
        double anchorX = actor.getX();
        double anchorY = actor.getY();
        double anchorZ = actor.getZ();

        // Crossing authors a 20-block X roaming half-extent. Moving 25 blocks would have remained
        // inside the old global 32-block leash; the authored policy must return it to its encounter anchor.
        actor.requestTeleport(anchorX + 25.0D, anchorY, anchorZ);
        PokemonEntity reconciled = MareaVisibleWildPokemonRuntime.actorForEncounter(
                server.getOverworld(), encounter.canonicalEncounterId());
        if (reconciled == null
                || !reconciled.getUuid().equals(originalUuid)
                || Math.abs(reconciled.getX() - anchorX) > 0.01D
                || Math.abs(reconciled.getY() - anchorY) > 0.01D
                || Math.abs(reconciled.getZ() - anchorZ) > 0.01D
                || !hasExactBinding(reconciled, encounter.canonicalEncounterId())) {
            throw new IllegalStateException("Marea authored roaming policy did not return the crossing actor without changing identity");
        }
        LOGGER.info("AutoPTU live Marea authored roaming enforcement smoke passed for crossing population: entity={}", originalUuid);
    }

    private static void verify(MinecraftServer server) {
        Probe probe;
        synchronized (PROBES) { probe = PROBES.get(server); }
        if (probe == null) return;

        PokemonEntity replacement = MareaVisibleWildPokemonRuntime.actorForEncounter(
                server.getOverworld(), probe.replacedEncounterId());
        if (replacement != null
                && !replacement.getUuid().equals(probe.removedUuid())
                && hasExactBinding(replacement, probe.replacedEncounterId())
                && !VisibleWildPokemonEncounterRuntime.isBound(probe.removedUuid())
                && stableBindingsPreserved(probe)
                && allBindingsDistinct(probe, replacement.getUuid())) {
            synchronized (PROBES) { PROBES.remove(server); }
            LOGGER.info("AutoPTU live Marea population policy reconciliation smoke passed: {} -> {}, seven stable bindings preserved",
                    probe.removedUuid(), replacement.getUuid());
            LOGGER.info("AutoPTU live Marea wild presence reconciliation smoke passed");
            return;
        }
        if (server.getTicks() > probe.deadlineTick()) {
            synchronized (PROBES) { PROBES.remove(server); }
            throw new IllegalStateException(
                    "Marea population policy reconciliation did not restore one member while preserving the other seven bindings");
        }
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
            long deadlineTick
    ) {}
}
