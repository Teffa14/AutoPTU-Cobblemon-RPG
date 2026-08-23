package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and bounded current evidence.
 * This supplements, but never broadens, the permanent support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "967b16237c6ea93a939bd4acbbe67da979885a60";
    public static final String AUTOPTU_PYTHON_SHA = "8cf78e737a85f3b57e786154cf0f5781c840624a";

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
                        "EffectiveMoveTargetResolver derives affected tiles, current authoritative combatant positions, footprint overlap, line of sight and stable candidate order from BattleRuntimeState. Current HP eligibility excludes hp <= 0 while preserving inactive positive-HP candidates, matching the inspected Python collector contract.",
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

        result.put(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "StatusStateStore preserves ordered stacked StatusEntry values, including repeated normalized names when a rule explicitly stacks them. Name-based views remain unique. TrainerFeatureEffectRegistry has Python-parity apply_status/remove_status effects over that canonical store. AutoPTU-Java main binds StatusAbilityPreventionResolver into live status application using target-owned RuntimeCombatantState ability suppression for Inner Focus/Flinch, Immunity/poison and Insomnia or Vital Spirit/sleep. Canonical Safeguard prevention is live after target ability prevention with authoritative Infiltrator bypass. Spatial status prevention for Aroma Veil, Aroma Veil [Errata], Pastel Veil and Sweet Veil uses canonical active/fainted state, ability suppression, combatant geometry and footprint distance and emits generic RuleEffectEvent status_block cues from the authoritative blocker.",
                        "Selected target-owned, Safeguard and spatial ability prevention are live, but complete status ticking, expiry, cures, remaining immunities, source-sensitive behavior and interactions are still partial. Safeguard charge consumption/removal and broader blessing-bypass semantics remain outside the proven boundary. Minecraft may persist and transport authoritative status entries and render emitted status_block cues, but must not evaluate radius, suppression, affiliation or missing status rules."));

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
                        "AutoPTU-Java main 967b162 includes merged Mirror Armor reflection through the generic CombatStagePreventionHookRegistry and CombatStageMutationService. For an external negative combat-stage request, the authoritative hook checks ability suppression and Mirror Armor ownership, blocks the original target mutation, emits RuleEffectEvent effect=reflect from the holder to the source, then recursively re-enters the same combat-stage pipeline with only the Mirror Armor hook suppressed to prevent reflection loops. The port is parity-gated against Python oracle 16d228efa63aabecb67fa788959a359aac7f8f03. Existing merged combat-stage prevention also includes Flower Veil and the target-owned prevention family.",
                        "Mirror Armor proves one reflected combat-stage path, not complete ability support. Ability support remains partial and broader rule families still depend on upstream hooks. Minecraft must not decide reflection eligibility, ability suppression, recursive-hook suppression, target/source reversal, stat identity or applied delta; it may only render the generic semantic rule_effect and authoritative state emitted by Java."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState owns Feature identities, base AP, temporary AP grants, ActionBudget, initiative inputs, explicit initiative Speed, team identity, generic Feature resources and usage/cooldown bookkeeping. TrainerFeaturePrerequisiteResolution, TrainerFeatureContextResolution, TrainerFeatureFrequencyResolution, TrainerFeatureResourceResolution and TrainerFeatureUsageResolution are Python-parity backed. TrainerFeatureExecutionService.executeAuthoritative commits generic bookkeeping only after the effect reports applied. TrainerFeatureTargetResolution provides Python-parity generic Pokemon target scopes and TrainerFeatureTrainerTargetResolution provides parity-backed trainer-target scopes. TrainerFeatureEffectRegistry includes heal/heal_active, raise_cs, grant_temp_hp, grant_ap, apply_status and remove_status. The current Python main at 8cf78e73 was inspected read-only; no evidence from that head promotes the wider Trainer Feature library beyond these verified families.",
                        "Trainer Features remain partial. The current families do not cover the wider Python effect library, every trigger/lifecycle binding, full Feature catalog or all AP/resource semantics. Temporary HP damage absorption still belongs to the incomplete stateful damage pipeline. Minecraft must not grant Features, decide gates, select or rewrite targets, execute Feature effects, mutate usage/cooldowns, spend resources/AP or report a Feature as applied."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleChoice is the legal decision contract. RuntimeAutobattlerActionSpace.legalChoices derives ShiftChoice and MoveChoice from BattleRuntimeState-owned position, geometry, affiliation, active/HP state, moveset, move-frequency usage, movement profile and ActionBudget, then returns an immutable stable-key-sorted list. Effective target collection remains server-owned through EffectiveMoveTargetResolver.",
                        "A client, AI or Minecraft adapter may select only from the current core-produced BattleChoice list. It must not manufacture a choice, grant a move, bypass frequency/action budget, replace targeting/range/LoS legality or prefilter effective targets. Tactical scoring remains separate and incomplete."));

        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Fabric 1.21.1 and Cobblemon 1.7.3 boot together in production-remapped dedicated-server CI. Live smokes verify UUID lookup, authoritative relocation, positive HP mirroring and public BATTLE_STARTED_PRE preemption. Player identity is authenticated through PlayerManager and durable player/profile/Pokemon state. Server-owned WILD blueprints are published into the world-scoped create-only registry before opaque Cobblemon identity correlation. The current WILD handoff adds a world-scoped canonical-encounter-to-WILD-actor correlation and a lazy CanonicalWildRosterSource that prepares only the already-published trusted blueprint when BATTLE_STARTED_PRE requests that actor. Generic rule-effect playback preserves authoritative source actor, target, move, effect, amount and HP for ability events, including spatial status_block, combat_stage_block and Mirror Armor reflect without mechanic-specific adapter branches.",
                        "A successful authenticated graphical client encounter is still pending. Concrete campaign/RPG generation policy must supply trusted WILD content and trusted projection code must register the encounter-to-actor correlation before battle start; the adapter must not derive the canonical encounter ID or PTU values from Cobblemon. WILD blueprint/correlation registries remain lifecycle-scoped rather than restart-durable, provisioning seed composition remains pending, and RuntimeCombatantState materialization, zero-HP/faint presentation and complete battle playback remain incomplete. Mirror Armor legality, recursive suppression and stage mutation remain entirely upstream."));

        return Map.copyOf(result);
    }
}
