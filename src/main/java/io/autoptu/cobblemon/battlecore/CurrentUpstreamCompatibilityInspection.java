package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and bounded current evidence.
 * This supplements, but never broadens, the permanent support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "14662fb67778e71f2d55fc7a74c43dd9a8b06fa1";
    public static final String AUTOPTU_PYTHON_SHA = "cd4668a1b0e7c995bc12f3768f7b04cfa0f1c896";

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
                        "EffectiveMoveTargetResolver derives affected tiles, current authoritative combatant positions, footprint overlap, line of sight and stable candidate order from BattleRuntimeState. RuntimeAreaMoveTargeting revalidates legal TILE choices against the live canonical moveset, frequency state and action space before expanding effective targets.",
                        "Minecraft must not supply effective target lists, live target anchors, footprint overlap, line-of-sight results, HP eligibility filters or a generic active-state filter. Target eligibility remains core-owned."));

        result.put(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleRuntimeState supplies canonical initiative inputs. RuntimeInitiativeOrderAssembly.fromState and InitiativeRoundRebuilder.authoritative own ordering and rollover. The multi-target move execution contract freezes one declaration-level action/frequency spend outside the sequential per-target resolution loop.",
                        "Minecraft must not provide a precomputed rollover order, action/frequency bookkeeping, Trainer action-reset state, temporary AP grants/expiry, or per-target resource consumption for an area move."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "ROUND_START executes authoritative field progression, delayed-hit maturity, temporary-effect expiry, Trainer AP/action reset and declared-action cleanup in frozen order. Initiative rebuild, active actor/phase state and selected phase/status/ability/perk hooks are core-owned.",
                        "Complete lifecycle remains broader. Initial send-out execution, generic round_start Feature semantics, Air Lock, Arena Trap, Intimidate, Impostor and other Python round behavior remain pending. Minecraft must not advance currentRound, run lifecycle hooks or manufacture missing round effects."));

        result.put(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "AutoPTU-Java main freezes the Python multi-target execution ownership contract: effective targets resolve sequentially while declaration-level action marking, move-frequency validation/consumption and move-used bookkeeping happen once outside the target loop. Authoritative TILE target expansion is merged on main.",
                        "AutoPTU-Java main at the inspected SHA does not yet contain the open #170 authoritative multi-target runtime executor. The integration must therefore keep AoE damage playback fail-closed. Minecraft must not loop targets, roll accuracy/damage, mutate HP/history or spend per-target resources."));

        result.put(UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "StatusStateStore preserves ordered stacked StatusEntry values. Live status application includes selected target-owned ability prevention, Safeguard with Infiltrator bypass and spatial Aroma/Pastel/Sweet prevention using canonical battle geometry and ability suppression.",
                        "Complete status ticking, expiry, cures, remaining immunities, source-sensitive behavior and interactions remain partial. Minecraft must not evaluate radius, suppression, affiliation or missing status rules."));

        result.put(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Delayed-hit targeting/execution and authoritative TILE target expansion are present. Move frequency and target semantics remain core-owned.",
                        "Complete move-special coverage remains absent. Minecraft must not infer keyword/special semantics, choose delayed targets or execute unported move behavior."));

        result.put(UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "BattleEnvironmentState owns duration-bearing field effects and authoritative terrain/weather inputs used by verified runtime slices.",
                        "Full terrain effects, weather progression, hazards, zones, reactions and forced movement remain incomplete. Minecraft must not create PTU field state from presentation observations."));

        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "The inspected AutoPTU-Java main includes generic combat-stage prevention/reflection, spatial status prevention, post-damage ability hooks and the authoritative PRE-damage reaction pipeline with Telepathy-related movement/context contracts merged before the multi-target execution contract.",
                        "Representative ability families do not establish complete ability parity. Minecraft must only render core-emitted semantic events and must not evaluate ability legality, suppression, movement reactions or damage adjustments."));

        result.put(UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "TrainerRuntimeState owns Feature identities, AP, resources and usage/cooldown bookkeeping. Generic prerequisite, context, frequency, resource, usage, target and bounded effect contracts are parity-backed.",
                        "The current Python main at cd4668a1 was inspected read-only and its recent changes are Career/deploy work; they do not promote the wider Trainer Feature library. Minecraft must not grant Features, decide gates, select targets, mutate bookkeeping or report Feature application."));

        result.put(UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "BattleChoice and RuntimeAutobattlerActionSpace derive legal decisions from canonical runtime state. TILE choices are revalidated before authoritative target expansion.",
                        "AI or Minecraft may choose only from the current core-produced choices. Tactical scoring remains separate and incomplete."));

        result.put(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Fabric/Cobblemon dedicated-server smokes, canonical identity/reservation/persistence paths, generic semantic rule-effect playback and the bounded graphical battle harness are present. Multi-target execution remains guarded behind upstream authority.",
                        "Authenticated campaign-path graphical playback and complete battle presentation remain incomplete. AoE damage stays disabled until authoritative multi-target execution is merged into AutoPTU-Java main and the integration consumes its semantic event stream without re-running PTU rules."));

        return Map.copyOf(result);
    }
}
