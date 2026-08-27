package io.autoptu.cobblemon.fabric.battle;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-owned zone/context table for choosing an already-authored canonical WILD roster.
 *
 * Entries contain complete {@link ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint}
 * values that were authored by trusted RPG/campaign code before this table is queried. This service
 * never reads Cobblemon Pokemon, entities, battle state, HP, moves, statuses, RNG or results and never
 * derives PTU values from species/level. It only chooses which trusted roster belongs to a canonical
 * encounter and freezes that decision create-only for later publication/provisioning.
 *
 * Selection randomness is a deterministic RPG/world choice derived from canonical encounter identity
 * plus the server-owned zone/context key. It is deliberately separate from AutoPTU-Java battle RNG.
 */
public final class ServerOwnedWildEncounterTable implements CanonicalWildEncounterBlueprintSource {
    public record ContextKey(String zoneId, String contextId) {
        public ContextKey {
            zoneId = requireId(zoneId, "zoneId");
            contextId = requireId(contextId, "contextId");
        }
    }

    public record WeightedRoster(
            String entryId,
            int weight,
            List<ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint> pokemon
    ) {
        public WeightedRoster {
            entryId = requireId(entryId, "entryId");
            if (weight < 1) throw new IllegalArgumentException("weight must be >= 1");
            if (pokemon == null || pokemon.isEmpty()) throw new IllegalArgumentException("pokemon must not be empty");
            if (pokemon.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("pokemon must not contain null entries");
            }
            pokemon = List.copyOf(pokemon);
        }
    }

    public record Selection(
            ContextKey context,
            String entryId,
            CanonicalWildEncounterBlueprint blueprint
    ) {
        public Selection {
            context = Objects.requireNonNull(context, "context");
            entryId = requireId(entryId, "entryId");
            blueprint = Objects.requireNonNull(blueprint, "blueprint");
        }
    }

    private final Map<ContextKey, List<WeightedRoster>> tables;
    private final Map<String, Selection> selectedByEncounterId = new LinkedHashMap<>();

    public ServerOwnedWildEncounterTable(Map<ContextKey, List<WeightedRoster>> tables) {
        if (tables == null || tables.isEmpty()) throw new IllegalArgumentException("tables must not be empty");
        LinkedHashMap<ContextKey, List<WeightedRoster>> copy = new LinkedHashMap<>();
        for (Map.Entry<ContextKey, List<WeightedRoster>> entry : tables.entrySet()) {
            ContextKey key = Objects.requireNonNull(entry.getKey(), "context key");
            List<WeightedRoster> rosters = entry.getValue();
            if (rosters == null || rosters.isEmpty()) {
                throw new IllegalArgumentException("each context must contain at least one roster");
            }
            if (rosters.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("rosters must not contain null entries");
            }
            ArrayList<WeightedRoster> stable = new ArrayList<>(rosters);
            ensureUniqueEntryIds(key, stable);
            copy.put(key, List.copyOf(stable));
        }
        this.tables = Map.copyOf(copy);
    }

    /**
     * Selects and freezes one trusted roster for this canonical encounter. Unknown contexts fail
     * closed. Re-selecting the same encounter ID is rejected rather than silently changing species
     * or canonical PTU state after presentation has begun.
     */
    public synchronized Optional<Selection> select(
            String canonicalEncounterId,
            String zoneId,
            String contextId,
            int side
    ) {
        String encounterId = requireId(canonicalEncounterId, "canonicalEncounterId");
        if (side < 0) throw new IllegalArgumentException("side must be >= 0");
        if (selectedByEncounterId.containsKey(encounterId)) {
            throw new IllegalStateException("canonical WILD encounter already has a frozen table selection");
        }

        ContextKey key = new ContextKey(zoneId, contextId);
        List<WeightedRoster> candidates = tables.get(key);
        if (candidates == null) return Optional.empty();

        WeightedRoster selected = weightedChoice(encounterId, key, candidates);
        CanonicalWildEncounterBlueprint blueprint = new CanonicalWildEncounterBlueprint(
                encounterId,
                side,
                selected.pokemon()
        );
        Selection selection = new Selection(key, selected.entryId(), blueprint);
        selectedByEncounterId.put(encounterId, selection);
        return Optional.of(selection);
    }

    @Override
    public synchronized Optional<CanonicalWildEncounterBlueprint> resolve(String canonicalEncounterId) {
        if (canonicalEncounterId == null || canonicalEncounterId.isBlank()) return Optional.empty();
        Selection selection = selectedByEncounterId.get(canonicalEncounterId.strip());
        return selection == null ? Optional.empty() : Optional.of(selection.blueprint());
    }

    public synchronized Optional<Selection> selection(String canonicalEncounterId) {
        if (canonicalEncounterId == null || canonicalEncounterId.isBlank()) return Optional.empty();
        return Optional.ofNullable(selectedByEncounterId.get(canonicalEncounterId.strip()));
    }

    public synchronized boolean release(String canonicalEncounterId) {
        if (canonicalEncounterId == null || canonicalEncounterId.isBlank()) return false;
        return selectedByEncounterId.remove(canonicalEncounterId.strip()) != null;
    }

    private static WeightedRoster weightedChoice(
            String encounterId,
            ContextKey context,
            List<WeightedRoster> candidates
    ) {
        long totalWeight = 0L;
        for (WeightedRoster candidate : candidates) {
            totalWeight = Math.addExact(totalWeight, candidate.weight());
        }
        if (totalWeight > Integer.MAX_VALUE) {
            throw new IllegalStateException("wild encounter table weight exceeds supported range");
        }

        int roll = Math.floorMod(selectionHash(encounterId, context), (int) totalWeight);
        int cursor = 0;
        for (WeightedRoster candidate : candidates) {
            cursor += candidate.weight();
            if (roll < cursor) return candidate;
        }
        throw new IllegalStateException("wild encounter table selection failed");
    }

    private static int selectionHash(String encounterId, ContextKey context) {
        byte[] digest = sha256("wild-table-v1|" + encounterId + "|" + context.zoneId() + "|" + context.contextId());
        return ByteBuffer.wrap(digest, 0, Integer.BYTES).getInt();
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void ensureUniqueEntryIds(ContextKey key, List<WeightedRoster> rosters) {
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        for (WeightedRoster roster : rosters) {
            if (!ids.add(roster.entryId())) {
                throw new IllegalArgumentException("duplicate wild encounter entry id for " + key.zoneId() + "/" + key.contextId());
            }
        }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
