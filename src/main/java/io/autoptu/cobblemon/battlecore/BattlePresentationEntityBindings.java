package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Reservation-scoped one-to-one binding from authoritative combatant IDs to opaque presentation entity IDs.
 * The binding never grants PTU authority to the presentation entity.
 */
public final class BattlePresentationEntityBindings {
    private final String reservationId;
    private final Map<String, PresentationEntityBinding> byCombatant;

    private BattlePresentationEntityBindings(String reservationId, Map<String, PresentationEntityBinding> byCombatant) {
        this.reservationId = reservationId;
        this.byCombatant = Map.copyOf(byCombatant);
    }

    public static BattlePresentationEntityBindings bind(
            BattleAuthoritySnapshot snapshot,
            Map<String, String> presentationEntityIdsByCombatant) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot is required");
        if (presentationEntityIdsByCombatant == null) throw new IllegalArgumentException("presentationEntityIdsByCombatant is required");

        Set<String> roster = new HashSet<>();
        snapshot.roster().forEach(pokemon -> roster.add(pokemon.pokemonId()));
        if (!presentationEntityIdsByCombatant.keySet().equals(roster)) {
            throw new IllegalArgumentException("presentation entity bindings must exactly cover the frozen combatant roster");
        }

        HashMap<String, PresentationEntityBinding> bindings = new HashMap<>();
        HashSet<String> presentationIds = new HashSet<>();
        for (String combatantId : roster) {
            PresentationEntityBinding binding = new PresentationEntityBinding(
                    combatantId, presentationEntityIdsByCombatant.get(combatantId));
            if (!presentationIds.add(binding.presentationEntityId())) {
                throw new IllegalArgumentException("one presentation entity cannot represent multiple combatants");
            }
            bindings.put(combatantId, binding);
        }
        return new BattlePresentationEntityBindings(snapshot.reservationId(), bindings);
    }

    public String reservationId() { return reservationId; }
    public Map<String, PresentationEntityBinding> byCombatant() { return byCombatant; }

    public PresentationEntityBinding requireBinding(String reservationId, String combatantId) {
        if (reservationId == null || !this.reservationId.equals(reservationId)) {
            throw new IllegalArgumentException("presentation binding reservation mismatch");
        }
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId is required");
        }
        PresentationEntityBinding binding = byCombatant.get(combatantId.strip());
        if (binding == null) {
            throw new IllegalArgumentException("combatant has no presentation entity binding: " + combatantId);
        }
        return binding;
    }
}
