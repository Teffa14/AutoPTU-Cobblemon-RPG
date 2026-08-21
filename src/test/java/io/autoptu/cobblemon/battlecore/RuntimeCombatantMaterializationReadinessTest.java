package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static io.autoptu.cobblemon.battlecore.RuntimeCombatantMaterializationReadiness.Authority.AUTOPTU_JAVA_RESOLVED;
import static io.autoptu.cobblemon.battlecore.RuntimeCombatantMaterializationReadiness.Authority.INTEGRATION_FROZEN;
import static io.autoptu.cobblemon.battlecore.RuntimeCombatantMaterializationReadiness.Requirement.ACTION_BUDGET_INITIALIZATION;
import static io.autoptu.cobblemon.battlecore.RuntimeCombatantMaterializationReadiness.Requirement.BASE_MOVEMENT;
import static io.autoptu.cobblemon.battlecore.RuntimeCombatantMaterializationReadiness.Requirement.DYNAMIC_ACCURACY_EVASION_FLAGS;
import static io.autoptu.cobblemon.battlecore.RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_DAMAGE_MODIFIERS;
import static io.autoptu.cobblemon.battlecore.RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_MOVEMENT_PROFILE;
import static io.autoptu.cobblemon.battlecore.RuntimeCombatantMaterializationReadiness.State.BLOCKED;
import static io.autoptu.cobblemon.battlecore.RuntimeCombatantMaterializationReadiness.State.READY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeCombatantMaterializationReadinessTest {
    @Test
    void integrationOwnedInputsAreExplicitlySeparatedFromCoreResolvedInputs() {
        assertEquals(INTEGRATION_FROZEN, RuntimeCombatantMaterializationReadiness.entry(BASE_MOVEMENT).authority());
        assertEquals(READY, RuntimeCombatantMaterializationReadiness.entry(BASE_MOVEMENT).state());

        assertEquals(AUTOPTU_JAVA_RESOLVED, RuntimeCombatantMaterializationReadiness.entry(RESOLVED_MOVEMENT_PROFILE).authority());
        assertEquals(BLOCKED, RuntimeCombatantMaterializationReadiness.entry(RESOLVED_MOVEMENT_PROFILE).state());

        assertEquals(AUTOPTU_JAVA_RESOLVED, RuntimeCombatantMaterializationReadiness.entry(ACTION_BUDGET_INITIALIZATION).authority());
        assertEquals(READY, RuntimeCombatantMaterializationReadiness.entry(ACTION_BUDGET_INITIALIZATION).state());
        assertTrue(RuntimeCombatantMaterializationReadiness.entry(ACTION_BUDGET_INITIALIZATION).evidence().contains("ActionBudget()"));
        assertTrue(RuntimeCombatantMaterializationReadiness.entry(ACTION_BUDGET_INITIALIZATION).evidence().contains("adapter supplies no trusted action availability"));

        assertEquals(BLOCKED, RuntimeCombatantMaterializationReadiness.entry(DYNAMIC_ACCURACY_EVASION_FLAGS).state());
        assertEquals(BLOCKED, RuntimeCombatantMaterializationReadiness.entry(RESOLVED_DAMAGE_MODIFIERS).state());
    }

    @Test
    void runtimeMaterializationFailsClosedUntilEveryRemainingCoreOwnedInputIsResolved() {
        assertEquals(RuntimeCombatantMaterializationReadiness.Requirement.values().length,
                RuntimeCombatantMaterializationReadiness.entries().size());
        assertEquals(3, RuntimeCombatantMaterializationReadiness.entries().values().stream()
                .filter(entry -> entry.state() == BLOCKED)
                .count());
        assertFalse(RuntimeCombatantMaterializationReadiness.canMaterializeRuntimeCombatant());
    }
}
