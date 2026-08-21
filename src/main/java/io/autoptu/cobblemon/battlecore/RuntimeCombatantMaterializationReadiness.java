package io.autoptu.cobblemon.battlecore;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Executable boundary describing which inputs needed by AutoPTU-Java RuntimeCombatantState
 * are already frozen by this integration project or can be initialized canonically by
 * AutoPTU-Java itself, and which still require additional authoritative upstream resolution.
 * This class must not manufacture PTU defaults for blocked inputs.
 */
public final class RuntimeCombatantMaterializationReadiness {
    public enum Requirement {
        STABLE_COMBATANT_ID,
        GRID_POSITION,
        HP_AND_MAX_HP,
        COMBAT_STATS,
        BASE_ACCURACY_EVASION,
        TYPES,
        ABILITY_IDENTITIES,
        CANONICAL_MOVESET,
        AFFILIATION,
        COMBATANT_GEOMETRY,
        BASE_MOVEMENT,
        RESOLVED_MOVEMENT_PROFILE,
        ACTION_BUDGET_INITIALIZATION,
        DYNAMIC_ACCURACY_EVASION_FLAGS,
        RESOLVED_DAMAGE_MODIFIERS
    }

    public enum Authority { INTEGRATION_FROZEN, AUTOPTU_JAVA_RESOLVED }
    public enum State { READY, BLOCKED }

    public record Entry(Authority authority, State state, String evidence) {
        public Entry {
            authority = Objects.requireNonNull(authority, "authority");
            state = Objects.requireNonNull(state, "state");
            if (evidence == null || evidence.isBlank()) {
                throw new IllegalArgumentException("evidence is required");
            }
        }
    }

    private static final Map<Requirement, Entry> ENTRIES = buildEntries();

    private RuntimeCombatantMaterializationReadiness() {}

    public static Entry entry(Requirement requirement) {
        Entry entry = ENTRIES.get(Objects.requireNonNull(requirement, "requirement"));
        if (entry == null) throw new IllegalStateException("unmapped runtime requirement: " + requirement);
        return entry;
    }

    public static Map<Requirement, Entry> entries() {
        return ENTRIES;
    }

    public static boolean canMaterializeRuntimeCombatant() {
        return ENTRIES.values().stream().allMatch(entry -> entry.state() == State.READY);
    }

    private static Map<Requirement, Entry> buildEntries() {
        EnumMap<Requirement, Entry> entries = new EnumMap<>(Requirement.class);
        ready(entries, Requirement.STABLE_COMBATANT_ID, Authority.INTEGRATION_FROZEN,
                "BattlePokemonSnapshot stable canonical Pokemon identity and roster binding");
        ready(entries, Requirement.GRID_POSITION, Authority.INTEGRATION_FROZEN,
                "BattleInitialPlacementSnapshot authoritative grid anchor");
        ready(entries, Requirement.HP_AND_MAX_HP, Authority.INTEGRATION_FROZEN,
                "CanonicalHealth -> BattleCombatantHealthProjection");
        ready(entries, Requirement.COMBAT_STATS, Authority.INTEGRATION_FROZEN,
                "CanonicalCombatStats -> BattleCombatantStatProjection");
        ready(entries, Requirement.BASE_ACCURACY_EVASION, Authority.INTEGRATION_FROZEN,
                "CanonicalAccuracyEvasion -> BattleCombatantAccuracyEvasionProjection");
        ready(entries, Requirement.TYPES, Authority.INTEGRATION_FROZEN,
                "CanonicalBattleTraits type identities");
        ready(entries, Requirement.ABILITY_IDENTITIES, Authority.INTEGRATION_FROZEN,
                "CanonicalBattleTraits ability identities; effects remain core-owned");
        ready(entries, Requirement.CANONICAL_MOVESET, Authority.INTEGRATION_FROZEN,
                "CanonicalMoveLoadout -> BattleCombatantMoveLoadoutProjection");
        ready(entries, Requirement.AFFILIATION, Authority.INTEGRATION_FROZEN,
                "BattleCombatantAffiliationProjection from reserved owner identity");
        ready(entries, Requirement.COMBATANT_GEOMETRY, Authority.INTEGRATION_FROZEN,
                "BattleCombatantGeometryProjection authoritative PTU footprint size");
        ready(entries, Requirement.BASE_MOVEMENT, Authority.INTEGRATION_FROZEN,
                "CanonicalBaseMovement values are frozen without runtime modifiers");
        ready(entries, Requirement.ACTION_BUDGET_INITIALIZATION, Authority.AUTOPTU_JAVA_RESOLVED,
                "AutoPTU-Java ActionBudget() creates the canonical empty consumed/extra-action state; BattleRoundController and ActionBudget own subsequent resets, grants and consumption, so the adapter supplies no trusted action availability");

        blocked(entries, Requirement.RESOLVED_MOVEMENT_PROFILE, Authority.AUTOPTU_JAVA_RESOLVED,
                "MovementProfile also requires sprint/capability/status/ability/weather/equipment/Trainer Feature resolution; integration must not invent these values");
        blocked(entries, Requirement.DYNAMIC_ACCURACY_EVASION_FLAGS, Authority.AUTOPTU_JAVA_RESOLVED,
                "Sniper, No Guard, Blur, Probability Control and other dynamic evasion/accuracy effects are hook/runtime state, not Minecraft inputs");
        blocked(entries, Requirement.RESOLVED_DAMAGE_MODIFIERS, Authority.AUTOPTU_JAVA_RESOLVED,
                "DamageModifierHookRegistry owns status/ability/item/move/terrain/weather/hazard/Feature modifiers as supported upstream");

        if (entries.size() != Requirement.values().length) {
            throw new IllegalStateException("runtime readiness must cover every constructor requirement");
        }
        return Collections.unmodifiableMap(entries);
    }

    private static void ready(Map<Requirement, Entry> entries, Requirement requirement, Authority authority, String evidence) {
        entries.put(requirement, new Entry(authority, State.READY, evidence));
    }

    private static void blocked(Map<Requirement, Entry> entries, Requirement requirement, Authority authority, String evidence) {
        entries.put(requirement, new Entry(authority, State.BLOCKED, evidence));
    }
}
