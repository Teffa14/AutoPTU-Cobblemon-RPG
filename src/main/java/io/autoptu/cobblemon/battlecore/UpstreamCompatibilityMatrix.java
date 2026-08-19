package io.autoptu.cobblemon.battlecore;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Executable compatibility checklist for the currently inspected AutoPTU-Java contract. */
public final class UpstreamCompatibilityMatrix {
    public static final String AUTOPTU_JAVA_SHA = "48083562c03b50e2e6601b3c52101f7a91934cac";
    public static final String AUTOPTU_PYTHON_SHA = "e4bb0ca38b7018710af476ce365d515a387de4e7";

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
    public static boolean mayProjectAuthoritativeBehavior(Capability capability) { return entry(capability).support() != Support.BLOCKING; }

    private static Map<Capability, Entry> buildEntries() {
        EnumMap<Capability, Entry> entries = new EnumMap<>(Capability.class);
        entries.put(Capability.CORE_TARGETING, verified(
                "MoveSpec, target anchors, footprints, range/area legality, line-of-sight tests and CombatantGeometryState size labels",
                "Project grid inputs and server-owned PTU size labels and render authoritative legal targets only. Combatant model scale, facing, footprint overlap and placement legality must not be invented by the adapter."));
        entries.put(Capability.CORE_MOVEMENT_LEGALITY, verified(
                "MovementGrid, resolved MovementProfile/JumpProfile, Shift/Jump legality, Overland/Swim/Sky and terrain-cost Python parity tests",
                "Supply canonical base movement and adapter-neutral world inputs only. Runtime movement effects such as sprint, Wallrunner, Naturewalk, statuses, abilities, weather, equipment and Trainer Features must be resolved by authoritative PTU contracts before geometry is evaluated; never decide them in Minecraft."));
        entries.put(Capability.COMPLETE_MOVEMENT_BEHAVIOR, blocking(
                "Forced movement, push/pull/knockback, interception and interaction-driven movement are not complete",
                "Do not synthesize forced movement, interception or knockback rules in the adapter."));
        entries.put(Capability.CORE_CALCULATIONS_AND_COMBAT_STATS, verified(
                "Damage Base tables, type effectiveness, STAB, accuracy stages, EvasionProfile, combat stages and CombatantStatProfile with Python parity coverage",
                "Supply canonical baseline inputs only. Final evasion and accuracy effects remain core-owned, including ability, item, Trainer Feature, status, terrain and temporary-effect contributions."));
        entries.put(Capability.ACTION_ECONOMY_AND_INITIATIVE, verified(
                "ActionBudget, typed turn phases, deterministic initiative, Trick Room/League ordering",
                "Treat action availability and ordering as core-owned state. The integration layer must not initialize or mutate ActionBudget on behalf of PTU lifecycle logic."));
        entries.put(Capability.FULL_TURN_ROUND_LIFECYCLE, partial(
                "BattleRoundController, ordered LifecycleHookRegistry, reusable ordered StatusPhaseEffectRegistry, server-owned active actor/phase pointer, authoritative phase-transition events, pending status-skip consumption after ordered phase hooks with last-pending-request overwrite parity, turn-end boundary, payload-bearing TemporaryEffectStore, move-frequency reset, selected round-start cleanup, damage/injury history rotation, delayed-hit scheduling/binding, move-damage history, Flinch START behavior and Strange Tempo Confusion START branching",
                "Consume verified lifecycle state/events only. Flinch and Strange Tempo are bounded parity-backed slices, not complete status behavior. Trainer Feature phase dispatch, concrete remaining status effects, Corrosive Toxins, delayed-hit execution, terrain/zone/room advancement, send-out effects and remaining Python lifecycle hooks stay deferred."));
        entries.put(Capability.FULL_STATEFUL_DAMAGE_PIPELINE, partial(
                "RuntimeMoveResolution, authoritative stats/types/statuses, ordered DamageModifierHookRegistry and pre-damage move hooks; Burn, Pink Pearl, Mega Launcher and actual-HP-loss move-damage-history recording are parity-backed",
                "Consume resolved damage and semantic events; never inject unported move/ability/item/terrain modifiers or infer non-move damage-history semantics in the adapter."));
        entries.put(Capability.COMPLETE_STATUS_LIFECYCLE, partial(
                "Canonical statuses, reusable ordered StatusPhaseEffectRegistry, Burn damage penalty, Sleep/Paralysis/Freeze evasion effects, status skips, pending phase status-skip consumption, Flinch START behavior, Strange Tempo Confusion START branching and selected Trainer Feature exceptions",
                "Expose only parity-backed status effects. Flinch and Strange Tempo coverage does not imply the status library is complete; do not implement missing ticks, cures, phase effects or interactions client-side."));
        entries.put(Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS, partial(
                "Terrain movement costs, weather calculation primitives, generic HookSource categories and lifecycle seams exist",
                "Project raw terrain/world observations and semantic events only. Do not classify Minecraft blocks into PTU terrain or approximate hazards, zones, reactions or forced movement before core contracts exist."));
        entries.put(Capability.MOVE_SPECIFIC_BEHAVIOR, partial(
                "Authoritative movesets, public MoveOption/MoveSpec/MoveCombatProfile metadata, move-frequency enforcement, temporary-effect state, ordered pre-damage move hooks and delayed-hit scheduling/binding",
                "Resolve frozen move IDs only through trusted server-owned catalog metadata matching public core contracts. Client/Minecraft may request move identity/target intent only; unported specials remain core-owned and deferred."));
        entries.put(Capability.ABILITIES, partial(
                "Canonical ability identities, generic hook source support, parity-backed Mega Launcher behavior and Strange Tempo Confusion START behavior",
                "Render only authoritative ability events/results already emitted by the core; do not implement the remaining ability library in Minecraft."));
        entries.put(Capability.ITEMS, partial(
                "BattleRuntimeState canonical heldItemsByCombatant map, HeldItemState stable item identity, generic rule-effect playback and parity-backed Pink Pearl damage hook; integration also has canonical item reservations",
                "Project only frozen held-item instance/catalog identity into authoritative runtime state, reserve/commit canonical items server-side, and render verified core item events only; do not manufacture unported item effects or trust client/entity equipment claims."));
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
                "Semantic battle-event contracts and project-owned headless playback/world-projection DTOs exist, but no verified live Minecraft/Cobblemon/Craftics runtime adapter has executed them",
                "Keep headless presentation tests separate from claims about in-game networking, animation or playback until a live adapter is exercised."));
        if (entries.size() != Capability.values().length) throw new IllegalStateException("compatibility matrix must cover every upstream capability");
        return Collections.unmodifiableMap(entries);
    }

    private static Entry verified(String contracts, String policy) { return new Entry(Support.VERIFIED, contracts, policy); }
    private static Entry partial(String contracts, String policy) { return new Entry(Support.PARTIAL, contracts, policy); }
    private static Entry blocking(String contracts, String policy) { return new Entry(Support.BLOCKING, contracts, policy); }
}
