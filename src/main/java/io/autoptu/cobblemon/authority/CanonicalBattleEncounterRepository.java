package io.autoptu.cobblemon.authority;

import java.util.Optional;

/**
 * Server-owned resolver from canonical participant/combatant identities to canonical battle state.
 * Platform UUID mapping must happen before this boundary.
 */
public interface CanonicalBattleEncounterRepository {
    Optional<CanonicalBattlePokemonView> findCombatant(
            BattleParticipantKind participantKind,
            String participantId,
            String combatantId
    );
}
