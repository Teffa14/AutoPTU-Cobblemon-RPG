package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and bounded current evidence.
 * This supplements, but never broadens, the permanent support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "4c75dc082ae7848bdfa9c4e385e08ffde6760d9e";
    public static final String AUTOPTU_PYTHON_SHA = "e4bb0ca38b7018710af476ce365d515a387de4e7";

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
                        "EffectiveMoveTargetResolver derives affected tiles, current authoritative combatant positions, footprint overlap, line of sight and stable candidate order from BattleRuntimeState. Current HP eligibility excludes hp <= 0 while preserving inactive positive-HP candidates, matching the pinned Python collector contract.",
                        "Minecraft must not supply effective target lists, live target anchors, footprint overlap, line-of-sight results, HP eligibility filters or a generic active-state filter. Target eligibility remains core-owned."));

        result.put(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleRuntimeState supplies canonical initiative inputs. RuntimeInitiativeOrderAssembly.fromState and InitiativeRoundRebuilder.authoritative own ordering and rollover. DelayedHitResourcePolicy preserves the originating action and frequency spend during ROUND_START. TrainerRuntimeState owns its ActionBudget and temporary AP grants; ROUND_START expires due temporary AP before resetting Trainer actions.",
                        "Minecraft must not provide a precomputed rollover order, delayed-hit RNG, delayed-hit queue mutation, action/frequency bookkeeping, Trainer action-reset state, temporary AP grants/expiry, or a Trainer ID that collides with a combatant ID."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "ROUND_START executes FieldRoundLifecycleHook at order 10, DelayedHitRoundLifecycleHook at order 20, RoundTemporaryEffectExpiryHook at order 30, server-owned TemporaryApGrant expiry and Trainer action reset at order 40, Pokemon round-temporary-effect cleanup at order 45, then DeclaredActionRoundLifecycleHook at order 50. BattleRuntimeState owns immutable DeclaredActionState. RoundTrainerFeatureLifecyclePolicy freezes the Python guards around declaration cleanup, initial send-out, initiative and round_start Trainer Feature dispatch.",
                        "Complete lifecycle remains broader. Java matches Python's relative cleanup ordering, but initial send-out execution, generic round_start Feature semantics, Air Lock, Arena Trap, Intimidate, Impostor and other Python round behavior remain pending. Minecraft must not supply currentRound, declared actions, cleanup ordering, temporary AP grants, Trainer action reset, send-out decisions, temporary-effect metadata, delayed maturity, queue/RNG mutation or action/frequency bookkeeping."));

        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "DelayedHitBindingResolver expands stale target-id anchors and position-only delayed requests through EffectiveMoveTargetResolver. Stored target_position remains an authoritative aim anchor rather than forcing TILE semantics; current geometry, footprints, line of sight and HP eligibility determine affected combatants in stable order, while live target IDs follow their current authoritative position.",
                        "This is bounded delayed-hit execution, not complete move-special coverage. Minecraft must not choose delayed targets, rewrite target mode, precompute affected tiles, supply RNG/combat inputs, consume or refund move frequency/actions, or execute unported move specials."));

        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState owns duration-bearing FieldEffectEntry state. FieldRoundLifecycleHook executes FieldRoundProgression, updates the authoritative environment, emits FieldEffectEndedEvent and applies FieldStatusCleanupRequest before delayed-hit maturity.",
                        "This is partial field-system support. Full terrain effects, weather progression, hazards, zones, reactions and forced movement remain incomplete. Minecraft must not create PTU field entries or perform Wonder Room cleanup."));

        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Initiative-time ability behavior and delayed-hit combat preparation read authoritative runtime state and rerun authoritative move/damage/post-damage hooks during ROUND_START.",
                        "The complete PTU ability library is not ported; Minecraft must not grant abilities or supply hook results."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState owns Features, base AP, temporary AP grants, ActionBudget, initiative inputs, explicit initiative Speed and team identity. BattleRuntimeState owns opaque declared-action records. RoundTrainerFeatureLifecyclePolicy defines send-out/initiative/Feature ordering. TrainerFeaturePrerequisiteResolution ports Python-parity feature/edge/known-feature discovery plus class, subclass, level and required-feature gates. TrainerFeatureContextResolution now ports actor scope, phase/action/move filters, actor-active, round/damage, once-per-actor-per-round and chance gates with PythonRandom parity from authoritative context observations.",
                        "Only bounded Trainer Feature discovery, prerequisite, context, skill, initiative, AP and lifecycle contracts exist. Frequency/cooldowns, resource checks, usage mutation, target scopes and broad effect application remain incomplete. Minecraft must not grant Features, decide prerequisites or context gates, choose AP grant expiry/source, create/clear declared actions, dispatch round_start Features, spend/restore AP, supply Feature RNG outcomes or execute perks."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleChoice is the legal decision contract. RuntimeAutobattlerActionSpace.legalChoices derives ShiftChoice and MoveChoice from BattleRuntimeState-owned position, geometry, affiliation, active/HP state, moveset, move-frequency usage, movement profile and ActionBudget, then returns an immutable stable-key-sorted list. Effective target collection remains server-owned through EffectiveMoveTargetResolver.",
                        "A client, AI or Minecraft adapter may select only from the current core-produced BattleChoice list. It must not manufacture a choice, grant a move, bypass frequency/action budget, replace targeting/range/LoS legality or prefilter effective targets. Tactical scoring remains separate and incomplete."));

        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Fabric 1.21.1 and Cobblemon 1.7.3 boot together in a production-remapped dedicated-server CI runtime. Server-side UUID lookup resolves only live Cobblemon PokemonEntity handles. Runtime smoke coverage spawns real PokemonEntity fixtures, binds opaque presentation UUIDs through PresentationEntityHandleRegistry/RegistryBackedPresentationEntityGateway, applies an authoritative WorldBlockCoordinate relocation and verifies the resulting server position, then separately sends an authoritative positive EntityBoundBattleHealthProjection and verifies the exact live Cobblemon HP mirror. The public cancelable Cobblemon BATTLE_STARTED_PRE hook is now exercised against a real BattleRegistry.startBattle call: the adapter claims only the opaque battle ID, cancels the pre-event, verifies an ErroredBattleStart, verifies the battle never enters BattleRegistry and verifies BATTLE_STARTED_POST never fires.",
                        "Live adapter support remains partial. AutoPTU reservation creation from intercepted participants, canonical participant mapping, zero-HP/faint presentation, move animation, semantic cues, full entity lifecycle and complete battle playback remain pending. Cobblemon HP and other entity data are write-through presentation state only and must never flow back into PTU authority."));

        return Map.copyOf(result);
    }
}
