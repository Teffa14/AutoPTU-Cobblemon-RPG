package io.autoptu.cobblemon.battlecore;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Executable compatibility checklist for the currently inspected AutoPTU-Java contract. */
public final class UpstreamCompatibilityMatrix {
    public static final String AUTOPTU_JAVA_SHA = "9e1c918f33faa45c4c8832ba457cc36b875267c7";
    public static final String AUTOPTU_PYTHON_SHA = "e4bb0ca38b7018710af476ce365d515a387de4e7";

    public enum Capability {
        CORE_TARGETING, CORE_MOVEMENT_LEGALITY, COMPLETE_MOVEMENT_BEHAVIOR,
        CORE_CALCULATIONS_AND_COMBAT_STATS, ACTION_ECONOMY_AND_INITIATIVE,
        FULL_TURN_ROUND_LIFECYCLE, FULL_STATEFUL_DAMAGE_PIPELINE, COMPLETE_STATUS_LIFECYCLE,
        TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS, MOVE_SPECIFIC_BEHAVIOR, ABILITIES, ITEMS,
        TRAINER_FEATURES_AND_PERKS, AI_LEGAL_ACTION_INFRASTRUCTURE, AI_TACTICAL_SCORING_POLICY,
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
                "Damage Base tables, type effectiveness, STAB, accuracy stages, EvasionProfile, CombatantStatProfile, server-owned mutable CombatStageState, ordered CombatStageHookRegistry with Simple/Defiant/Competitive/Plus [SwSh]/Minus [SwSh] Python parity, centralized CombatStageMutationService/CombatStageMutationResult, generic CombatStageMutationOptions recursive-hook suppression and authoritative SpatialAbilityQuery radius lookup",
                "Supply canonical baseline inputs only. Final evasion, accuracy, combat-stage mutation and combat-stage reaction effects remain core-owned, including move, ability, item, Trainer Feature, status, terrain and temporary-effect contributions. The adapter may project returned starting/base/final stages and emitted semantic events in authoritative order, but must not claim current stages, applied deltas, spatial reaction sources, recursive suppression state or apply stage deltas in Minecraft."));
        entries.put(Capability.ACTION_ECONOMY_AND_INITIATIVE, verified(
                "ActionBudget, typed turn phases, deterministic initiative, Trick Room/League ordering; Trainer AP is separately authoritative in TrainerRuntimeState",
                "Treat action availability, initiative and Trainer resource spending as core-owned state. The integration may freeze canonical Trainer AP at battle start, but must not spend/restore AP or initialize/mutate ActionBudget on behalf of PTU lifecycle logic."));
        entries.put(Capability.FULL_TURN_ROUND_LIFECYCLE, partial(
                "BattleRoundController, ordered LifecycleHookRegistry, StatusPhaseEffectRegistry, AbilityPhaseEffectRegistry, PerkPhaseEffectRegistry, CombatantPhaseEffectDispatcher STATUS -> ABILITY -> PERK ordering, structured StatusStateStore, active actor/phase state, authoritative phase/turn-end events, pending status-skip consumption, turn-end boundary, TemporaryEffectStore, move-frequency reset, selected round-start cleanup, history rotation, delayed-hit scheduling/binding, Flinch behavior, Strange Tempo, Lancer END, authoritative TrainerRuntimeState/controller binding, mutable CombatStageState and parity-backed Defense Mastery/fixed Link Feature END behavior",
                "Consume verified lifecycle state/events only. Preserve authoritative ordering. Canonical Trainer Features, AP and controller bindings may be prepared from the server reservation, but AP spending, combat-stage mutation, perk execution, START-on-beginTurn wiring, broader concrete perk/status/ability libraries, delayed-hit execution, terrain/zone/room advancement, send-out effects and remaining Python lifecycle hooks stay core-owned."));
        entries.put(Capability.FULL_STATEFUL_DAMAGE_PIPELINE, partial(
                "RuntimeMoveResolution, authoritative stats/types/statuses, ordered DamageModifierHookRegistry, pre-damage move hooks and ordered PostDamageHookRegistry/PostDamageHookResult; Burn, Pink Pearl, Mega Launcher, actual-HP-loss move-damage-history recording, Aqua Boost/Ignition Boost/Thunder Boost, Power Spot and Type Aura post-result aura selection are parity-backed. AuraStormResolution freezes normal/errata Aura Storm damage-bonus parity, and shared AuraBreakErrataAdjustment now freezes reusable ability-targeted inversion/expiry semantics against server-owned TemporaryEffectEntry state. Live Aura Storm registry wiring still requires an authoritative injury-count source and Aura Break blocker query. BattleRuntime applies resolved wired post-damage flat bonuses before authoritative HP mutation, MoveResolvedEvent emission and move-damage-history recording",
                "Consume only core-resolved damage, target HP and ordered semantic events. Server-owned injury count may be transported as canonical battle input, but Minecraft must never calculate Aura Storm scaling, determine Aura Break blockers/inversion, inspect or mutate aura_break_errata temporary effects, select aura sources, calculate or apply post-damage bonuses, inject unported move/ability/item/terrain modifiers, infer missing damage-history semantics, or perform an independent HP mutation."));
        entries.put(Capability.COMPLETE_STATUS_LIFECYCLE, partial(
                "Canonical status names plus structured StatusEntry/StatusStateStore, StatusPhaseEffectRegistry, StatusApplicationHookRegistry with Inner Focus Flinch prevention, Burn damage penalty, Sleep/Paralysis/Freeze evasion effects, status skips, Flinch START/expiry, Strange Tempo Confusion branching and selected Trainer Feature exceptions",
                "Transport server-owned status identity and scalar metadata only. Status application/prevention remains core-owned; do not implement missing ticks, cures, immunities, durations, source-sensitive behavior, phase effects or interactions in Minecraft."));
        entries.put(Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS, partial(
                "Terrain movement costs, weather calculation primitives, generic HookSource categories and lifecycle seams exist",
                "Project raw terrain/world observations and semantic events only. Do not classify Minecraft blocks into PTU terrain or approximate hazards, zones, reactions or forced movement before core contracts exist."));
        entries.put(Capability.MOVE_SPECIFIC_BEHAVIOR, partial(
                "Authoritative movesets, public MoveOption/MoveSpec/MoveCombatProfile metadata, canonical MoveSpec keyword transport with Python move_has_keyword parity, move-frequency enforcement, temporary-effect state, ordered pre-damage move hooks and delayed-hit scheduling/binding",
                "Resolve frozen move IDs only through trusted server-owned catalog metadata matching public core contracts, including canonical keyword identities. Client/Minecraft may request move identity/target intent only; it may not supply, infer from text, or execute keyword semantics such as push, pull, crash or contact, and unported specials remain core-owned and deferred."));
        entries.put(Capability.ABILITIES, partial(
                "Canonical ability identities, generic HookSource support, AbilityPhaseEffectRegistry, CombatStageHookRegistry, PostDamageHookRegistry and parity-backed Mega Launcher, Strange Tempo, Inner Focus, Lancer, Simple, Defiant, Competitive, Plus [SwSh], Minus [SwSh], Aqua Boost, Ignition Boost, Thunder Boost, Power Spot and Type Aura behavior; AuraStormResolution freezes normal and errata Aura Storm formulas and shared AuraBreakErrataAdjustment freezes reusable Aura Break [Errata] inversion/expiry behavior, but live Aura Storm wiring still awaits authoritative injury count and Aura Break blocker state; spatial abilities use authoritative positions, team identity, active/HP state, deterministic holder order and recursive hook suppression where applicable",
                "Render only authoritative ability events/results already emitted by the core. Injury count may come only from canonical server state. Combat-stage reactions, final stage values, radius/adjacency holder selection, team filtering, Aura Storm scaling, Aura Break blocker/inversion decisions, aura_break_errata temporary-effect cleanup, damage-bonus source selection, recursive mutation and hook suppression must be projected from core results rather than recomputed from Minecraft entities or requested deltas. Aura-adjusted damage/HP may be mirrored only from the final MoveResolvedEvent; do not apply aura bonuses or implement the remaining ability library in Minecraft."));
        entries.put(Capability.ITEMS, partial(
                "BattleRuntimeState heldItemsByCombatant, HeldItemState stable identity, generic rule-effect playback and parity-backed Pink Pearl damage hook; integration also has canonical item reservations",
                "Project only frozen held-item identity into authoritative runtime state, reserve/commit canonical items server-side, and render verified core item events only; do not manufacture unported item effects or trust client/entity equipment claims."));
        entries.put(Capability.TRAINER_FEATURES_AND_PERKS, partial(
                "Authoritative TrainerRuntimeState stores server-owned Trainer Feature identities and AP; BattleRuntimeState binds combatants to trainer controllers; PerkPhaseLifecycleHook derives owned Features from runtime state; CombatStageState and CombatStageHookRegistry are authoritative battle state/reaction seams; selected status-skip exceptions, Defense Mastery and fixed Attack/Defense/Special Attack/Special Defense/Speed Link END behavior are parity-backed; Link stage changes intentionally remain on their Python-parity direct mutation path",
                "Freeze and transport Trainer Feature ownership, battle-start AP and combatant-controller bindings only from canonical server state. Minecraft/client payloads may not grant Features, set AP, spend/restore AP, choose controllers, mutate combat stages, run combat-stage reactions or execute perks. Remaining Trainer Feature/perk implementations stay AutoPTU-Java-owned."));
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
