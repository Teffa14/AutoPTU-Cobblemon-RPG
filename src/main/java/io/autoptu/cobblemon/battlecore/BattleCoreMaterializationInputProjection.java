package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reservation-scoped immutable bundle of every integration-frozen input currently
 * ready for AutoPTU-Java combatant materialization.
 *
 * The bundle stops before core-resolved constructor inputs. In particular it does
 * not carry a resolved MovementProfile, ActionBudget, dynamic accuracy/evasion
 * flags, or damage modifiers.
 */
public record BattleCoreMaterializationInputProjection(
        String reservationId,
        long rngSeed,
        Map<String, RuntimeCombatantMaterializationInput> combatants
) {
    public BattleCoreMaterializationInputProjection {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId must not be blank");
        }
        reservationId = reservationId.strip();
        if (combatants == null || combatants.isEmpty()) {
            throw new IllegalArgumentException("combatants must not be empty");
        }
        LinkedHashMap<String, RuntimeCombatantMaterializationInput> copy = new LinkedHashMap<>();
        for (Map.Entry<String, RuntimeCombatantMaterializationInput> entry : combatants.entrySet()) {
            String combatantId = entry.getKey();
            if (combatantId == null || combatantId.isBlank()) {
                throw new IllegalArgumentException("combatant map key must not be blank");
            }
            combatantId = combatantId.strip();
            RuntimeCombatantMaterializationInput input = Objects.requireNonNull(entry.getValue(), "materialization input");
            if (!combatantId.equals(input.combatantId())) {
                throw new IllegalArgumentException("combatant map key must match embedded combatantId");
            }
            if (copy.put(combatantId, input) != null) {
                throw new IllegalArgumentException("duplicate materialization input");
            }
        }
        combatants = Map.copyOf(copy);
    }

    public static BattleCoreMaterializationInputProjection from(BattleCoreAccuracyBootstrapProjection accuracyBootstrap) {
        Objects.requireNonNull(accuracyBootstrap, "accuracyBootstrap");
        BattleCoreTraitsBootstrapProjection traitsBootstrap = accuracyBootstrap.traitsBootstrap();
        BattleCoreGeometryBootstrapProjection geometryBootstrap = traitsBootstrap.geometryBootstrap();
        BattleCoreMovementBootstrapProjection movementBootstrap = geometryBootstrap.movementBootstrap();
        BattleCorePlacedBootstrapProjection placedBootstrap = movementBootstrap.placedBootstrap();
        BattleCoreBootstrapProjection combatState = placedBootstrap.combatState();
        BattleInitialPlacementSnapshot initialPlacement = placedBootstrap.initialPlacement();

        String reservationId = accuracyBootstrap.reservationId();
        requireReservation(reservationId, traitsBootstrap.reservationId());
        requireReservation(reservationId, geometryBootstrap.reservationId());
        requireReservation(reservationId, movementBootstrap.reservationId());
        requireReservation(reservationId, placedBootstrap.reservationId());
        requireReservation(reservationId, combatState.reservationId());
        requireReservation(reservationId, initialPlacement.reservationId());

        Set<String> roster = combatState.combatantIds();
        requireCoverage(roster, initialPlacement.placementsByCombatant(), "initial placement");
        requireCoverage(roster, combatState.healthByCombatant(), "health");
        requireCoverage(roster, combatState.combatStatsByCombatant(), "combat stats");
        requireCoverage(roster, accuracyBootstrap.accuracyEvasionByCombatant(), "accuracy/evasion");
        requireCoverage(roster, traitsBootstrap.traitsByCombatant(), "traits");
        requireCoverage(roster, combatState.moveLoadoutsByCombatant(), "move loadouts");
        requireCoverage(roster, combatState.affiliationByCombatant(), "affiliation");
        requireCoverage(roster, geometryBootstrap.geometryByCombatant(), "geometry");
        requireCoverage(roster, movementBootstrap.baseMovementByCombatant(), "base movement");

        LinkedHashMap<String, RuntimeCombatantMaterializationInput> inputs = new LinkedHashMap<>();
        for (String combatantId : roster) {
            RuntimeCombatantMaterializationInput input = new RuntimeCombatantMaterializationInput(
                    combatantId,
                    initialPlacement.placementsByCombatant().get(combatantId),
                    combatState.healthByCombatant().get(combatantId),
                    combatState.combatStatsByCombatant().get(combatantId),
                    accuracyBootstrap.accuracyEvasionByCombatant().get(combatantId),
                    traitsBootstrap.traitsByCombatant().get(combatantId),
                    combatState.moveLoadoutsByCombatant().get(combatantId),
                    combatState.affiliationByCombatant().get(combatantId),
                    geometryBootstrap.geometryByCombatant().get(combatantId),
                    movementBootstrap.baseMovementByCombatant().get(combatantId),
                    combatState.statusesByCombatant().getOrDefault(combatantId, Set.of())
            );
            inputs.put(combatantId, input);
        }
        return new BattleCoreMaterializationInputProjection(reservationId, combatState.rngSeed(), inputs);
    }

    private static void requireReservation(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("materialization inputs span different battle reservations");
        }
    }

    private static void requireCoverage(Set<String> roster, Map<String, ?> values, String label) {
        if (!values.keySet().equals(roster)) {
            throw new IllegalArgumentException(label + " must exactly cover the authoritative combatant roster");
        }
    }
}
