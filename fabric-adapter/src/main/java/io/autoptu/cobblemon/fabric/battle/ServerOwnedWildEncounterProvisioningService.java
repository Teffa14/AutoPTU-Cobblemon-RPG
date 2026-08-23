package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleParticipantKind;
import io.autoptu.cobblemon.authority.CanonicalAccuracyEvasion;
import io.autoptu.cobblemon.authority.CanonicalBaseMovement;
import io.autoptu.cobblemon.authority.CanonicalBattleEncounterRepository;
import io.autoptu.cobblemon.authority.CanonicalBattlePokemonView;
import io.autoptu.cobblemon.authority.CanonicalBattleTraits;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalEncounterPokemonState;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import io.autoptu.cobblemon.authority.CanonicalInjuryState;
import io.autoptu.cobblemon.authority.CanonicalMoveLoadout;
import io.autoptu.cobblemon.authority.CanonicalStatusState;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Prepares canonical WILD encounter state before Cobblemon battle-start interception.
 *
 * Canonical values come from server-owned RPG/encounter data. The external WILD actor identifier is
 * retained only as a correlation key for the later identity-only Cobblemon handoff. No PokemonEntity
 * fields are accepted by this service.
 */
public final class ServerOwnedWildEncounterProvisioningService
        implements ServerOwnedWildEncounterIdentityBinder.CanonicalWildRosterSource,
        CanonicalBattleEncounterRepository {

    public record WildPokemonBlueprint(
            String speciesId,
            int level,
            Set<String> capabilities,
            Set<String> statuses,
            CanonicalStatusState statusState,
            CanonicalCombatStats combatStats,
            CanonicalHealth health,
            CanonicalMoveLoadout moveLoadout,
            CanonicalBaseMovement baseMovement,
            CanonicalBattleTraits battleTraits,
            CanonicalAccuracyEvasion accuracyEvasion,
            CanonicalInjuryState injuryState,
            String heldItemInstanceId,
            long revision
    ) {
        public WildPokemonBlueprint {
            if (speciesId == null || speciesId.isBlank()) throw new IllegalArgumentException("speciesId is required");
            speciesId = speciesId.strip();
            if (level < 1) throw new IllegalArgumentException("level must be >= 1");
            capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
            statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
            statusState = Objects.requireNonNull(statusState, "statusState");
            combatStats = Objects.requireNonNull(combatStats, "combatStats");
            health = Objects.requireNonNull(health, "health");
            moveLoadout = Objects.requireNonNull(moveLoadout, "moveLoadout");
            baseMovement = Objects.requireNonNull(baseMovement, "baseMovement");
            battleTraits = Objects.requireNonNull(battleTraits, "battleTraits");
            accuracyEvasion = Objects.requireNonNull(accuracyEvasion, "accuracyEvasion");
            injuryState = Objects.requireNonNull(injuryState, "injuryState");
            heldItemInstanceId = heldItemInstanceId == null || heldItemInstanceId.isBlank()
                    ? null
                    : heldItemInstanceId.strip();
            if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");
        }
    }

    public record ProvisionedWildEncounter(
            String canonicalEncounterId,
            String externalWildActorId,
            int side,
            String canonicalParticipantId,
            List<CanonicalEncounterPokemonState> pokemon,
            long deterministicSeed
    ) {
        public ProvisionedWildEncounter {
            canonicalEncounterId = requireId(canonicalEncounterId, "canonicalEncounterId");
            externalWildActorId = requireId(externalWildActorId, "externalWildActorId");
            canonicalParticipantId = requireId(canonicalParticipantId, "canonicalParticipantId");
            if (side < 0) throw new IllegalArgumentException("side must be >= 0");
            if (pokemon == null || pokemon.isEmpty()) throw new IllegalArgumentException("pokemon must not be empty");
            pokemon = List.copyOf(pokemon);
        }

        ServerOwnedWildEncounterIdentityBinder.CanonicalWildRoster roster() {
            return new ServerOwnedWildEncounterIdentityBinder.CanonicalWildRoster(
                    canonicalParticipantId,
                    pokemon.stream().map(CanonicalEncounterPokemonState::pokemonId).toList()
            );
        }
    }

    private final Map<String, ProvisionedWildEncounter> byExternalActor = new HashMap<>();
    private final Map<String, ProvisionedWildEncounter> byParticipant = new HashMap<>();

    public synchronized ProvisionedWildEncounter provision(
            String canonicalEncounterId,
            String externalWildActorId,
            int side,
            List<WildPokemonBlueprint> roster
    ) {
        String encounterId = requireId(canonicalEncounterId, "canonicalEncounterId");
        String actorId = requireId(externalWildActorId, "externalWildActorId");
        if (side < 0) throw new IllegalArgumentException("side must be >= 0");
        if (roster == null || roster.isEmpty()) throw new IllegalArgumentException("roster must not be empty");
        if (roster.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("roster must not contain null entries");

        String participantId = "wild-participant:" + digestHex("participant|" + encounterId).substring(0, 24);
        if (byExternalActor.containsKey(actorId)) {
            throw new IllegalStateException("external WILD actor is already provisioned");
        }
        if (byParticipant.containsKey(participantId)) {
            throw new IllegalStateException("canonical WILD encounter is already provisioned");
        }

        ArrayList<CanonicalEncounterPokemonState> states = new ArrayList<>(roster.size());
        for (int index = 0; index < roster.size(); index++) {
            WildPokemonBlueprint seed = roster.get(index);
            String pokemonId = "wild-pokemon:" + digestHex("pokemon|" + encounterId + "|" + index).substring(0, 24);
            states.add(new CanonicalEncounterPokemonState(
                    pokemonId,
                    seed.speciesId(),
                    seed.level(),
                    seed.capabilities(),
                    seed.statuses(),
                    seed.statusState(),
                    seed.combatStats(),
                    seed.health(),
                    seed.moveLoadout(),
                    seed.baseMovement(),
                    seed.battleTraits(),
                    seed.accuracyEvasion(),
                    seed.injuryState(),
                    seed.heldItemInstanceId(),
                    seed.revision()
            ));
        }

        ProvisionedWildEncounter provisioned = new ProvisionedWildEncounter(
                encounterId,
                actorId,
                side,
                participantId,
                states,
                deterministicSeed(encounterId)
        );
        byExternalActor.put(actorId, provisioned);
        byParticipant.put(participantId, provisioned);
        return provisioned;
    }

    @Override
    public synchronized Optional<ServerOwnedWildEncounterIdentityBinder.CanonicalWildRoster> resolve(
            String cobblemonBattleId,
            int side,
            String externalWildActorId
    ) {
        if (cobblemonBattleId == null || cobblemonBattleId.isBlank()
                || externalWildActorId == null || externalWildActorId.isBlank()) {
            return Optional.empty();
        }
        ProvisionedWildEncounter provisioned = byExternalActor.get(externalWildActorId.strip());
        if (provisioned == null || provisioned.side() != side) return Optional.empty();
        return Optional.of(provisioned.roster());
    }

    @Override
    public synchronized Optional<CanonicalBattlePokemonView> findCombatant(
            BattleParticipantKind participantKind,
            String participantId,
            String combatantId
    ) {
        if (participantKind != BattleParticipantKind.WILD
                || participantId == null || participantId.isBlank()
                || combatantId == null || combatantId.isBlank()) {
            return Optional.empty();
        }
        ProvisionedWildEncounter provisioned = byParticipant.get(participantId.strip());
        if (provisioned == null) return Optional.empty();
        String requested = combatantId.strip();
        return provisioned.pokemon().stream()
                .filter(state -> state.pokemonId().equals(requested))
                .map(state -> (CanonicalBattlePokemonView) state)
                .findFirst();
    }

    public synchronized Optional<ProvisionedWildEncounter> findByExternalActor(String externalWildActorId) {
        if (externalWildActorId == null || externalWildActorId.isBlank()) return Optional.empty();
        return Optional.ofNullable(byExternalActor.get(externalWildActorId.strip()));
    }

    public synchronized boolean release(String externalWildActorId) {
        if (externalWildActorId == null || externalWildActorId.isBlank()) return false;
        ProvisionedWildEncounter removed = byExternalActor.remove(externalWildActorId.strip());
        if (removed == null) return false;
        byParticipant.remove(removed.canonicalParticipantId(), removed);
        return true;
    }

    static long deterministicSeed(String canonicalEncounterId) {
        byte[] digest = digest("rng|" + requireId(canonicalEncounterId, "canonicalEncounterId"));
        return ByteBuffer.wrap(digest, 0, Long.BYTES).getLong();
    }

    private static String digestHex(String value) {
        byte[] bytes = digest(value);
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) hex.append(String.format("%02x", b & 0xff));
        return hex.toString();
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}
