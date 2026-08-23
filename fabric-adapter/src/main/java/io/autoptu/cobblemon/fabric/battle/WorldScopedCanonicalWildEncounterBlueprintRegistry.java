package io.autoptu.cobblemon.fabric.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-owned, world-lifecycle-scoped registry for already-decided canonical WILD encounter blueprints.
 *
 * Writers are trusted RPG/campaign services. Cobblemon identities and entity state never enter this
 * registry. Registration is create-only so a later presentation signal cannot silently replace the
 * canonical values chosen for an encounter. Removal is explicit when the encounter is abandoned or
 * completed. Durability across a server restart is deliberately outside this bounded contract.
 */
public final class WorldScopedCanonicalWildEncounterBlueprintRegistry
        implements CanonicalWildEncounterBlueprintSource {

    private final Map<String, CanonicalWildEncounterBlueprint> byEncounterId = new LinkedHashMap<>();

    public synchronized void register(CanonicalWildEncounterBlueprint blueprint) {
        CanonicalWildEncounterBlueprint trusted = Objects.requireNonNull(blueprint, "blueprint");
        String encounterId = trusted.canonicalEncounterId();
        if (byEncounterId.putIfAbsent(encounterId, trusted) != null) {
            throw new IllegalStateException("canonical WILD encounter blueprint is already registered");
        }
    }

    @Override
    public synchronized Optional<CanonicalWildEncounterBlueprint> resolve(String canonicalEncounterId) {
        if (canonicalEncounterId == null || canonicalEncounterId.isBlank()) return Optional.empty();
        return Optional.ofNullable(byEncounterId.get(canonicalEncounterId.strip()));
    }

    public synchronized boolean remove(String canonicalEncounterId) {
        if (canonicalEncounterId == null || canonicalEncounterId.isBlank()) return false;
        return byEncounterId.remove(canonicalEncounterId.strip()) != null;
    }

    public synchronized int size() {
        return byEncounterId.size();
    }
}
