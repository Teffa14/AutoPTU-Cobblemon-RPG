package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpstreamCompatibilityMatrixTest {
    @Test
    void coversEveryPermanentCapabilityCategory() {
        assertEquals(EnumSet.allOf(UpstreamCompatibilityMatrix.Capability.class),
                EnumSet.copyOf(UpstreamCompatibilityMatrix.entries().keySet()));
    }

    @Test
    void unsupportedMechanicsStayBlockedWhileRuntimeTestedAdapterIsOnlyPartial() {
        assertFalse(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_MOVEMENT_BEHAVIOR));
        assertFalse(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(
                UpstreamCompatibilityMatrix.Capability.AI_TACTICAL_SCORING_POLICY));

        UpstreamCompatibilityMatrix.Entry adapter = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, adapter.support());
        assertTrue(adapter.contracts().contains("BATTLE_STARTED_PRE"));
        assertTrue(adapter.contracts().contains("World-scoped durable player/profile/Pokemon/item stores"));
        assertTrue(adapter.contracts().contains("create-only WILD blueprint registry"));
        assertTrue(adapter.contracts().contains("identity-only WILD encounter correlation registry"));
        assertTrue(adapter.contracts().contains("Claim-time WILD roster resolution"));
        assertTrue(adapter.contracts().contains("status_block"));
        assertTrue(adapter.contracts().contains("combat_stage_block"));
        assertTrue(adapter.contracts().contains("Mirror Armor reflect"));
        assertTrue(adapter.adapterPolicy().contains("logged-in graphical player encounter is still pending"));
        assertTrue(adapter.adapterPolicy().contains("WILD blueprint registry"));
        assertTrue(adapter.adapterPolicy().contains("not durable across restart"));
        assertTrue(adapter.adapterPolicy().contains("no canonical encounter ID or PTU values may be derived from Cobblemon"));
        assertTrue(adapter.adapterPolicy().contains("cross-aggregate transaction recovery"));
        assertTrue(adapter.adapterPolicy().contains("Mirror Armor presentation may consume only"));
    }

    @Test
    void verifiedAndPartialContractsCanBeConsumedWithoutExpandingTheirScope() {
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING));
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE));
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ITEMS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE).support());
    }

    @Test
    void selectedPreventionFamiliesStayBoundedWithoutPromotingWholeLibraries() {
        UpstreamCompatibilityMatrix.Entry statuses = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE);
        assertTrue(statuses.contracts().contains("spatial ability prevention"));
        assertTrue(statuses.contracts().contains("Aroma Veil"));
        assertTrue(statuses.contracts().contains("RuleEffectEvent status_block"));
        assertTrue(statuses.contracts().contains("Infiltrator bypass"));
        assertTrue(statuses.adapterPolicy().contains("complete status ticking"));
        assertTrue(statuses.adapterPolicy().contains("charge consumption/removal"));
        assertTrue(statuses.adapterPolicy().contains("must never evaluate spatial radius"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, statuses.support());

        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);
        assertTrue(abilities.contracts().contains("CombatStagePreventionHookRegistry"));
        assertTrue(abilities.contracts().contains("Flower Veil"));
        assertTrue(abilities.contracts().contains("Mirror Armor"));
        assertTrue(abilities.contracts().contains("effect=reflect"));
        assertTrue(abilities.contracts().contains("CombatStageMutationService"));
        assertTrue(abilities.adapterPolicy().contains("must not decide external-drop eligibility"));
        assertTrue(abilities.adapterPolicy().contains("recursive-hook suppression"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
    }

    @Test
    void trainerFeaturesAdvanceOnlyWithinVerifiedFamilies() {
        UpstreamCompatibilityMatrix.Entry perks = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, perks.support());
        assertTrue(perks.contracts().contains("TrainerFeatureExecutionService.executeAuthoritative"));
        assertTrue(perks.contracts().contains("grant_temp_hp"));
        assertTrue(perks.contracts().contains("grant_ap"));
        assertTrue(perks.contracts().contains("apply_status"));
        assertTrue(perks.contracts().contains("remove_status"));
        assertTrue(perks.adapterPolicy().contains("2cd5c22f"));
        assertTrue(perks.adapterPolicy().contains("wider Python effect library"));
        assertTrue(perks.adapterPolicy().contains("may not grant Features"));
    }

    @Test
    void movementAndDamageGuardsRemainCoreOwned() {
        UpstreamCompatibilityMatrix.Entry movement = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY);
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, movement.support());
        assertTrue(movement.adapterPolicy().contains("Wallrunner"));
        assertTrue(movement.adapterPolicy().contains("weather"));
        assertTrue(movement.adapterPolicy().contains("Trainer Features"));

        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertTrue(damage.adapterPolicy().contains("independent HP mutation"));
    }

    @Test
    void itemContractCarriesIdentityWithoutPromotingItemLibrary() {
        UpstreamCompatibilityMatrix.Entry items = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ITEMS);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, items.support());
        assertTrue(items.contracts().contains("durable item/reservation store"));
        assertTrue(items.adapterPolicy().contains("unported item effects"));
        assertTrue(items.adapterPolicy().contains("client/entity equipment"));
    }

    @Test
    void matrixPinsTheUpstreamsThatWereActuallyInspected() {
        assertEquals("57c7c2a9751cf02facf5d176b9d0f95b996a9bd1", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
        assertEquals("2cd5c22f98dbab9524ff65b6bc6a3df6f54baa08", UpstreamCompatibilityMatrix.AUTOPTU_PYTHON_SHA);
    }
}
