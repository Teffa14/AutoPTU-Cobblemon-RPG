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
    public static final String AUTOPTU_JAVA_SHA = "28d49949b63f2e675680356e650ac5b04e0c5c6b";
    public static final String AUTOPTU_PYTHON_SHA = "9233a6e4bc159c5d7326a2ec9d828da9f828cffd";

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

    public enum Support { VERIFIED, PARTIAL, BLOCKING }

    public record Entry(Support support, String contracts, String adapterPolicy) {
        public Entry {
            support = Objects.requireNonNull(support, "support");
            if (contracts == null || contracts.isBlank()) throw new IllegalArgumentException("contracts are required");
            if (adapterPolicy == null || adapterPolicy.isBlank()) throw new IllegalArgumentException("adapterPolicy is required");
        }
    }

    private static final Map<Capability, Entry> ENTRIES = buildEntries();

    private UpstreamCompatibilityMatrix() {}

    public static Entry entry(Capability capability) {
        Entry entry = ENTRIES.get(Objects.requireNonNull(capability, "capability"));
        if (entry == null) throw new IllegalStateException("unmapped upstream capability: " + capability);
        return entry;
    }

    public static Map<Capability, Entry> entries() { return ENTRIES; }

    public static boolean mayProjectAuthoritativeBehavior(Capability capability) {
        return entry(capability).support() != Support.BLOCKING;
    }

    private static Map<Capability, Entry> buildEntries() {
        EnumMap<Capability, Entry> entries = new EnumMap<>(Capability.class);
        entries.put(Capability.CORE_TARGETING, verified(
                "MoveSpec, target anchors, footprints, range/area legality, line-of-sight tests and CombatantGeometryState size labels",
                "Project grid inputs and render authoritative legal targets only. Combatant model scale, facing, footprint overlap and placement legality must not be invented by the adapter."));
        entries.put(Capability.CORE_MOVEMENT_LEGALITY, verified(
                "MovementGrid, MovementProfile, Shift/Jump legality, Overland/Swim/Sky and terrain-cost tests",
                "Project adapter-neutral world observations toward core movement inputs; never decide PTU path legality or invent terrain costs in Minecraft."));
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
                "BattleRoundController, ordered LifecycleHookRegistry, payload-bearing TemporaryEffectEntry/TemporaryEffectStore, move-frequency reset, parity-backed round-start cleanup, authoritative round damage-history and injury-history rotation, DelayedHitEntry/DelayedHitQueue scheduling and due/future partition, plus DelayedHitBinding/DelayedHitBindingResolver canonical move/target reconstruction",
                "Consume verified lifecycle state/events only; actual delayed-hit execution, terrain/zone/room advancement, send-out effects, remaining status/ability/Feature hooks, broader payload expiry semantics and other Python lifecycle behavior remain deferred."));
        entries.put(Capability.FULL_STATEFUL_DAMAGE_PIPELINE, partial(
                "RuntimeMoveResolution, authoritative stats/types/statuses, ordered DamageModifierHookRegistry and pre-damage move hooks; Burn, Pink Pearl and Mega Launcher slices are parity-backed",
                "Consume resolved damage and semantic events; never inject unported move/ability/item/terrain modifiers in the adapter."));
        entries.put(Capability.COMPLETE_STATUS_LIFECYCLE, partial(
                "Canonical statuses, Burn damage penalty, Sleep/Paralysis/Freeze evasion effects, status skips and selected Trainer Feature exceptions",
                "Expose only parity-backed status effects; do not implement missing ticks, cures or interactions client-side."));
        entries.put(Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS, partial(
                "Terrain movement costs, weather calculation primitives, generic HookSource categories and lifecycle seams exist",
                "Project raw terrain/world observations and semantic events only. Do not classify Minecraft blocks into PTU terrain or approximate hazards, zones, reactions or forced movement before core contracts exist."));
        entries.put(Capability.MOVE_SPECIFIC_BEHAVIOR, partial(
                "Authoritative movesets, MoveSpec/MoveCombatProfile metadata, move-frequency enforcement, temporary-effect payload state, ordered pre-damage move hook seam, DelayedHitEntry scheduling and DelayedHitBinding canonical move/target inputs",
                "Send requested move identity/target intent only; consume verified move-specific events and state while actual delayed-hit execution and unported specials remain core-owned and deferred."));
        entries.put(Capability.ABILITIES, partial(
                "Canonical ability identities, generic hook source support and parity-backed Mega Launcher pre-damage behavior",
                "Render only authoritative ability events/results already emitted by the core; do not implement the remaining ability library in Minecraft."));
        entries.put(Capability.ITEMS, partial(
                "Canonical held-item battle state, generic rule-effect playback and parity-backed Pink Pearl damage hook; integration also has canonical item reservations",
                "Reserve/commit canonical items server-side and render verified core item events only; do not manufacture unported item effects."));
        entries.put(Capability.TRAINER_FEATURES_AND_PERKS, partial(
                "Selected status-skip Trainer Feature exceptions plus TRAINER_FEATURE/PERK hook-source and lifecycle categories",
                "Use only specifically verified Feature behavior/events; keep all other perks/features deferred."));
        entries.put(Capability.AI_LEGAL_ACTION_INFRASTRUCTURE, verified(
                "Runtime-authoritative autobattler action space, affiliation, geometry, action budget, move availability and frequency filtering",
                "AI may choose only from the legal choices produced by the core."));
        entries.put(Capability.AI_TACTICAL_SCORING_POLICY, blocking(
                "Full Python-equivalent tactical scoring/policy is not yet ported",
                "Do not claim Python AI parity or move tactical policy into the Minecraft adapter."));
        entries.put(Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK, blocking(
                "BattleEvent/RuleEffectEvent semantic contracts and project-owned headless playback/presentation/world-projection DTOs can exist, but no verified live Minecraft/Cobblemon/Craftics runtime adapter has executed them",
                "Keep headless presentation tests separate from claims about in-game entity animation, networking or playback until a live adapter is exercised."));
        if (entries.size() != Capability.values().length) throw new IllegalStateException("compatibility matrix must cover every upstream capability");
        return Collections.unmodifiableMap(entries);
    }

    private static Entry verified(String contracts, String policy) { return new Entry(Support.VERIFIED, contracts, policy); }
    private static Entry partial(String contracts, String policy) { return new Entry(Support.PARTIAL, contracts, policy); }
    private static Entry blocking(String contracts, String policy) { return new Entry(Support.BLOCKING, contracts, policy); }
}
