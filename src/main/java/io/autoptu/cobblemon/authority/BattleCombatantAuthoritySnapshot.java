package io.autoptu.cobblemon.authority;

import java.util.Set;

/**
 * Immutable combatant snapshot for a multi-side encounter.
 *
 * Participant/team identity is explicit and server-owned. Player ownership is not overloaded as
 * battle affiliation, which allows wild/NPC combatants to exist without fake player ownership.
 */
public record BattleCombatantAuthoritySnapshot(
        String combatantId,
        String participantId,
        String teamId,
        BattleParticipantKind participantKind,
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
    public BattleCombatantAuthoritySnapshot {
        combatantId = requireId(combatantId, "combatantId");
        participantId = requireId(participantId, "participantId");
        teamId = requireId(teamId, "teamId");
        if (participantKind == null) throw new IllegalArgumentException("participantKind is required");
        speciesId = requireId(speciesId, "speciesId");
        if (level < 1) throw new IllegalArgumentException("level must be >= 1");
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
        statusState = statusState == null ? CanonicalStatusState.fromNames(statuses) : statusState;
        if (!statusState.names().equals(statuses)) {
            throw new IllegalArgumentException("statusState names must exactly match statuses");
        }
        heldItemInstanceId = heldItemInstanceId == null || heldItemInstanceId.isBlank()
                ? null
                : heldItemInstanceId.strip();
        if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");
    }

    public static BattleCombatantAuthoritySnapshot from(
            CanonicalBattlePokemonView state,
            String participantId,
            String teamId,
            BattleParticipantKind participantKind
    ) {
        if (state == null) throw new IllegalArgumentException("state is required");
        return new BattleCombatantAuthoritySnapshot(
                state.pokemonId(),
                participantId,
                teamId,
                participantKind,
                state.speciesId(),
                state.level(),
                state.capabilities(),
                state.statuses(),
                state.statusState(),
                state.combatStats(),
                state.health(),
                state.moveLoadout(),
                state.baseMovement(),
                state.battleTraits(),
                state.accuracyEvasion(),
                state.injuryState(),
                state.heldItemInstanceId(),
                state.revision()
        );
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }
}
