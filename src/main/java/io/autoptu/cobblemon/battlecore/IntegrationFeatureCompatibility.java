package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Maps bounded integration features to the upstream capabilities they are allowed to consume. */
public final class IntegrationFeatureCompatibility {
    public enum Feature {
        GRID_TARGET_PREVIEW,
        GRID_WORLD_COORDINATE_TRANSFORM,
        BATTLE_ARENA_RESERVATION,
        WORLD_RELOCATION_PROJECTION,
        PLAYER_SHIFT_REQUEST,
        MOVE_SELECTION_REQUEST,
        ROUND_LIFECYCLE_PLAYBACK,
        DAMAGE_RESULT_PLAYBACK,
        STATUS_RESULT_PLAYBACK,
        FORCED_MOVEMENT_PLAYBACK,
        ABILITY_EFFECT_PLAYBACK,
        ITEM_BATTLE_EFFECT_PLAYBACK,
        TRAINER_FEATURE_PLAYBACK,
        SEMANTIC_PRESENTATION_COMMANDS,
        AUTOBATTLER_LEGAL_CHOICE_INPUT,
        AUTOBATTLER_TACTICAL_POLICY,
        LIVE_MINECRAFT_BATTLE_ADAPTER
    }

    public record Requirement(Set<UpstreamCompatibilityMatrix.Capability> capabilities, String boundedScope) {
        public Requirement {
            capabilities = Set.copyOf(capabilities);
            if (capabilities.isEmpty()) throw new IllegalArgumentException("capabilities are required");
            if (boundedScope == null || boundedScope.isBlank()) throw new IllegalArgumentException("boundedScope is required");
        }

        public boolean hasBlockingDependency() {
            return capabilities.stream()
                    .map(UpstreamCompatibilityMatrix::entry)
                    .anyMatch(entry -> entry.support() == UpstreamCompatibilityMatrix.Support.BLOCKING);
        }
    }

    private static final Map<Feature, Requirement> REQUIREMENTS = buildRequirements();

    private IntegrationFeatureCompatibility() {
    }

    public static Requirement requirement(Feature feature) {
        Requirement requirement = REQUIREMENTS.get(feature);
        if (requirement == null) throw new IllegalStateException("unmapped integration feature: " + feature);
        return requirement;
    }

    public static Map<Feature, Requirement> requirements() {
        return REQUIREMENTS;
    }

    private static Map<Feature, Requirement> buildRequirements() {
        EnumMap<Feature, Requirement> requirements = new EnumMap<>(Feature.class);
        requirements.put(Feature.GRID_TARGET_PREVIEW, requirement(
                "Render only core-produced legal target/tile choices.",
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING));
        requirements.put(Feature.GRID_WORLD_COORDINATE_TRANSFORM, requirement(
                "Map authoritative 2D grid coordinates to a project-owned horizontal world-block plane only; collision, terrain cost, targeting and movement legality remain core-owned.",
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        requirements.put(Feature.BATTLE_ARENA_RESERVATION, requirement(
                "Freeze dimension, world origin, elevation and cardinal grid orientation into the server-owned battle reservation. The snapshot supplies a stable coordinate frame only and never decides PTU legality.",
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        requirements.put(Feature.WORLD_RELOCATION_PROJECTION, requirement(
                "Bind authoritative ENTITY_RELOCATION commands to the matching frozen reservation/roster and translate their grid endpoints into world coordinates only. Path legality, collision, terrain, forced movement and reactions remain core-owned.",
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        requirements.put(Feature.PLAYER_SHIFT_REQUEST, requirement(
                "Send a requested destination; core owns path/movement legality.",
                UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY,
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        requirements.put(Feature.MOVE_SELECTION_REQUEST, requirement(
                "Send move identity/target intent only; core owns loadout, frequency, targeting and resolution.",
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING,
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR,
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        requirements.put(Feature.ROUND_LIFECYCLE_PLAYBACK, requirement(
                "Render only lifecycle events/state emitted through verified core behavior; do not invent missing round/turn effects.",
                UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE));
        requirements.put(Feature.DAMAGE_RESULT_PLAYBACK, requirement(
                "Render resolved HP/damage events without adapter-side modifiers.",
                UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS,
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE));
        requirements.put(Feature.STATUS_RESULT_PLAYBACK, requirement(
                "Render only status behavior emitted or exposed by verified core contracts.",
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE));
        requirements.put(Feature.FORCED_MOVEMENT_PLAYBACK, requirement(
                "Wait for core-owned push/pull/knockback/interception semantics.",
                UpstreamCompatibilityMatrix.Capability.COMPLETE_MOVEMENT_BEHAVIOR));
        requirements.put(Feature.ABILITY_EFFECT_PLAYBACK, requirement(
                "Render only parity-backed ability RuleEffectEvent/results, currently including Mega Launcher behavior; defer the remaining ability library.",
                UpstreamCompatibilityMatrix.Capability.ABILITIES));
        requirements.put(Feature.ITEM_BATTLE_EFFECT_PLAYBACK, requirement(
                "Render only parity-backed item RuleEffectEvent/results, currently including Pink Pearl; canonical reservation remains server-owned.",
                UpstreamCompatibilityMatrix.Capability.ITEMS));
        requirements.put(Feature.TRAINER_FEATURE_PLAYBACK, requirement(
                "Only selected verified Trainer Feature behavior/events may be projected.",
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS));
        requirements.put(Feature.SEMANTIC_PRESENTATION_COMMANDS, requirement(
                "Convert current authoritative move/shift/status/Trainer Feature/rule-effect stable event contracts into rendering commands only; malformed or unknown semantics fail closed.",
                UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY,
                UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS,
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE,
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
                UpstreamCompatibilityMatrix.Capability.ABILITIES,
                UpstreamCompatibilityMatrix.Capability.ITEMS,
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS));
        requirements.put(Feature.AUTOBATTLER_LEGAL_CHOICE_INPUT, requirement(
                "Consume the legal BattleChoice space produced from authoritative runtime state.",
                UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE));
        requirements.put(Feature.AUTOBATTLER_TACTICAL_POLICY, requirement(
                "Wait for Python-equivalent tactical scoring/policy.",
                UpstreamCompatibilityMatrix.Capability.AI_TACTICAL_SCORING_POLICY));
        requirements.put(Feature.LIVE_MINECRAFT_BATTLE_ADAPTER, requirement(
                "Requires an actually exercised Minecraft/Cobblemon/Craftics adapter; headless playback and presentation commands alone do not satisfy this capability.",
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));

        if (requirements.size() != Feature.values().length) {
            throw new IllegalStateException("integration feature matrix must cover every feature");
        }
        return Map.copyOf(requirements);
    }

    private static Requirement requirement(String scope, UpstreamCompatibilityMatrix.Capability... capabilities) {
        return new Requirement(Set.of(capabilities), scope);
    }
}
