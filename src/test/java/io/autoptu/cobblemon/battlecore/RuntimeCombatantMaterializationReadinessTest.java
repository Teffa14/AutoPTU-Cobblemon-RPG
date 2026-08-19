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

class RuntimeCombatantMaterializationReadinessTest {
    @Test
    void integrationOwnedInputsAreExplicitlySeparatedFromCoreResolvedInputs() {
        assertEquals(INTEGRATION_FROZEN, RuntimeCombatantMaterializationReadiness.entry(BASE_MOVEMENT).authority());
        assertEquals(READY, RuntimeCombatantMaterializationReadiness.entry(BASE_MOVEMENT).state());

        assertEquals(AUTOPTU_JAVA_RESOLVED, RuntimeCombatantMaterializationReadiness.entry(RESOLVED_MOVEMENT_PROFILE).authority());
        assertEquals(BLOCKED, RuntimeCombatantMaterializationReadiness.entry(RESOLVED_MOVEMENT_PROFILE).state());
        assertEquals(BLOCKED, RuntimeCombatantMaterializationReadiness.entry(ACTION_BUDGET_INITIALIZATION).state());
        assertEquals(BLOCKED, RuntimeCombatantMaterializationReadiness.entry(DYNAMIC_ACCURACY_EVASION_FLAGS).state());
        assertEquals(BLOCKED, RuntimeCombatantMaterializationReadiness.entry(RESOLVED_DAMAGE_MODIFIERS).state());
    }

    @Test
    void runtimeMaterializationFailsClosedUntilEveryCoreOwnedInputIsResolved() {
        assertEquals(RuntimeCombatantMaterializationReadiness.Requirement.values().length,
                RuntimeCombatantMaterializationReadiness.entries().size());
        assertFalse(RuntimeCombatantMaterializationReadiness.canMaterializeRuntimeCombatant());
    }
}
