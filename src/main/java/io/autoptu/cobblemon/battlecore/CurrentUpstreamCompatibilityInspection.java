package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and bounded current evidence.
 * This supplements, but never broadens, the permanent support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "cdb229db787ac93f28745f796c1d9944546676cc";
    public static final String AUTOPTU_PYTHON_SHA = "0db989a259f84d04e7fdcb161bb986bc6ef69275";

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
                        "StatusStateStore now preserves ordered stacked StatusEntry values, including repeated normalized names when a rule explicitly stacks them. Name-based views remain unique. Upstream parity tests freeze append, first-match replacement, remove-one, remove-all and clear storage behavior against the Python list representation. Current TrainerFeatureEffectRegistry also has Python-parity apply_status/remove_status effects over that canonical store.",
                        "This verifies representation/storage semantics only plus bounded Trainer Feature status mutations. Complete status ticking, expiry, cures, immunities, source-sensitive behavior and remaining interactions are still partial. Minecraft may persist and transport authoritative status entries but must not interpret or execute missing status rules."));

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
                        "Initiative-time ability behavior and delayed-hit combat preparation read authoritative runtime state and rerun authoritative move/damage/post-damage hooks during ROUND_START. A draft upstream contract now freezes declarative status-prevention ability mapping, but that draft does not change live status application and is not treated as runtime support.",
                        "The complete PTU ability library is not ported; status prevention from the draft contract is not yet runtime-authoritative, and Minecraft must not grant abilities or supply hook results."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState owns Feature identities, base AP, temporary AP grants, ActionBudget, initiative inputs, explicit initiative Speed, team identity, generic Feature resources and usage/cooldown bookkeeping. TrainerFeaturePrerequisiteResolution, TrainerFeatureContextResolution, TrainerFeatureFrequencyResolution, TrainerFeatureResourceResolution and TrainerFeatureUsageResolution are Python-parity backed. TrainerFeatureExecutionService.executeAuthoritative commits generic bookkeeping to TrainerRuntimeState only after the effect reports applied. TrainerFeatureTargetResolution provides Python-parity generic Pokemon target scopes and TrainerFeatureTrainerTargetResolution provides parity-backed trainer-target scopes. TrainerFeatureEffectRegistry includes heal/heal_active, raise_cs, grant_temp_hp, grant_ap, apply_status and remove_status families against authoritative runtime state. The inspected Python oracle still applies the effect before resource consumption and usage/cooldown bookkeeping.",
                        "Trainer Features remain partial. The current families do not cover the wider Python effect library, every trigger/lifecycle binding, full Feature catalog or all AP/resource semantics. Temporary HP damage absorption still belongs to the incomplete stateful damage pipeline. Minecraft must not grant Features, decide gates, select or rewrite targets, execute Feature effects, mutate usage/cooldowns, spend resources/AP or report a Feature as applied."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleChoice is the legal decision contract. RuntimeAutobattlerActionSpace.legalChoices derives ShiftChoice and MoveChoice from BattleRuntimeState-owned position, geometry, affiliation, active/HP state, moveset, move-frequency usage, movement profile and ActionBudget, then returns an immutable stable-key-sorted list. Effective target collection remains server-owned through EffectiveMoveTargetResolver.",
                        "A client, AI or Minecraft adapter may select only from the current core-produced BattleChoice list. It must not manufacture a choice, grant a move, bypass frequency/action budget, replace targeting/range/LoS legality or prefilter effective targets. Tactical scoring remains separate and incomplete."));

        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Fabric 1.21.1 and Cobblemon 1.7.3 boot together in a production-remapped dedicated-server CI runtime. Live smokes verify server-side PokemonEntity UUID lookup, authoritative relocation and positive HP mirroring. Public BATTLE_STARTED_PRE interception prevents Cobblemon registration/post-start. The identity-only handoff maps opaque actor/Pokemon UUIDs to independently server-owned canonical participant/combatant IDs and atomically reserves opposing canonical rosters. Player-versus-wild authority composition binds Trainer/item/arena reservation and multi-side roster reservation under one server-issued reservation ID and RNG seed. FabricAuthenticatedPlayerContextResolver requires the PLAYER actor UUID to resolve through the real MinecraftServer PlayerManager before a separate canonical PTU context source may be queried. CanonicalPlayerMutationService adds revision-guarded server-owned writes. FileVersionedCanonicalStateRepository supplies schema-versioned durable single-player storage with deterministic file identity, one CAS winner across repository instances and required atomic replacement. FabricCanonicalPlayerStoreRuntime binds world-scoped canonical storage to the real Minecraft save root and Fabric lifecycle and opens the player, encounter-profile, FileCanonicalPokemonRepository and FileCanonicalItemReservationRepository stores together. CanonicalPlayerEncounterProfile plus FileCanonicalPlayerEncounterProfileRepository persist a server-owned roster/consumable/arena selection with revision CAS. PersistentCanonicalPlayerEncounterContextSource.fromWorldRuntime now resolves those exact world-scoped player/profile repositories, FabricAuthenticatedPlayerContextResolver.persistentWorld composes the real PlayerManager authentication gate with that durable source, and CobblemonPlayerVsWildClaimCoordinator.persistentWorld composes the result with the existing authoritative player-versus-wild reservation service. BattleAuthorityService still re-resolves every Pokemon and item and verifies ownership and quantities before reservation. FileCanonicalItemReservationRepository persists canonical item identity/ownership/template/quantity/revision together with one active reservation and commits or releases that reservation through one atomically replaced item file across repository restart. FileCanonicalPokemonRepository persists complete CanonicalPokemonState aggregates with revision CAS, process/OS locking, required atomic replacement and lossless ordered stacked-status metadata. The two-process dedicated-server restart smoke requires Trainer, encounter profile, Pokemon aggregate, item quantity and an active item reservation to survive reopening the same world.",
                        "Live adapter support remains partial. A successful authenticated graphical client encounter and automatic live identity/profile provisioning are still pending. Cross-aggregate transaction recovery, transaction journaling and reservation reconciliation/expiry remain pending. RuntimeCombatantState materialization, zero-HP/faint presentation, move animation, semantic cues, full entity lifecycle and complete battle playback remain pending. ServerPlayerEntity and Cobblemon entity state are identity/presentation inputs only and never supply PTU stats, inventory truth, modifiers, legality or outcomes."));

        return Map.copyOf(result);
    }
}
