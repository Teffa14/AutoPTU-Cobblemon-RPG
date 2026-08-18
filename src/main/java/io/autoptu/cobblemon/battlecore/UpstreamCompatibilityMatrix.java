package io.autoptu.cobblemon.battlecore;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Executable compatibility checklist for the currently inspected AutoPTU-Java contract.
 *
 * Adapter code must only expose PTU behavior that the upstream battle core owns. PARTIAL
 * categories may be consumed only through the concrete contracts listed here; Minecraft,
 * Cobblemon, and Craftics adapters must not fill the missing rules themselves.
 */
public final class UpstreamCompatibilityMatrix {
    public static final String AUTOPTU_JAVA_SHA = "7b0fac33d139d8bd72b265aa00bb939e895d5a9a";
    public static final String AUTOPTU_PYTHON_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    public enum Capability {
        CORE_TARGETING,
        CORE_MOVEMENT_LEGALITY,
        COMPLETE_MOVEMENT_BEHAVIOR,
        CORE_CALCULATIONS_AND_COMBAT_STATS,
        ACTION_ECONOMY_AND_INITIATIVE,
        FULL_TURN_ROUND_LIFECYCLE,
        FULL_STATEFUL_DAMAGE_PIPELINE,
        COMPLETE_STATUS_LIFECYCLE,
        TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS,
        MOVE_SPECIFIC_BEHAVIOR,
        ABILITIES,
        ITEMS,
        TRAINER_FEATURES_AND_PERKS,
        AI_LEGAL_ACTION_INFRASTRUCTURE,
        AI_TACTICAL_SCORING_POLICY,
        MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK
    }

    public enum Support {
        VERIFIED,
        PARTIAL,
        BLOCKING
    }

    public record Entry(Support support, String contracts, String adapterPolicy) {
        public Entry {
            support = Objects.requireNonNull(support, "support");
            if (contracts == null || contracts.isBlank()) throw new IllegalArgumentException("contracts are required");
            if (adapterPolicy == null || adapterPolicy.isBlank()) throw new IllegalArgumentException("adapterPolicy is required");
        }
    }

    private static final Map<Capability, Entry> ENTRIES = buildEntries();

    private UpstreamCompatibilityMatrix() {
    }

    public static Entry entry(Capability capability) {
        Entry entry = ENTRIES.get(Objects.requireNonNull(capability, "capability"));
        if (entry == null) throw new IllegalStateException("unmapped upstream capability: " + capability);
        return entry;
    }

    public static Map<Capability, Entry> entries() {
        return ENTRIES;
    }

    public static boolean mayProjectAuthoritativeBehavior(Capability capability) {
        return entry(capability).support() != Support.BLOCKING;
    }

    private static Map<Capability, Entry> buildEntries() {
        EnumMap<Capability, Entry> entries = new EnumMap<>(Capability.class);
        entries.put(Capability.CORE_TARGETING, verified(
                "MoveSpec, target anchors, footprints, range/area legality, line-of-sight tests",
                "Project grid inputs and render authoritative legal targets only."));
        entries.put(Capability.CORE_MOVEMENT_LEGALITY, verified(
                "MovementGrid, MovementProfile, Shift/Jump legality, Overland/Swim/Sky and terrain-cost tests",
                "Project world geometry into core movement DTOs; never decide PTU path legality in Minecraft."));
        entries.put(Capability.COMPLETE_MOVEMENT_BEHAVIOR, blocking(
                "Forced movement, push/pull/knockback, interception and interaction-driven movement are not complete",
                "Do not synthesize forced movement, interception or knockback rules in the adapter."));
        entries.put(Capability.CORE_CALCULATIONS_AND_COMBAT_STATS, verified(
                "Damage Base tables, type effectiveness, STAB, accuracy/evasion, combat stages and CombatantStatProfile",
                "Supply canonical inputs only and render resolved values/results."));
        entries.put(Capability.ACTION_ECONOMY_AND_INITIATIVE, verified(
                "ActionBudget, typed turn phases, deterministic initiative, Trick Room/League ordering",
                "Treat action availability and ordering as core-owned state."));
        entries.put(Capability.FULL_TURN_ROUND_LIFECYCLE, partial(
                "BattleRoundController round increment/reset semantics plus existing turn/action budget contracts",
                "Use verified round-start semantics only; defer unported lifecycle hooks and end-of-round effects."));
        entries.put(Capability.FULL_STATEFUL_DAMAGE_PIPELINE, partial(
                "RuntimeMoveResolution, authoritative stats/types/statuses and ordered DamageModifierHookRegistry; Burn is routed through the registry",
                "Consume resolved damage/events; do not inject ability/item/terrain modifiers that the registry has not ported."));
        entries.put(Capability.COMPLETE_STATUS_LIFECYCLE, partial(
                "Canonical statuses, Burn damage penalty, Sleep/Paralysis/Freeze evasion effects, status skips and selected Trainer Feature exceptions",
                "Expose only parity-backed status effects; do not implement missing ticks, cures or interactions client-side."));
        entries.put(Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS, partial(
                "Terrain movement costs and weather calculation primitives exist; HookSource reserves terrain/weather/hazard/zone/reaction origins",
                "Project terrain facts only. Do not approximate hazards, zones, reactions or forced movement before core support exists."));
        entries.put(Capability.MOVE_SPECIFIC_BEHAVIOR, partial(
                "Authoritative movesets, MoveSpec/MoveCombatProfile metadata and move-frequency enforcement",
                "Send requested move identity only; defer unported move specials to the core."));
        entries.put(Capability.ABILITIES, blocking(
                "HookSource.ABILITY exists as registry architecture but no complete parity-backed ability library is present",
                "Do not implement ability battle effects in Minecraft."));
        entries.put(Capability.ITEMS, blocking(
                "HookSource.ITEM exists as registry architecture; integration has canonical item reservations but core battle item effects are incomplete",
                "Reserve/commit canonical items server-side but do not manufacture battle modifiers."));
        entries.put(Capability.TRAINER_FEATURES_AND_PERKS, partial(
                "Selected status-skip Trainer Feature exceptions plus TRAINER_FEATURE/PERK hook-source categories",
                "Use only specifically verified Feature behavior; keep all other perks/features deferred."));
        entries.put(Capability.AI_LEGAL_ACTION_INFRASTRUCTURE, verified(
                "Runtime-authoritative autobattler action space, affiliation, geometry, action budget, move availability and frequency filtering",
                "AI may choose only from the legal choices produced by the core."));
        entries.put(Capability.AI_TACTICAL_SCORING_POLICY, blocking(
                "Full Python-equivalent tactical scoring/policy is not yet ported",
                "Do not claim Python AI parity or move tactical policy into the Minecraft adapter."));
        entries.put(Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK, blocking(
                "Architecture boundary exists but no verified live Minecraft/Cobblemon/Craftics runtime adapter/playback is present",
                "Keep headless DTO/authority tests separate from claims about in-game behavior."));

        if (entries.size() != Capability.values().length) {
            throw new IllegalStateException("compatibility matrix must cover every upstream capability");
        }
        return Collections.unmodifiableMap(entries);
    }

    private static Entry verified(String contracts, String policy) {
        return new Entry(Support.VERIFIED, contracts, policy);
    }

    private static Entry partial(String contracts, String policy) {
        return new Entry(Support.PARTIAL, contracts, policy);
    }

    private static Entry blocking(String contracts, String policy) {
        return new Entry(Support.BLOCKING, contracts, policy);
    }
}
