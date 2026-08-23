package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and bounded current evidence.
 * This supplements, but never broadens, the permanent support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "dbc1bfb14c0e0036c1cc3301d35355d36611bf4b";
    public static final String AUTOPTU_PYTHON_SHA = "8108e0d2b876414a5e62c2021801a3692cda05b8";

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
                        "ROUND_START executes the verified field, delayed-hit, temporary-effect, Trainer action/AP and declared-action cleanup sequence through authoritative runtime state.",
                        "Complete lifecycle remains broader. Initial send-out execution, generic round_start Feature semantics, remaining status/ability/perk hooks and other Python round behavior remain incomplete. Minecraft must not manufacture missing lifecycle behavior."));

        result.put(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "StatusStateStore now preserves ordered stacked StatusEntry values, including repeated normalized names when a rule explicitly stacks them. Name-based views remain unique, and upstream parity tests freeze append, first-match replacement, remove-one, remove-all and clear storage semantics against Python list behavior.",
                        "This verifies representation/storage semantics only. Complete status ticking, expiry, cures, immunities, source-sensitive behavior and all interactions remain partial. Minecraft may persist and transport authoritative status entries but must not interpret or execute those rules."));

        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "DelayedHitBindingResolver expands stale target-id anchors and position-only delayed requests through EffectiveMoveTargetResolver using authoritative geometry, footprints, line of sight and HP eligibility.",
                        "This is bounded delayed-hit execution, not complete move-special coverage. Minecraft must not choose delayed targets, rewrite target mode, precompute affected tiles, supply RNG/combat inputs, consume or refund move frequency/actions, or execute unported move specials."));

        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState owns duration-bearing field state and verified round progression/cleanup seams.",
                        "Full terrain effects, weather progression, hazards, zones, reactions and forced movement remain incomplete. Minecraft must not create PTU field entries or manufacture missing environment semantics."));

        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Initiative-time ability behavior and selected damage/post-damage hooks read authoritative runtime state and emit authoritative results.",
                        "The complete PTU ability library is not ported; Minecraft must not grant abilities or supply hook results."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState owns Feature identities, AP, resources and usage/cooldown bookkeeping. Generic prerequisite/context/frequency/resource/usage resolution, Pokemon and trainer target scopes, and bounded heal, raise_cs, temporary-HP and AP-grant effect families are parity-backed. The current Python oracle still applies an effect before resource consumption and usage/cooldown bookkeeping.",
                        "Trainer Features remain partial. The bounded effect families do not cover the wider Python effect library, every trigger/lifecycle binding, full catalog or all resource semantics. Minecraft must not grant Features, decide gates, select targets, execute effects, mutate usage/cooldowns or spend resources/AP."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleChoice is the legal decision contract. RuntimeAutobattlerActionSpace.legalChoices derives immutable stable-key-sorted choices from BattleRuntimeState-owned legality and EffectiveMoveTargetResolver.",
                        "A client, AI or Minecraft adapter may select only from the current core-produced BattleChoice list. Tactical scoring remains separate and incomplete."));

        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Fabric/Cobblemon production smokes verify UUID lookup, authoritative relocation/HP projection, public battle-start preemption, identity mapping, canonical reservations, authenticated player presence, revisioned Player persistence, durable encounter profiles and durable item reservations. This slice adds schema-versioned complete CanonicalPokemonState persistence with revision CAS, process/OS locking, required atomic replacement and lossless ordered stacked-status metadata.",
                        "Live adapter support remains partial. The successful authenticated graphical client encounter, Fabric lifecycle wiring for Pokemon/items, cross-aggregate transaction recovery, reservation reconciliation/expiry, RuntimeCombatantState materialization and complete battle playback remain pending. Minecraft/Cobblemon state never supplies PTU truth."));

        return Map.copyOf(result);
    }
}
