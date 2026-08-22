package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationFeatureCompatibilityTest {
    @Test
    void coversEveryIntegrationFeature() {
        assertEquals(
                EnumSet.allOf(IntegrationFeatureCompatibility.Feature.class),
                EnumSet.copyOf(IntegrationFeatureCompatibility.requirements().keySet())
        );
    }

    @Test
    void boundedCoreOwnedFeaturesHaveNoBlockingDependency() {
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.GRID_TARGET_PREVIEW).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.GRID_WORLD_COORDINATE_TRANSFORM).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.BATTLE_ARENA_RESERVATION).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.PLAYER_VS_WILD_AUTHORITY_COMPOSITION).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.INITIAL_COMBATANT_PLACEMENT).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_BASE_MOVEMENT_SNAPSHOT).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.RUNTIME_COMBATANT_MATERIALIZATION_INPUT).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.BATTLEFIELD_WORLD_OBSERVATION_SNAPSHOT).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.BATTLEFIELD_MOVEMENT_OBSERVATION_INPUT).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.WORLD_RELOCATION_PROJECTION).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.PLAYER_SHIFT_REQUEST).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.MOVE_SELECTION_REQUEST).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.DAMAGE_RESULT_PLAYBACK).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.ABILITY_EFFECT_PLAYBACK).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.ITEM_BATTLE_EFFECT_PLAYBACK).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.SEMANTIC_PRESENTATION_COMMANDS).hasBlockingDependency());
    }

    @Test
    void playerVsWildCompositionConsumesOnlyNarrowPartialAuthorityBoundaries() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.PLAYER_VS_WILD_AUTHORITY_COMPOSITION);
        assertEquals(EnumSet.of(
                        UpstreamCompatibilityMatrix.Capability.ITEMS,
                        UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                        UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK),
                EnumSet.copyOf(requirement.capabilities()));
        assertFalse(requirement.hasBlockingDependency());
        assertTrue(requirement.boundedScope().contains("server-issued reservation ID and RNG seed"));
        assertTrue(requirement.boundedScope().contains("exact player roster equality"));
        assertTrue(requirement.boundedScope().contains("compensate the player reservation"));
        assertTrue(requirement.boundedScope().contains("does not execute item effects"));
        assertTrue(requirement.boundedScope().contains("Trainer Features"));
        assertTrue(requirement.boundedScope().contains("damage"));
    }

    @Test
    void materializationInputPackagesOnlyFrozenDataAndLeavesRuntimeResolutionAbsent() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.RUNTIME_COMBATANT_MATERIALIZATION_INPUT);
        assertEquals(EnumSet.of(
                        UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                        UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY,
                        UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS,
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                        UpstreamCompatibilityMatrix.Capability.ABILITIES),
                EnumSet.copyOf(requirement.capabilities()));
        assertTrue(requirement.boundedScope().contains("Resolved MovementProfile"));
        assertTrue(requirement.boundedScope().contains("ActionBudget"));
        assertTrue(requirement.boundedScope().contains("dynamic accuracy/evasion"));
        assertTrue(requirement.boundedScope().contains("damage modifiers"));
        assertTrue(requirement.boundedScope().contains("remain absent"));
        assertFalse(requirement.hasBlockingDependency());
    }

    @Test
    void gridWorldFeaturesDependOnlyOnVerifiedGridContracts() {
        EnumSet<UpstreamCompatibilityMatrix.Capability> expected = EnumSet.of(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY
        );

        for (IntegrationFeatureCompatibility.Feature feature : new IntegrationFeatureCompatibility.Feature[]{
                IntegrationFeatureCompatibility.Feature.GRID_WORLD_COORDINATE_TRANSFORM,
                IntegrationFeatureCompatibility.Feature.BATTLE_ARENA_RESERVATION,
                IntegrationFeatureCompatibility.Feature.INITIAL_COMBATANT_PLACEMENT,
                IntegrationFeatureCompatibility.Feature.WORLD_RELOCATION_PROJECTION
        }) {
            IntegrationFeatureCompatibility.Requirement requirement =
                    IntegrationFeatureCompatibility.requirement(feature);
            assertEquals(expected, EnumSet.copyOf(requirement.capabilities()));
            requirement.capabilities().forEach(capability -> assertEquals(
                    UpstreamCompatibilityMatrix.Support.VERIFIED,
                    UpstreamCompatibilityMatrix.entry(capability).support()
            ));
        }
    }

    @Test
    void canonicalBaseMovementStopsBeforeRuntimeEffectResolution() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_BASE_MOVEMENT_SNAPSHOT);
        assertEquals(EnumSet.of(UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY),
                EnumSet.copyOf(requirement.capabilities()));
        assertTrue(requirement.boundedScope().contains("Overland/Swim/Sky"));
        assertTrue(requirement.boundedScope().contains("Wallrunner"));
        assertTrue(requirement.boundedScope().contains("weather"));
        assertTrue(requirement.boundedScope().contains("core-owned"));
    }

    @Test
    void initialPlacementScopeDoesNotClaimMissingSpatialRules() {
        String scope = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.INITIAL_COMBATANT_PLACEMENT).boundedScope();
        assertTrue(scope.contains("Footprint size"));
        assertTrue(scope.contains("facing"));
        assertTrue(scope.contains("placement legality"));
    }

    @Test
    void worldObservationSnapshotUsesPartialTerrainContractWithoutClaimingLegality() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.BATTLEFIELD_WORLD_OBSERVATION_SNAPSHOT);
        assertEquals(EnumSet.of(
                        UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                        UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY,
                        UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS),
                EnumSet.copyOf(requirement.capabilities()));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS).support());
        assertTrue(requirement.boundedScope().contains("raw world inputs"));
        assertFalse(requirement.hasBlockingDependency());
    }

    @Test
    void movementObservationInputStopsBeforeMovementGridSemantics() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.BATTLEFIELD_MOVEMENT_OBSERVATION_INPUT);
        assertEquals(EnumSet.of(
                        UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY,
                        UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS),
                EnumSet.copyOf(requirement.capabilities()));
        assertTrue(requirement.boundedScope().contains("stops before MovementGrid"));
        assertTrue(requirement.boundedScope().contains("terrain cost"));
        assertFalse(requirement.hasBlockingDependency());
    }

    @Test
    void unsupportedRuleFamiliesRemainExplicitlyBlockedWhileLiveAdapterIsBoundedPartial() {
        assertTrue(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.FORCED_MOVEMENT_PLAYBACK).hasBlockingDependency());
        assertTrue(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.AUTOBATTLER_TACTICAL_POLICY).hasBlockingDependency());

        IntegrationFeatureCompatibility.Requirement live = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.LIVE_MINECRAFT_BATTLE_ADAPTER);
        assertFalse(live.hasBlockingDependency());
        assertEquals(EnumSet.of(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK),
                EnumSet.copyOf(live.capabilities()));
        assertTrue(live.boundedScope().contains("entity-bound authoritative relocation"));
        assertTrue(live.boundedScope().contains("HP projection"));
        assertTrue(live.boundedScope().contains("battle-trigger interception"));
        assertTrue(live.boundedScope().contains("canonical participant/combatant IDs"));
        assertTrue(live.boundedScope().contains("contract-tested"));
    }

    @Test
    void partialFamiliesRemainNarrowEvenWhenPlaybackIsAllowed() {
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.ITEMS).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK).support());
    }
}
