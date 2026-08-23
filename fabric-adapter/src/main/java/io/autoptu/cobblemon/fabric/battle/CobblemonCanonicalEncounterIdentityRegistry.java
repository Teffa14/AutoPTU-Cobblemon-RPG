package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleEncounterParticipantRequest;
import io.autoptu.cobblemon.authority.BattleParticipantKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Server-side identity bridge from opaque Cobblemon battle UUIDs to canonical encounter IDs.
 *
 * This registry stores identity mappings only. It must never derive PTU stats, HP, moves, abilities,
 * items, legality or outcomes from Cobblemon state. Canonical battle state is resolved later by the
 * authority repository using the IDs returned here.
 */
public final class CobblemonCanonicalEncounterIdentityRegistry {
    private record ExternalParticipantKey(
            CobblemonBattleStartInterceptor.ParticipantKind kind,
            String actorId
    ) {}

    private record Binding(
            String canonicalParticipantId,
            LinkedHashMap<String, String> combatantIdsByPokemonId
    ) {}

    private final Map<ExternalParticipantKey, Binding> bindings = new HashMap<>();
    private final Map<String, ExternalParticipantKey> pokemonOwners = new HashMap<>();
    private final Map<String, ExternalParticipantKey> canonicalCombatantOwners = new HashMap<>();

    public synchronized void register(
            CobblemonBattleStartInterceptor.ParticipantKind kind,
            String externalActorId,
            String canonicalParticipantId,
            Map<String, String> canonicalCombatantIdsByExternalPokemonId
    ) {
        ExternalParticipantKey key = participantKey(kind, externalActorId);
        if (bindings.containsKey(key)) {
            throw new IllegalStateException("external participant is already registered");
        }
        install(key, canonicalParticipantId, canonicalCombatantIdsByExternalPokemonId, false);
    }

    /**
     * Refreshes the identity-only Pokemon mapping for an already known external participant.
     *
     * The participant's canonical identity cannot change. This supports party changes between
     * encounters while retaining alias protection across other actors. No Pokemon values other than
     * opaque external identity and canonical combatant identity are stored here.
     */
    public synchronized void registerOrReplace(
            CobblemonBattleStartInterceptor.ParticipantKind kind,
            String externalActorId,
            String canonicalParticipantId,
            Map<String, String> canonicalCombatantIdsByExternalPokemonId
    ) {
        ExternalParticipantKey key = participantKey(kind, externalActorId);
        Binding existing = bindings.get(key);
        String participantId = requireId(canonicalParticipantId, "canonicalParticipantId");
        if (existing != null && !existing.canonicalParticipantId().equals(participantId)) {
            throw new IllegalStateException("canonical participant identity cannot be replaced");
        }
        install(key, participantId, canonicalCombatantIdsByExternalPokemonId, true);
    }

    /** Resolves only the server-owned participant identity for an already registered external actor. */
    public synchronized Optional<String> resolveParticipantId(
            CobblemonBattleStartInterceptor.ParticipantKind kind,
            String externalActorId
    ) {
        if (kind == null || externalActorId == null || externalActorId.isBlank()) return Optional.empty();
        Binding binding = bindings.get(new ExternalParticipantKey(kind, externalActorId.strip()));
        return binding == null ? Optional.empty() : Optional.of(binding.canonicalParticipantId());
    }

    public synchronized Optional<BattleEncounterParticipantRequest> resolve(
            CobblemonBattleStartInterceptor.ParticipantIdentity external
    ) {
        if (external == null) return Optional.empty();
        ExternalParticipantKey key = new ExternalParticipantKey(external.kind(), external.actorId());
        Binding binding = bindings.get(key);
        if (binding == null) return Optional.empty();

        if (!List.copyOf(binding.combatantIdsByPokemonId().keySet()).equals(external.pokemonIds())) {
            return Optional.empty();
        }

        ArrayList<String> combatantIds = new ArrayList<>();
        for (String externalPokemonId : external.pokemonIds()) {
            String canonicalCombatantId = binding.combatantIdsByPokemonId().get(externalPokemonId);
            if (canonicalCombatantId == null) return Optional.empty();
            combatantIds.add(canonicalCombatantId);
        }

        return Optional.of(new BattleEncounterParticipantRequest(
                external.side(),
                binding.canonicalParticipantId(),
                authorityKind(external.kind()),
                combatantIds
        ));
    }

    public synchronized int registeredParticipantCount() {
        return bindings.size();
    }

    private void install(
            ExternalParticipantKey key,
            String canonicalParticipantId,
            Map<String, String> canonicalCombatantIdsByExternalPokemonId,
            boolean replacing
    ) {
        String participantId = requireId(canonicalParticipantId, "canonicalParticipantId");
        LinkedHashMap<String, String> mappings = normalizeMappings(canonicalCombatantIdsByExternalPokemonId);

        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            ExternalParticipantKey externalOwner = pokemonOwners.get(entry.getKey());
            if (externalOwner != null && !externalOwner.equals(key)) {
                throw new IllegalStateException("external Pokemon identity is already registered to another participant");
            }
            ExternalParticipantKey canonicalOwner = canonicalCombatantOwners.get(entry.getValue());
            if (canonicalOwner != null && !canonicalOwner.equals(key)) {
                throw new IllegalStateException("canonical combatant identity is already mapped to another participant");
            }
        }

        Binding existing = bindings.get(key);
        if (existing != null && !replacing) {
            throw new IllegalStateException("external participant is already registered");
        }
        if (existing != null) {
            existing.combatantIdsByPokemonId().forEach((externalPokemonId, canonicalCombatantId) -> {
                pokemonOwners.remove(externalPokemonId, key);
                canonicalCombatantOwners.remove(canonicalCombatantId, key);
            });
        }

        bindings.put(key, new Binding(participantId, mappings));
        mappings.forEach((externalPokemonId, canonicalCombatantId) -> {
            pokemonOwners.put(externalPokemonId, key);
            canonicalCombatantOwners.put(canonicalCombatantId, key);
        });
    }

    private static LinkedHashMap<String, String> normalizeMappings(
            Map<String, String> canonicalCombatantIdsByExternalPokemonId
    ) {
        if (canonicalCombatantIdsByExternalPokemonId == null || canonicalCombatantIdsByExternalPokemonId.isEmpty()) {
            throw new IllegalArgumentException("Pokemon identity mappings are required");
        }
        LinkedHashMap<String, String> mappings = new LinkedHashMap<>();
        Set<String> pendingCanonicalCombatantIds = new HashSet<>();
        for (Map.Entry<String, String> entry : canonicalCombatantIdsByExternalPokemonId.entrySet()) {
            String externalPokemonId = requireId(entry.getKey(), "externalPokemonId");
            String canonicalCombatantId = requireId(entry.getValue(), "canonicalCombatantId");
            if (mappings.putIfAbsent(externalPokemonId, canonicalCombatantId) != null) {
                throw new IllegalArgumentException("duplicate external Pokemon identity");
            }
            if (!pendingCanonicalCombatantIds.add(canonicalCombatantId)) {
                throw new IllegalArgumentException("duplicate canonical combatant identity");
            }
        }
        return mappings;
    }

    private static ExternalParticipantKey participantKey(
            CobblemonBattleStartInterceptor.ParticipantKind kind,
            String externalActorId
    ) {
        if (kind == null) throw new IllegalArgumentException("kind is required");
        return new ExternalParticipantKey(kind, requireId(externalActorId, "externalActorId"));
    }

    private static BattleParticipantKind authorityKind(CobblemonBattleStartInterceptor.ParticipantKind kind) {
        return switch (kind) {
            case PLAYER -> BattleParticipantKind.PLAYER;
            case NPC -> BattleParticipantKind.NPC;
            case WILD -> BattleParticipantKind.WILD;
        };
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
