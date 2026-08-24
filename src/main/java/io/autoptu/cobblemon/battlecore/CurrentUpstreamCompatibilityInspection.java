package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/** Records exact read-only upstream heads and bounded evidence for the current integration slice. */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "3caac611a987322a70dbdc34c56d613b96dadb92";
    public static final String AUTOPTU_PYTHON_SHA = "5ab2c175be6542b867f1676cf6848b9b15fd346f";

    public record Evidence(UpstreamCompatibilityMatrix.Support support, String contracts, String limitation) {
        public Evidence {
            if (support == null) throw new IllegalArgumentException("support is required");
            if (contracts == null || contracts.isBlank()) throw new IllegalArgumentException("contracts are required");
            if (limitation == null || limitation.isBlank()) throw new IllegalArgumentException("limitation is required");
        }
    }

    private static final Map<UpstreamCompatibilityMatrix.Capability, Evidence> EVIDENCE = build();
    private CurrentUpstreamCompatibilityInspection() {}

    public static Evidence evidence(UpstreamCompatibilityMatrix.Capability capability) {
        Evidence evidence = EVIDENCE.get(capability);
        if (evidence == null) throw new IllegalArgumentException("no current inspection evidence for " + capability);
        return evidence;
    }

    public static Map<UpstreamCompatibilityMatrix.Capability, Evidence> evidence() { return EVIDENCE; }

    private static Map<UpstreamCompatibilityMatrix.Capability, Evidence> build() {
        EnumMap<UpstreamCompatibilityMatrix.Capability, Evidence> result =
                new EnumMap<>(UpstreamCompatibilityMatrix.Capability.class);

        result.put(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "EffectiveMoveTargetResolver and RuntimeAreaMoveTargeting derive and revalidate authoritative targets, footprints, range, area anchors, line of sight, HP eligibility including hp <= 0 exclusion, inactive positive-HP candidate preservation, and stable target order from BattleRuntimeState. StatusMoveRuntimeResolution reuses MoveChoiceRevalidation for combatant-target Status moves.",
                        "Minecraft must not supply effective target lists, target anchors, footprint overlap, line-of-sight results, HP eligibility filters or a generic active-state filter."));

        result.put(UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "MovementGrid plus resolved MovementProfile/JumpProfile own bounded Shift/Jump legality, terrain cost and base Overland/Swim/Sky movement contracts. PRE-damage reaction hooks consume authoritative legal movement rather than Minecraft geometry rules.",
                        "Verified base movement legality does not authorize the adapter to invent status, ability, weather, equipment or Trainer Feature movement modifiers."));

        result.put(UpstreamCompatibilityMatrix.Capability.COMPLETE_MOVEMENT_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.BLOCKING,
                        "Current main has bounded authoritative reaction movement primitives, including reaction escape and Sway adjacent push selection, but the general push/pull/knockback/interception and interaction-driven movement family is not complete.",
                        "Minecraft must fail closed for unverified forced movement and must not generalize the Sway push primitive into broad forced-movement legality."));

        result.put(UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "Damage-base tables, type effectiveness, STAB, accuracy/evasion stages, CombatantStatProfile, CombatStageMutationService and selected parity-backed prevention/reflection hooks are core-owned. StatusMoveRuntimeResolution resolves ordinary authoritative accuracy but deliberately skips physical/special damage arithmetic.",
                        "Minecraft may project authoritative results and stages only; it must not calculate accuracy, damage, stage legality, prevention or reflection."));

        result.put(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleRuntimeState, TrainerRuntimeState, ActionBudget, RuntimeInitiativeOrderAssembly and InitiativeRoundRebuilder own initiative and declaration resources. TemporaryApGrant state and temporary AP grants/expiry are core-owned. PRE-damage follow-up execution reuses the authoritative runtime without spending ordinary action economy or move frequency twice; Sway's STANDARD spend remains hook-owned. StatusMoveRuntimeResolution consumes the declared action and records move-frequency use exactly once on hit or miss.",
                        "Minecraft must not spend/refund actions, validate frequency, grant temporary AP, perform temporary AP grants/expiry, rebuild initiative or duplicate reaction/resource consumption."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "ROUND_START executes FieldRoundLifecycleHook at order 10, DelayedHitRoundLifecycleHook at order 20, RoundTemporaryEffectExpiryHook at order 30, server-owned TemporaryApGrant expiry and Trainer action reset at order 40, Pokemon round-temporary-effect cleanup at order 45, then DeclaredActionRoundLifecycleHook at order 50. Reaction readiness/usage state is stored in authoritative runtime state.",
                        "Complete Python lifecycle parity remains broader. Minecraft must not supply currentRound, temporary-effect metadata, temporary AP grants, Trainer action reset, queue/RNG mutation, manufacture round transitions, expiry, send-out decisions or missing lifecycle effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "RuntimeMoveResolution owns canonical accuracy/damage, PRE/post hooks, sequential authoritative area resolution, HP/history mutation and declaration-level resource bookkeeping. RuntimeMoveResolutionWithFollowUps executes PRE-damage follow-up moves synchronously through the authoritative runtime. Java main 3caac61 keeps MoveSpecialPreDamageResolution live inside BattleRuntime before defender PRE-damage reactions. The merged MoveSpecialPostDamageResolution bridge carries Python-compatible already-applied damage_dealt and shared result state for POST_DAMAGE, but remains a bridge contract rather than a BattleRuntime call site. StatusMoveRuntimeResolution provides the verified non-damaging Status execution branch without mutating HP or damage history.",
                        "Only PRE_DAMAGE move-special execution is live in the main damage pipeline. POST_DAMAGE and END_ACTION runtime parity remain incomplete; the merged POST_DAMAGE bridge is intentionally not wired into BattleRuntime because Python orders it after item/HP/history interactions not fully represented in Java. Minecraft must not dispatch hooks, mutate move-special result state, re-run damage, HP/history mutation, nested follow-up resolution or infer missing modifiers/effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "StatusStateStore preserves canonical status entries and selected prevention/application behavior is authoritative. Shell Shield authoritatively applies Withdrawn and a DEF combat-stage mutation through the generic PRE-damage seam. Java main includes StatusMoveRuntimeResolution: combatant-target Status moves use ordinary accuracy, crit is false, damage is zero, HP and damage history are unchanged, and action/frequency resources are spent once.",
                        "Complete ticking, expiry, cures, immunities, source-sensitive interactions and generic move-special status application remain partial. A successful Status move resolution does not itself apply any status or move-special effect; Minecraft must wait for upstream generic effect hooks/events instead of inventing them."));

        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState and generic hook registries provide canonical environment/reaction seams. Main includes parity-backed PRE-damage reactions for Telepathy, Perception, Perception [Errata], Parry, Sway and Shell Shield. Live PRE_DAMAGE move-special execution now precedes defender PRE-damage reactions in BattleRuntime.",
                        "Full terrain, weather, hazards, zones, reaction families and forced movement remain incomplete. Minecraft must not infer missing semantics or reorder authoritative hook phases."));

        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Authoritative MoveSpec metadata, move-frequency enforcement, delayed-hit support, area targeting, PRE-damage follow-up execution and merged combatant-target zero-damage Status execution are present for their verified contracts. MoveSpecialHookRegistry freezes Python-compatible generic move-special dispatch: unknown phases default to POST_DAMAGE, POST_DAMAGE runs move-specific handlers before global handlers, other phases run global before specific, move names normalize by trim/lowercase, and Shield Dust skips non-Status POST_DAMAGE dispatch while allowing Status dispatch. MoveSpecialResultState preserves the shared mutable result mapping across handlers while MoveSpecialHookContext.hit remains the dispatch-start snapshot. Java main 3caac61 keeps MoveSpecialPreDamageResolution live in BattleRuntime and also includes the bridge-only MoveSpecialPostDamageResolution contract: damage_dealt is the already-applied nonnegative damage, POST_DAMAGE sees shared PRE result state, later POST mutations are not retroactive HP authority, and dispatch preserves specific-before-global ordering. The frozen Python oracle execution-order contract remains PRE_DAMAGE, POST_DAMAGE and END_ACTION with digest 743ef231a164727cee549d39d4c2b7a898c64cd7c4365931b71008267bdeff53 pinned to AutoPTU 16d228efa63aabecb67fa788959a359aac7f8f03. DelayedHitBindingResolver expands stale target-id anchors and position-only delayed requests through EffectiveMoveTargetResolver. Stored target_position remains an authoritative aim anchor rather than forcing TILE semantics; current geometry, footprints, line of sight and HP eligibility determine affected combatants in stable order, while live target IDs follow their current authoritative position. StatusMoveRuntimeResolution emits MoveResolvedEvent with authoritative hit/miss, crit=false, damage=0 and unchanged target HP.",
                        "Live PRE_DAMAGE execution plus a merged POST_DAMAGE bridge does not imply POST_DAMAGE/END_ACTION runtime parity or complete move-special coverage. MoveSpecialPostDamageResolution is not wired into BattleRuntime. Minecraft must not register substitute PTU mechanics, dispatch move-special phases, mutate move-special result state, choose delayed targets, supply RNG/combat inputs, consume or refund move frequency/actions, or infer push, pull, crash, contact, status application, stat changes or other move semantics."));

        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Generic hook families cover selected combat-stage, status, post-damage and PRE-damage behavior. Current Java main carries parity-backed Telepathy, Perception, Perception [Errata], Parry, Sway and Shell Shield; Sway has live authoritative nested follow-up execution and Shell Shield mutates status/stage state in core. The generic PRE_DAMAGE move-special registry is executed in the authoritative BattleRuntime; the POST_DAMAGE bridge remains uncalled infrastructure.",
                        "Representative ability families and generic bridge coverage do not establish full parity. Minecraft may render generic semantic events only and must not evaluate ability legality, suppression, registry dispatch, phase timing, result mutation, resource state, movement choice or effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.ITEMS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleRuntimeState can hold canonical item identity, selected item hooks such as Pink Pearl are parity-backed, and the RPG adapter has server-authoritative durable inventory reservations.",
                        "The item effect library is incomplete. Inventory authority does not authorize Minecraft-side healing, capture, crafting, damage or equipment mechanics."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState plus TrainerFeaturePrerequisiteResolution, TrainerFeatureContextResolution, TrainerFeatureFrequencyResolution, TrainerFeatureResourceResolution and TrainerFeatureUsageResolution are parity-backed for bounded families. AutoPTU Python main 5ab2c17 was inspected read-only; its newest commits continue Career persistence/UI hardening, including guarding repeated stalled battle retry clicks, and do not promote Trainer Feature or battle-oracle coverage.",
                        "Trainer Features remain partial. Minecraft must not grant Features, decide gates, mutate AP/resources or execute missing effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "RuntimeAutobattlerActionSpace derives immutable legal BattleChoice values from canonical state and authoritative lifecycle/action availability. Status execution still requires a legal combatant-target MoveChoice and live MoveChoiceRevalidation.",
                        "AI or Minecraft may select only from core-produced legal choices and must not manufacture legality."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_TACTICAL_SCORING_POLICY,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.BLOCKING,
                        "Full Python-equivalent tactical scoring and policy are not yet ported to the authoritative Java runtime.",
                        "Do not claim AI tactical parity or implement tactical scoring inside the Minecraft adapter."));

        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Fabric/Cobblemon dedicated-server smoke coverage, authenticated identity, canonical reservations, live entity projection and generic semantic-event playback are present. The adapter may project authoritative zero-damage Status MoveResolvedEvent playback because Java owns declaration legality, accuracy result and resource spend. Live PRE_DAMAGE move-special execution remains wholly inside AutoPTU-Java BattleRuntime. The merged POST_DAMAGE bridge carries authoritative transport semantics only and does not create downstream execution authority until Java wires that phase into its runtime.",
                        "Authenticated graphical campaign battle playback, complete RuntimeCombatantState materialization and full entity lifecycle remain pending. POST_DAMAGE/END_ACTION move-special runtime, complete Status effects and other missing mechanics remain deferred to upstream. Minecraft must not derive effects from move names, descriptions, damage=0, handler ordering, call-site phase order, damage_dealt transport or mutable result contents."));

        return Map.copyOf(result);
    }
}
