package io.autoptu.cobblemon.fabric.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * World-lifecycle-scoped correlation between a server-owned canonical encounter and the opaque
 * Cobblemon WILD actor chosen to present it.
 *
 * Trusted server encounter/projection code registers this association before battle interception.
 * The registry carries identity only: no species, level, HP, stats, moves, abilities, items,
 * statuses or other PTU values may enter this boundary.
 */
public final class WorldScopedWildEncounterCorrelationRegistry {
    private final Map<String, String> encounterByExternalActor = new LinkedHashMap<>();
    private final Map<String, String> externalActorByEncounter = new LinkedHashMap<>();

    public synchronized void register(String canonicalEncounterId, String externalWildActorId) {
        String encounterId = requireId(canonicalEncounterId, "canonicalEncounterId");
        String actorId = requireId(externalWildActorId, "externalWildActorId");

        String existingEncounter = encounterByExternalActor.get(actorId);
        if (existingEncounter != null && !existingEncounter.equals(encounterId)) {
            throw new IllegalStateException("external WILD actor is already correlated to another encounter");
        }
        String existingActor = externalActorByEncounter.get(encounterId);
        if (existingActor != null && !existingActor.equals(actorId)) {
            throw new IllegalStateException("canonical WILD encounter is already correlated to another actor");
        }
        if (existingEncounter != null) {
            throw new IllegalStateException("WILD encounter correlation is already registered");
        }

        encounterByExternalActor.put(actorId, encounterId);
        externalActorByEncounter.put(encounterId, actorId);
    }

    public synchronized Optional<String> resolveCanonicalEncounterId(String externalWildActorId) {
        if (externalWildActorId == null || externalWildActorId.isBlank()) return Optional.empty();
        return Optional.ofNullable(encounterByExternalActor.get(externalWildActorId.strip()));
    }

    public synchronized boolean removeByExternalActor(String externalWildActorId) {
        if (externalWildActorId == null || externalWildActorId.isBlank()) return false;
        String actorId = externalWildActorId.strip();
        String encounterId = encounterByExternalActor.remove(actorId);
        if (encounterId == null) return false;
        externalActorByEncounter.remove(encounterId, actorId);
        return true;
    }

    public synchronized int size() {
        return encounterByExternalActor.size();
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
