package io.autoptu.cobblemon.battlecore;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable handoff of integration-frozen combatant inputs that are already safe to
 * supply toward AutoPTU-Java runtime materialization.
 *
 * This record deliberately excludes resolved MovementProfile, ActionBudget, dynamic
 * accuracy/evasion flags, and resolved damage modifiers. Those values remain
 * AutoPTU-Java-owned and must not be defaulted or inferred by Minecraft/Cobblemon.
 */
public record RuntimeCombatantMaterializationInput(
        String combatantId,
        BattleCombatantInitialPlacement initialPlacement,
        BattleCombatantHealthProjection health,
        BattleCombatantStatProjection combatStats,
        BattleCombatantAccuracyEvasionProjection baseAccuracyEvasion,
        BattleCombatantTraitsProjection traits,
        BattleCombatantMoveLoadoutProjection moveLoadout,
        BattleCombatantAffiliationProjection affiliation,
        BattleCombatantGeometryProjection geometry,
        BattleCombatantBaseMovementProjection baseMovement,
        Set<String> statuses
) {
    public RuntimeCombatantMaterializationInput {
        if (combatantId == null || combatantId.isBlank()) {
            throw new IllegalArgumentException("combatantId must not be blank");
        }
        combatantId = combatantId.strip();
        initialPlacement = requireMatching(combatantId, Objects.requireNonNull(initialPlacement, "initialPlacement"));
        health = requireMatching(combatantId, Objects.requireNonNull(health, "health"));
        combatStats = requireMatching(combatantId, Objects.requireNonNull(combatStats, "combatStats"));
        baseAccuracyEvasion = requireMatching(combatantId, Objects.requireNonNull(baseAccuracyEvasion, "baseAccuracyEvasion"));
        traits = requireMatching(combatantId, Objects.requireNonNull(traits, "traits"));
        moveLoadout = requireMatching(combatantId, Objects.requireNonNull(moveLoadout, "moveLoadout"));
        affiliation = requireMatching(combatantId, Objects.requireNonNull(affiliation, "affiliation"));
        geometry = requireMatching(combatantId, Objects.requireNonNull(geometry, "geometry"));
        baseMovement = requireMatching(combatantId, Objects.requireNonNull(baseMovement, "baseMovement"));

        LinkedHashSet<String> copiedStatuses = new LinkedHashSet<>();
        if (statuses != null) {
            for (String status : statuses) {
                if (status == null || status.isBlank()) {
                    throw new IllegalArgumentException("status must not be blank");
                }
                copiedStatuses.add(status.strip());
            }
        }
        statuses = Set.copyOf(copiedStatuses);
    }

    private static BattleCombatantInitialPlacement requireMatching(String id, BattleCombatantInitialPlacement value) {
        requireId(id, value.combatantId());
        return value;
    }

    private static BattleCombatantHealthProjection requireMatching(String id, BattleCombatantHealthProjection value) {
        requireId(id, value.combatantId());
        return value;
    }

    private static BattleCombatantStatProjection requireMatching(String id, BattleCombatantStatProjection value) {
        requireId(id, value.combatantId());
        return value;
    }

    private static BattleCombatantAccuracyEvasionProjection requireMatching(String id, BattleCombatantAccuracyEvasionProjection value) {
        requireId(id, value.combatantId());
        return value;
    }

    private static BattleCombatantTraitsProjection requireMatching(String id, BattleCombatantTraitsProjection value) {
        requireId(id, value.combatantId());
        return value;
    }

    private static BattleCombatantMoveLoadoutProjection requireMatching(String id, BattleCombatantMoveLoadoutProjection value) {
        requireId(id, value.combatantId());
        return value;
    }

    private static BattleCombatantAffiliationProjection requireMatching(String id, BattleCombatantAffiliationProjection value) {
        requireId(id, value.combatantId());
        return value;
    }

    private static BattleCombatantGeometryProjection requireMatching(String id, BattleCombatantGeometryProjection value) {
        requireId(id, value.combatantId());
        return value;
    }

    private static BattleCombatantBaseMovementProjection requireMatching(String id, BattleCombatantBaseMovementProjection value) {
        requireId(id, value.combatantId());
        return value;
    }

    private static void requireId(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("all materialization inputs must belong to combatant " + expected);
        }
    }
}
