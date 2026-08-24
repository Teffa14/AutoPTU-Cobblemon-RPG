package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("5f0df19c14e1e2a9b2bfc64522cf704b483e564e", CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("1cc0e5e449e0ad1745abf0d432892fd90998ef1c", CurrentUpstreamCompatibilityInspection.AUTOPTU_PYTHON_SHA);
    }

    @Test
    void currentInspectionCoversEveryPermanentCapabilityCategory() {
        assertEquals(UpstreamCompatibilityMatrix.Capability.values().length,
                CurrentUpstreamCompatibilityInspection.evidence().size());
        for (UpstreamCompatibilityMatrix.Capability capability : UpstreamCompatibilityMatrix.Capability.values()) {
            CurrentUpstreamCompatibilityInspection.Evidence evidence =
                    CurrentUpstreamCompatibilityInspection.evidence(capability);
            assertTrue(evidence.contracts() != null && !evidence.contracts().isBlank());
            assertTrue(evidence.limitation() != null && !evidence.limitation().isBlank());
        }
    }

    @Test
    void boundedForcedMovementDoesNotPromoteCompleteMovementBehavior() {
        CurrentUpstreamCompatibilityInspection.Evidence movement = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_MOVEMENT_BEHAVIOR);
        assertEquals(UpstreamCompatibilityMatrix.Support.BLOCKING, movement.support());
        assertTrue(movement.contracts().contains("Sway adjacent push"));
        assertTrue(movement.limitation().contains("must fail closed"));
        assertTrue(movement.limitation().contains("must not generalize"));
    }

    @Test
    void mergedPreDamageHooksStayPartialAtCategoryLevel() {
        CurrentUpstreamCompatibilityInspection.Evidence abilities = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertTrue(abilities.contracts().contains("Sway"));
        assertTrue(abilities.contracts().contains("Shell Shield"));
        assertTrue(abilities.contracts().contains("nested follow-up execution"));
        assertTrue(abilities.limitation().contains("do not establish full parity"));

        CurrentUpstreamCompatibilityInspection.Evidence statuses = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, statuses.support());
        assertTrue(statuses.contracts().contains("Withdrawn"));
        assertTrue(statuses.limitation().contains("remain partial"));
    }

    @Test
    void mergedStatusRuntimeIsPromotedWithoutInventingStatusEffects() {
        CurrentUpstreamCompatibilityInspection.Evidence statuses = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE);
        CurrentUpstreamCompatibilityInspection.Evidence moves = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);
        CurrentUpstreamCompatibilityInspection.Evidence adapter = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, statuses.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, adapter.support());
        assertTrue(statuses.contracts().contains("StatusMoveRuntimeResolution"));
        assertTrue(statuses.contracts().contains("crit is false"));
        assertTrue(statuses.contracts().contains("damage is zero"));
        assertTrue(statuses.contracts().contains("action/frequency resources are spent once"));
        assertTrue(statuses.limitation().contains("does not itself apply any status"));
        assertTrue(moves.contracts().contains("MoveResolvedEvent"));
        assertTrue(moves.contracts().contains("damage=0"));
        assertTrue(moves.limitation().contains("status application"));
        assertTrue(adapter.contracts().contains("zero-damage Status MoveResolvedEvent playback"));
        assertTrue(adapter.limitation().contains("must not derive effects"));
    }

    @Test
    void genericMoveSpecialRegistryIsPromotedOnlyAsAnAuthoritativeSeam() {
        CurrentUpstreamCompatibilityInspection.Evidence moves = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);
        CurrentUpstreamCompatibilityInspection.Evidence abilities = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);
        CurrentUpstreamCompatibilityInspection.Evidence adapter = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, adapter.support());
        assertTrue(moves.contracts().contains("MoveSpecialHookRegistry"));
        assertTrue(moves.contracts().contains("POST_DAMAGE runs move-specific handlers before global handlers"));
        assertTrue(moves.contracts().contains("other phases run global before specific"));
        assertTrue(moves.contracts().contains("MoveSpecialResultState"));
        assertTrue(moves.contracts().contains("shared mutable result mapping"));
        assertTrue(moves.contracts().contains("dispatch-start snapshot"));
        assertTrue(moves.limitation().contains("complete move-special coverage"));
        assertTrue(moves.limitation().contains("must not register substitute PTU mechanics"));
        assertTrue(abilities.limitation().contains("must not evaluate ability legality"));
        assertTrue(abilities.limitation().contains("registry dispatch"));
        assertTrue(adapter.contracts().contains("downstream projection"));
        assertTrue(adapter.limitation().contains("handler ordering"));
        assertTrue(adapter.limitation().contains("mutable result contents"));
    }

    @Test
    void livePreDamageMoveSpecialExecutionPromotesOnlyItsVerifiedPhaseBoundary() {
        CurrentUpstreamCompatibilityInspection.Evidence damage = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        CurrentUpstreamCompatibilityInspection.Evidence moves = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);
        CurrentUpstreamCompatibilityInspection.Evidence abilities = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);
        CurrentUpstreamCompatibilityInspection.Evidence adapter = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
        assertTrue(damage.contracts().contains("5f0df19"));
        assertTrue(damage.contracts().contains("before defender PRE-damage reactions"));
        assertTrue(damage.contracts().contains("final HP/history mutation"));
        assertTrue(damage.limitation().contains("Only PRE_DAMAGE move-special execution is live"));
        assertTrue(damage.limitation().contains("POST_DAMAGE and END_ACTION parity remain incomplete"));
        assertTrue(moves.contracts().contains("5f0df19"));
        assertTrue(moves.contracts().contains("executes MoveSpecialPreDamageResolution inside BattleRuntime"));
        assertTrue(moves.contracts().contains("PRE_DAMAGE, POST_DAMAGE and END_ACTION"));
        assertTrue(moves.contracts().contains("743ef231a164727cee549d39d4c2b7a898c64cd7c4365931b71008267bdeff53"));
        assertTrue(moves.contracts().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(moves.limitation().contains("PR #188"));
        assertTrue(moves.limitation().contains("does not wire POST_DAMAGE into BattleRuntime"));
        assertTrue(moves.limitation().contains("dispatch move-special phases"));
        assertTrue(abilities.contracts().contains("executed in the authoritative BattleRuntime"));
        assertTrue(abilities.limitation().contains("do not establish full parity"));
        assertTrue(adapter.contracts().contains("wholly inside AutoPTU-Java BattleRuntime"));
        assertTrue(adapter.limitation().contains("POST_DAMAGE/END_ACTION"));
    }

    @Test
    void statusRuntimeStillDependsOnCoreTargetingActionEconomyAndLegalChoices() {
        CurrentUpstreamCompatibilityInspection.Evidence targeting = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING);
        CurrentUpstreamCompatibilityInspection.Evidence actions = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE);
        CurrentUpstreamCompatibilityInspection.Evidence legalActions = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE);

        assertTrue(targeting.contracts().contains("StatusMoveRuntimeResolution"));
        assertTrue(targeting.contracts().contains("MoveChoiceRevalidation"));
        assertTrue(actions.contracts().contains("StatusMoveRuntimeResolution"));
        assertTrue(actions.contracts().contains("exactly once"));
        assertTrue(legalActions.contracts().contains("legal combatant-target MoveChoice"));
        assertTrue(legalActions.contracts().contains("MoveChoiceRevalidation"));
    }

    @Test
    void tacticalScoringRemainsBlockingWhileLegalActionInfrastructureIsVerified() {
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.BLOCKING,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.AI_TACTICAL_SCORING_POLICY).support());
    }

    @Test
    void currentPythonInspectionDoesNotPromoteTrainerFeatures() {
        CurrentUpstreamCompatibilityInspection.Evidence perks = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, perks.support());
        assertTrue(perks.contracts().contains("1cc0e5e"));
        assertTrue(perks.contracts().contains("Career active-roster and season-progress persistence"));
    }
}
