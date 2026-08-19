package io.autoptu.cobblemon.battlecore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reservation-scoped immutable handoff that groups every integration-owned runtime
 * preparation artifact currently safe to pass toward AutoPTU-Java.
 *
 * This envelope still stops before runtime materialization. AutoPTU-Java must resolve
 * MovementProfile, ActionBudget, dynamic accuracy/evasion flags and damage modifiers.
 */
public record BattleRuntimePreparationEnvelope(
        String reservationId,
        long rngSeed,
        Map<String, RuntimeCombatantMaterializationInput> combatants,
        Map<String, List<AuthoritativeMoveMetadata>> movesByCombatant,
        Map<String, BattleCombatantHeldItemProjection> heldItemsByCombatant,
        Set<RuntimeCombatantMaterializationReadiness.Requirement> unresolvedCoreRequirements
) {
    public BattleRuntimePreparationEnvelope {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        combatants = copyCombatants(combatants);
        movesByCombatant = copyMoves(movesByCombatant);
        heldItemsByCombatant = copyHeldItems(heldItemsByCombatant);
        unresolvedCoreRequirements = unresolvedCoreRequirements == null
                ? Set.of()
                : Set.copyOf(unresolvedCoreRequirements);

        Set<String> roster = combatants.keySet();
        if (!movesByCombatant.keySet().equals(roster)) {
            throw new IllegalArgumentException("resolved move metadata must exactly cover the materialization roster");
        }
        if (!roster.containsAll(heldItemsByCombatant.keySet())) {
            throw new IllegalArgumentException("held items may reference only authoritative combatants");
        }

        for (String combatantId : roster) {
            RuntimeCombatantMaterializationInput input = combatants.get(combatantId);
            List<String> expectedMoveIds = input.moveLoadout().moveIds();
            List<String> actualMoveIds = movesByCombatant.get(combatantId).stream()
                    .map(AuthoritativeMoveMetadata::moveId)
                    .toList();
            if (!expectedMoveIds.equals(actualMoveIds)) {
                throw new IllegalArgumentException("resolved move metadata must match canonical loadout order for " + combatantId);
            }
        }
    }

    public static BattleRuntimePreparationEnvelope from(
            BattleCoreMaterializationInputProjection materialization,
            BattleCoreMoveCatalogProjection moveCatalog,
            BattleCoreHeldItemBootstrapProjection heldItems
    ) {
        Objects.requireNonNull(materialization, "materialization");
        Objects.requireNonNull(moveCatalog, "moveCatalog");
        Objects.requireNonNull(heldItems, "heldItems");
        requireReservation(materialization.reservationId(), moveCatalog.reservationId());
        requireReservation(materialization.reservationId(), heldItems.reservationId());

        LinkedHashSet<RuntimeCombatantMaterializationReadiness.Requirement> unresolved = new LinkedHashSet<>();
        for (Map.Entry<RuntimeCombatantMaterializationReadiness.Requirement, RuntimeCombatantMaterializationReadiness.Entry> entry
                : RuntimeCombatantMaterializationReadiness.entries().entrySet()) {
            if (entry.getValue().state() == RuntimeCombatantMaterializationReadiness.State.BLOCKED) {
                unresolved.add(entry.getKey());
            }
        }

        return new BattleRuntimePreparationEnvelope(
                materialization.reservationId(),
                materialization.rngSeed(),
                materialization.combatants(),
                moveCatalog.movesByCombatant(),
                heldItems.heldItemsByCombatant(),
                unresolved
        );
    }

    public boolean readyForRuntimeMaterialization() {
        return unresolvedCoreRequirements.isEmpty();
    }

    private static Map<String, RuntimeCombatantMaterializationInput> copyCombatants(
            Map<String, RuntimeCombatantMaterializationInput> source
    ) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("combatants must not be empty");
        }
        LinkedHashMap<String, RuntimeCombatantMaterializationInput> copy = new LinkedHashMap<>();
        for (Map.Entry<String, RuntimeCombatantMaterializationInput> entry : source.entrySet()) {
            String id = normalizeId(entry.getKey(), "combatant map key");
            RuntimeCombatantMaterializationInput value = Objects.requireNonNull(entry.getValue(), "combatant input");
            if (!id.equals(value.combatantId())) {
                throw new IllegalArgumentException("combatant map key must match embedded combatantId");
            }
            if (copy.put(id, value) != null) {
                throw new IllegalArgumentException("duplicate combatant input");
            }
        }
        return Map.copyOf(copy);
    }

    private static Map<String, List<AuthoritativeMoveMetadata>> copyMoves(
            Map<String, List<AuthoritativeMoveMetadata>> source
    ) {
        if (source == null) {
            throw new IllegalArgumentException("movesByCombatant is required");
        }
        LinkedHashMap<String, List<AuthoritativeMoveMetadata>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<AuthoritativeMoveMetadata>> entry : source.entrySet()) {
            String id = normalizeId(entry.getKey(), "move map key");
            ArrayList<AuthoritativeMoveMetadata> moves = new ArrayList<>();
            for (AuthoritativeMoveMetadata move : Objects.requireNonNull(entry.getValue(), "move list")) {
                moves.add(Objects.requireNonNull(move, "move metadata"));
            }
            if (copy.put(id, List.copyOf(moves)) != null) {
                throw new IllegalArgumentException("duplicate move metadata combatant");
            }
        }
        return Map.copyOf(copy);
    }

    private static Map<String, BattleCombatantHeldItemProjection> copyHeldItems(
            Map<String, BattleCombatantHeldItemProjection> source
    ) {
        LinkedHashMap<String, BattleCombatantHeldItemProjection> copy = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, BattleCombatantHeldItemProjection> entry : source.entrySet()) {
                String id = normalizeId(entry.getKey(), "held-item map key");
                BattleCombatantHeldItemProjection value = Objects.requireNonNull(entry.getValue(), "held-item projection");
                if (!id.equals(value.combatantId())) {
                    throw new IllegalArgumentException("held-item map key must match embedded combatantId");
                }
                if (copy.put(id, value) != null) {
                    throw new IllegalArgumentException("duplicate held-item combatant");
                }
            }
        }
        return Map.copyOf(copy);
    }

    private static String normalizeId(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }

    private static void requireReservation(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("runtime preparation artifacts span different battle reservations");
        }
    }
}
