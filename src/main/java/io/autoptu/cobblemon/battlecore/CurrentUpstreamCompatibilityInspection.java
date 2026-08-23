package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and bounded current evidence.
 * This supplements, but never broadens, the permanent support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "45feae6161c9b92ccb008a60d9b6e16dcbc0c377";
    public static final String AUTOPTU_PYTHON_SHA = "cd2d31ab9438713629ad3fc65939e8cc622b5a1f";

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
                        "StatusStateStore preserves ordered stacked StatusEntry values, including repeated normalized names when a rule explicitly stacks them. Name-based views remain unique. TrainerFeatureEffectRegistry has Python-parity apply_status/remove_status effects over that canonical store. AutoPTU-Java main binds StatusAbilityPreventionResolver into live status application using target-owned RuntimeCombatantState ability suppression for Inner Focus/Flinch, Immunity/poison and Insomnia or Vital Spirit/sleep. Merged PR #155 wires canonical Safeguard prevention into live status application after target ability prevention and honors the authoritative Infiltrator bypass. Merged PR #156 adds live spatial status prevention for Aroma Veil, Aroma Veil [Errata], Pastel Veil and Sweet Veil through canonical active/fainted state, ability suppression, combatant geometry and footprint distance, emitting generic RuleEffectEvent status_block cues from the authoritative blocker.",
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
                        "Merged upstream PR #153 binds StatusAbilityPreventionResolver into live authoritative status application. RuntimeCombatantState owns abilitiesSuppressed; status requests cannot supply or override it. Inner Focus blocks Flinch, Immunity blocks poison, and Insomnia/Vital Spirit block sleep unless the target's abilities are suppressed. Merged PR #155 wires canonical Safeguard prevention into live status application after target ability prevention and honors the authoritative Infiltrator bypass. Merged PR #156 adds spatial status prevention for Aroma Veil, Aroma Veil [Errata], Pastel Veil and Sweet Veil and emits a generic status_block RuleEffectEvent from the authoritative blocking combatant.",
                        "Own Tempo, Oblivious, Run Away, attacker-side ignore_defensive_abilities beyond the verified Infiltrator Safeguard bypass, Safeguard charge consumption/removal, and the sources that create/remove ability suppression remain outside this verified family. Draft PR #157 for Flower Veil combat-stage prevention is not part of inspected main and therefore grants no adapter support. The complete PTU ability library is not ported, and Minecraft must not decide status or combat-stage prevention, radius, blocker selection, suppression, grant abilities or supply hook results."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState owns Feature identities, base AP, temporary AP grants, ActionBudget, initiative inputs, explicit initiative Speed, team identity, generic Feature resources and usage/cooldown bookkeeping. TrainerFeaturePrerequisiteResolution, TrainerFeatureContextResolution, TrainerFeatureFrequencyResolution, TrainerFeatureResourceResolution and TrainerFeatureUsageResolution are Python-parity backed. TrainerFeatureExecutionService.executeAuthoritative commits generic bookkeeping only after the effect reports applied. TrainerFeatureTargetResolution provides Python-parity generic Pokemon target scopes and TrainerFeatureTrainerTargetResolution provides parity-backed trainer-target scopes. TrainerFeatureEffectRegistry includes heal/heal_active, raise_cs, grant_temp_hp, grant_ap, apply_status and remove_status. The inspected Python oracle at cd2d31ab still performs guards and target/effect execution before resource consumption and usage/cooldown bookkeeping.",
                        "Trainer Features remain partial. The current families do not cover the wider Python effect library, every trigger/lifecycle binding, full Feature catalog or all AP/resource semantics. Temporary HP damage absorption still belongs to the incomplete stateful damage pipeline. Minecraft must not grant Features, decide gates, select or rewrite targets, execute Feature effects, mutate usage/cooldowns, spend resources/AP or report a Feature as applied."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleChoice is the legal decision contract. RuntimeAutobattlerActionSpace.legalChoices derives ShiftChoice and MoveChoice from BattleRuntimeState-owned position, geometry, affiliation, active/HP state, moveset, move-frequency usage, movement profile and ActionBudget, then returns an immutable stable-key-sorted list. Effective target collection remains server-owned through EffectiveMoveTargetResolver.",
                        "A client, AI or Minecraft adapter may select only from the current core-produced BattleChoice list. It must not manufacture a choice, grant a move, bypass frequency/action budget, replace targeting/range/LoS legality or prefilter effective targets. Tactical scoring remains separate and incomplete."));

        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Fabric 1.21.1 and Cobblemon 1.7.3 boot together in production-remapped dedicated-server CI. Live smokes verify UUID lookup, authoritative relocation, positive HP mirroring and public BATTLE_STARTED_PRE preemption. Player identity is authenticated through PlayerManager and durable player/profile/Pokemon state. ServerOwnedWildEncounterIdentityBinder accepts only battle/side/actor/Pokemon identity keys from the Cobblemon signal. CanonicalWildEncounterBlueprintSource isolates trusted RPG blueprint lookup behind canonical encounter ID only; WorldScopedCanonicalWildEncounterBlueprintRegistry provides the server-owned create-only implementation attached to the active Fabric world lifecycle. ServerOwnedWildEncounterBlueprintPublisher now copies an already-decided campaign/RPG blueprint into that world registry before any Cobblemon identity is attached and rejects missing, mismatched or duplicate publication. ServerOwnedWildEncounterPreparationService resolves the registered blueprint before attaching the opaque WILD actor correlation and delegates to ServerOwnedWildEncounterProvisioningService. The provisioner creates deterministic canonical WILD participant/combatant IDs and encounter seed, exposes CanonicalEncounterPokemonState through the canonical encounter repository contract, and keeps external WILD actor identity out of PTU values. FileCanonicalPokemonRepository and FileCanonicalItemReservationRepository remain world-scoped durable authority stores; the two-process dedicated-server restart smoke verifies Trainer, encounter profile, Pokemon aggregate, item quantity and active item reservation across world reopen.",
                        "A successful authenticated graphical client encounter is still pending. Concrete campaign/RPG generation policy must supply the trusted source used by ServerOwnedWildEncounterBlueprintPublisher; this integration boundary does not invent encounter content. The blueprint registry is intentionally lifecycle-scoped and is not durable across restart; durable WILD provisioning/recovery remains pending. The deterministic provisioning seed is not yet composed into the later shared battle reservation seed. Encounter-profile creation, cross-aggregate transaction recovery, journaling, reservation expiry/reconciliation, RuntimeCombatantState materialization, zero-HP/faint presentation and complete battle playback remain pending. ServerPlayerEntity and Cobblemon state may provide identity/presentation inputs only and never PTU stats, inventory truth, modifiers, legality or outcomes."));

        return Map.copyOf(result);
    }
}
