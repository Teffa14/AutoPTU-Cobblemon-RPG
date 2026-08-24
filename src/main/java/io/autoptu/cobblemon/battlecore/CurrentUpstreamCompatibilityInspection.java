package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and bounded current evidence.
 * This supplements, but never broadens, the permanent support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "14662fb67778e71f2d55fc7a74c43dd9a8b06fa1";
    public static final String AUTOPTU_PYTHON_SHA = "0b9da120608343e286e93fa38daa8ecaaf4b5893";

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
                        "EffectiveMoveTargetResolver derives affected tiles, current authoritative combatant positions, footprint overlap, line of sight and stable candidate order from BattleRuntimeState. Current HP eligibility excludes hp <= 0 while preserving inactive positive-HP candidates, matching the inspected Python collector contract. RuntimeAreaMoveTargeting revalidates legal TILE choices against the live canonical moveset, frequency state and action space before expanding effective targets.",
                        "Minecraft must not supply effective target lists, live target anchors, footprint overlap, line-of-sight results, HP eligibility filters or a generic active-state filter. Target eligibility remains core-owned."));

        result.put(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleRuntimeState supplies canonical initiative inputs. RuntimeInitiativeOrderAssembly.fromState and InitiativeRoundRebuilder.authoritative own ordering and rollover. DelayedHitResourcePolicy preserves the originating action and frequency spend during ROUND_START. TrainerRuntimeState owns its ActionBudget and temporary AP grants; ROUND_START expires due temporary AP before resetting Trainer actions. The multi-target move execution contract freezes one declaration-level action/frequency spend outside the sequential per-target resolution loop.",
                        "Minecraft must not provide a precomputed rollover order, delayed-hit RNG, delayed-hit queue mutation, action/frequency bookkeeping, Trainer action-reset state, temporary AP grants/expiry, a Trainer ID that collides with a combatant ID, or per-target resource consumption for an area move."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "ROUND_START executes FieldRoundLifecycleHook at order 10, DelayedHitRoundLifecycleHook at order 20, RoundTemporaryEffectExpiryHook at order 30, server-owned TemporaryApGrant expiry and Trainer action reset at order 40, Pokemon round-temporary-effect cleanup at order 45, then DeclaredActionRoundLifecycleHook at order 50. BattleRuntimeState owns immutable DeclaredActionState. RoundTrainerFeatureLifecyclePolicy freezes the Python guards around declaration cleanup, initial send-out, initiative and round_start Trainer Feature dispatch.",
                        "Complete lifecycle remains broader. Java matches Python's relative cleanup ordering, but initial send-out execution, generic round_start Feature semantics, Air Lock, Arena Trap, Intimidate, Impostor and other Python round behavior remain pending. Minecraft must not supply currentRound, declared actions, cleanup ordering, temporary AP grants, Trainer action reset, send-out decisions, temporary-effect metadata, delayed maturity, queue/RNG mutation or action/frequency bookkeeping."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "AutoPTU-Java main freezes the Python multi-target execution ownership contract: effective targets resolve sequentially while declaration-level action marking, move-frequency validation/consumption and move-used bookkeeping happen once outside the target loop. Authoritative TILE target expansion is merged on main.",
                        "AutoPTU-Java main at the inspected SHA does not yet contain the open #170 authoritative multi-target runtime executor. The integration must therefore keep AoE damage playback fail-closed. Minecraft must not loop targets, roll accuracy/damage, mutate HP/history or spend per-target resources."));

        result.put(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "StatusStateStore preserves ordered stacked StatusEntry values, including repeated normalized names when a rule explicitly stacks them. Name-based views remain unique. TrainerFeatureEffectRegistry has Python-parity apply_status/remove_status effects over that canonical store. AutoPTU-Java main binds StatusAbilityPreventionResolver into live status application using target-owned RuntimeCombatantState ability suppression for Inner Focus/Flinch, Immunity/poison and Insomnia or Vital Spirit/sleep. Canonical Safeguard prevention is live after target ability prevention with authoritative Infiltrator bypass. Spatial status prevention for Aroma Veil, Aroma Veil [Errata], Pastel Veil and Sweet Veil uses canonical active/fainted state, ability suppression, combatant geometry and footprint distance and emits generic RuleEffectEvent status_block cues from the authoritative blocker.",
                        "Selected target-owned, Safeguard and spatial ability prevention are live, but complete status ticking, expiry, cures, remaining immunities, source-sensitive behavior and interactions are still partial. Safeguard charge consumption/removal and broader blessing-bypass semantics remain outside the proven boundary. Minecraft may persist and transport authoritative status entries and render emitted status_block cues, but must not evaluate radius, suppression, affiliation or missing status rules."));

        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "DelayedHitBindingResolver expands stale target-id anchors and position-only delayed requests through EffectiveMoveTargetResolver. Stored target_position remains an authoritative aim anchor rather than forcing TILE semantics; current geometry, footprints, line of sight and HP eligibility determine affected combatants in stable order, while live target IDs follow their current authoritative position. Authoritative TILE target expansion is also merged for current area declarations.",
                        "This is bounded delayed-hit execution plus authoritative area target expansion, not complete move-special coverage. Minecraft must not choose delayed targets, rewrite target mode, precompute affected tiles, supply RNG/combat inputs, consume or refund move frequency/actions, execute unported move specials, or execute AoE targets itself."));

        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState owns duration-bearing FieldEffectEntry state. FieldRoundLifecycleHook executes FieldRoundProgression, updates the authoritative environment, emits FieldEffectEndedEvent and applies FieldStatusCleanupRequest before delayed-hit maturity. The inspected main also contains the authoritative PRE-damage reaction pipeline and reaction movement context contracts.",
                        "This is partial field/reaction support. Full terrain effects, weather progression, hazards, zones, reactions and forced movement remain incomplete. Minecraft must not create PTU field entries, perform Wonder Room cleanup, infer reaction legality or manufacture push/pull/knockback."));

        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "AutoPTU-Java main includes merged Mirror Armor reflection through the generic CombatStagePreventionHookRegistry and CombatStageMutationService, spatial status prevention, post-damage ability hooks and the authoritative PRE-damage reaction pipeline. Mirror Armor legality checks ability suppression, blocks the original negative combat-stage mutation, emits a generic RuleEffectEvent reflect cue and recursively re-enters the authoritative combat-stage pipeline with loop suppression. These representative families remain parity-gated against the pinned Python oracle used by AutoPTU-Java.",
                        "Representative ability families do not establish complete ability parity. Minecraft must not decide reflection eligibility, ability suppression, recursive-hook suppression, target/source reversal, movement reactions, stat identity, applied delta or damage adjustments; it may only render generic semantic events and authoritative state emitted by Java."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState owns Feature identities, base AP, temporary AP grants, ActionBudget, initiative inputs, explicit initiative Speed, team identity, generic Feature resources and usage/cooldown bookkeeping. TrainerFeaturePrerequisiteResolution, TrainerFeatureContextResolution, TrainerFeatureFrequencyResolution, TrainerFeatureResourceResolution and TrainerFeatureUsageResolution are Python-parity backed. TrainerFeatureExecutionService.executeAuthoritative commits generic bookkeeping only after the effect reports applied. TrainerFeatureTargetResolution and TrainerFeatureTrainerTargetResolution own generic target scopes. TrainerFeatureEffectRegistry includes bounded heal/heal_active, raise_cs, grant_temp_hp, grant_ap, apply_status and remove_status families. The current Python main at 0b9da120 was inspected read-only; its recent changes are Career/deploy work and do not promote the wider Trainer Feature library.",
                        "Trainer Features remain partial. The current families do not cover the wider Python effect library, every trigger/lifecycle binding, full Feature catalog or all AP/resource semantics. Temporary HP damage absorption still belongs to the incomplete stateful damage pipeline. Minecraft must not grant Features, decide gates, select or rewrite targets, execute Feature effects, mutate usage/cooldowns, spend resources/AP or report a Feature as applied."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleChoice is the legal decision contract. RuntimeAutobattlerActionSpace.legalChoices derives ShiftChoice and MoveChoice from BattleRuntimeState-owned position, geometry, affiliation, active/HP state, moveset, move-frequency usage, movement profile and ActionBudget, then returns an immutable stable-key-sorted list. Effective target collection remains server-owned through EffectiveMoveTargetResolver, and TILE choices are revalidated before authoritative target expansion.",
                        "A client, AI or Minecraft adapter may select only from the current core-produced BattleChoice list. It must not manufacture a choice, grant a move, bypass frequency/action budget, replace targeting/range/LoS legality or prefilter effective targets. Tactical scoring remains separate and incomplete."));

        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Fabric 1.21.1 and Cobblemon 1.7.3 boot together in production-remapped dedicated-server CI. Live smokes verify UUID lookup, authoritative relocation, positive HP mirroring and public BATTLE_STARTED_PRE preemption. Player identity is authenticated through PlayerManager and durable player/profile/Pokemon state. Server-owned WILD blueprints and world-scoped encounter correlation feed trusted roster preparation. Generic rule-effect playback preserves authoritative semantic source/target/move/effect data without mechanic-specific adapter branches. Multi-target execution remains guarded behind upstream authority.",
                        "A successful authenticated campaign-path graphical client encounter is still pending. WILD blueprint/correlation registries remain lifecycle-scoped rather than restart-durable, provisioning seed composition remains pending, RuntimeCombatantState materialization, zero-HP/faint presentation and complete battle playback remain incomplete. AoE damage stays disabled until authoritative multi-target execution is merged into AutoPTU-Java main and the integration consumes its semantic event stream without re-running PTU rules."));

        return Map.copyOf(result);
    }
}
