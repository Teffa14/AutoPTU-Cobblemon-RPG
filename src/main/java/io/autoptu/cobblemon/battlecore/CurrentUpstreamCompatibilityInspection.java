package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/** Records exact read-only upstream heads and bounded evidence for the current integration slice. */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "1f9b721ef2f5e2e1dcc2c6d70a93b13875e5f0db";
    public static final String AUTOPTU_PYTHON_SHA = "a3ff5cc71adb080973522abd604b0248b4447e06";

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
                        "RuntimeMoveResolution owns canonical accuracy/damage, PRE/post hooks, sequential authoritative area resolution, HP/history mutation and declaration-level resource bookkeeping. RuntimeMoveResolutionWithFollowUps executes PRE-damage follow-up moves synchronously through the authoritative runtime. StatusMoveRuntimeResolution now provides the verified non-damaging Status execution branch without mutating HP or damage history.",
                        "This does not establish complete modifier parity across all moves, abilities, items and terrain. Minecraft must not re-run damage, hooks, HP/history mutation, nested follow-up resolution or infer Status effects from a zero-damage result."));

        result.put(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "StatusStateStore preserves canonical status entries and selected prevention/application behavior is authoritative. Shell Shield authoritatively applies Withdrawn and a DEF combat-stage mutation through the generic PRE-damage seam. Java main 1f9b721 merges StatusMoveRuntimeResolution: combatant-target Status moves use ordinary accuracy, crit is false, damage is zero, HP and damage history are unchanged, and action/frequency resources are spent once.",
                        "Complete ticking, expiry, cures, immunities, source-sensitive interactions and generic move-special status application remain partial. A successful Status move resolution does not itself apply any status or move-special effect; Minecraft must wait for upstream generic effect hooks/events instead of inventing them."));

        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState and generic hook registries provide canonical environment/reaction seams. Main includes parity-backed PRE-damage reactions for Telepathy, Perception, Perception [Errata], Parry, Sway and Shell Shield.",
                        "Full terrain, weather, hazards, zones, reaction families and forced movement remain incomplete. Minecraft must not infer missing semantics."));

        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Authoritative MoveSpec metadata, move-frequency enforcement, delayed-hit support, area targeting, PRE-damage follow-up execution and merged combatant-target zero-damage Status execution are present for their verified contracts. DelayedHitBindingResolver expands stale target-id anchors and position-only delayed requests through EffectiveMoveTargetResolver. Stored target_position remains an authoritative aim anchor rather than forcing TILE semantics; current geometry, footprints, line of sight and HP eligibility determine affected combatants in stable order, while live target IDs follow their current authoritative position. StatusMoveRuntimeResolution emits MoveResolvedEvent with authoritative hit/miss, crit=false, damage=0 and unchanged target HP.",
                        "Unported move specials remain core-owned. Minecraft must not choose delayed targets, supply RNG/combat inputs, consume or refund move frequency/actions, or infer push, pull, crash, contact, status application, stat changes or other move semantics from text or presentation metadata."));

        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Generic hook families cover selected combat-stage, status, post-damage and PRE-damage behavior. Current Java main carries parity-backed Telepathy, Perception, Perception [Errata], Parry, Sway and Shell Shield; Sway has live authoritative nested follow-up execution and Shell Shield mutates status/stage state in core.",
                        "Representative ability families do not establish full parity. Minecraft may render generic semantic events only and must not evaluate ability legality, suppression, resource state, movement choice or effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.ITEMS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleRuntimeState can hold canonical item identity, selected item hooks such as Pink Pearl are parity-backed, and the RPG adapter has server-authoritative durable inventory reservations.",
                        "The item effect library is incomplete. Inventory authority does not authorize Minecraft-side healing, capture, crafting, damage or equipment mechanics."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState plus TrainerFeaturePrerequisiteResolution, TrainerFeatureContextResolution, TrainerFeatureFrequencyResolution, TrainerFeatureResourceResolution and TrainerFeatureUsageResolution are parity-backed for bounded families. AutoPTU Python main a3ff5cc was inspected read-only; its newest changes are Career loan-return preview behavior and do not promote Trainer Feature coverage.",
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
                        "Fabric/Cobblemon dedicated-server smoke coverage, authenticated identity, canonical reservations, live entity projection and generic semantic-event playback are present. The adapter may now project authoritative zero-damage Status MoveResolvedEvent playback because Java owns the declaration legality, accuracy result and resource spend.",
                        "Authenticated graphical campaign battle playback, complete RuntimeCombatantState materialization and full entity lifecycle remain pending. Status or move-special effects remain deferred until upstream emits authoritative generic effect/state events; Minecraft must not derive effects from move names, descriptions or damage=0."));

        return Map.copyOf(result);
    }
}
