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
        INITIAL_COMBATANT_PLACEMENT,
        CANONICAL_BASE_MOVEMENT_SNAPSHOT,
        CANONICAL_BASE_MOVEMENT_BOOTSTRAP,
        CANONICAL_COMBATANT_GEOMETRY_BOOTSTRAP,
        CANONICAL_BATTLE_TRAITS_SNAPSHOT,
        CANONICAL_BATTLE_TRAITS_BOOTSTRAP,
        CANONICAL_ACCURACY_EVASION_SNAPSHOT,
        CANONICAL_ACCURACY_EVASION_BOOTSTRAP,
        CANONICAL_STATUS_METADATA_SNAPSHOT,
        CANONICAL_STATUS_METADATA_BOOTSTRAP,
        CANONICAL_TRAINER_FEATURE_SNAPSHOT,
        CANONICAL_TRAINER_FEATURE_BOOTSTRAP,
        CANONICAL_TRAINER_RUNTIME_BOOTSTRAP,
        CANONICAL_HELD_ITEM_BOOTSTRAP,
        AUTHORITATIVE_MOVE_CATALOG_PROJECTION,
        RUNTIME_COMBATANT_MATERIALIZATION_INPUT,
        RUNTIME_BATTLE_PREPARATION_ENVELOPE,
        BATTLEFIELD_WORLD_OBSERVATION_SNAPSHOT,
        BATTLEFIELD_MOVEMENT_OBSERVATION_INPUT,
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
            return capabilities.stream().map(UpstreamCompatibilityMatrix::entry)
                    .anyMatch(entry -> entry.support() == UpstreamCompatibilityMatrix.Support.BLOCKING);
        }
    }

    private static final Map<Feature, Requirement> REQUIREMENTS = buildRequirements();
    private IntegrationFeatureCompatibility() {}
    public static Requirement requirement(Feature feature) {
        Requirement requirement = REQUIREMENTS.get(feature);
        if (requirement == null) throw new IllegalStateException("unmapped integration feature: " + feature);
        return requirement;
    }
    public static Map<Feature, Requirement> requirements() { return REQUIREMENTS; }

    private static Map<Feature, Requirement> buildRequirements() {
        EnumMap<Feature, Requirement> requirements = new EnumMap<>(Feature.class);
        requirements.put(Feature.GRID_TARGET_PREVIEW, requirement("Render only core-produced legal target/tile choices.", UpstreamCompatibilityMatrix.Capability.CORE_TARGETING));
        requirements.put(Feature.GRID_WORLD_COORDINATE_TRANSFORM, requirement("Map authoritative 2D grid coordinates to a project-owned horizontal world-block plane only; collision, terrain cost, targeting and movement legality remain core-owned.", UpstreamCompatibilityMatrix.Capability.CORE_TARGETING, UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        requirements.put(Feature.BATTLE_ARENA_RESERVATION, requirement("Freeze dimension, world origin, elevation and cardinal grid orientation into the server-owned battle reservation. The snapshot supplies a stable coordinate frame only and never decides PTU legality.", UpstreamCompatibilityMatrix.Capability.CORE_TARGETING, UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        requirements.put(Feature.INITIAL_COMBATANT_PLACEMENT, requirement("Freeze one initial authoritative grid anchor for every reserved combatant and bind it to the battle reservation/arena. Footprint size, overlap/collision, facing, terrain and placement legality remain core-owned or deferred until explicit upstream contracts exist.", UpstreamCompatibilityMatrix.Capability.CORE_TARGETING, UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        requirements.put(Feature.CANONICAL_BASE_MOVEMENT_SNAPSHOT, requirement("Freeze persistent base Overland/Swim/Sky/Long Jump/High Jump values into the battle Pokemon snapshot only. Runtime movement effects remain unresolved and core-owned.", UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        requirements.put(Feature.CANONICAL_BASE_MOVEMENT_BOOTSTRAP, requirement("Bind frozen base movement values to the same reservation, roster and initial placement. Do not approximate resolved MovementProfile.", UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        requirements.put(Feature.CANONICAL_COMBATANT_GEOMETRY_BOOTSTRAP, requirement("Bind server-owned PTU size labels to the exact placed roster. Minecraft model dimensions never become rule inputs.", UpstreamCompatibilityMatrix.Capability.CORE_TARGETING));
        requirements.put(Feature.CANONICAL_BATTLE_TRAITS_SNAPSHOT, requirement("Freeze server-owned Pokemon type and ability identities. Type arithmetic and every ability effect remain AutoPTU-Java-owned.", UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS, UpstreamCompatibilityMatrix.Capability.ABILITIES));
        requirements.put(Feature.CANONICAL_BATTLE_TRAITS_BOOTSTRAP, requirement("Bind canonical type and ability identities to the exact reservation/roster. Transport identities only.", UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS, UpstreamCompatibilityMatrix.Capability.ABILITIES));
        requirements.put(Feature.CANONICAL_ACCURACY_EVASION_SNAPSHOT, requirement("Freeze only Python-oracle baseline accuracy/evasion inputs. Dynamic contributions remain core-owned.", UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS));
        requirements.put(Feature.CANONICAL_ACCURACY_EVASION_BOOTSTRAP, requirement("Bind baseline accuracy/evasion inputs to the exact canonical reservation and roster. AutoPTU-Java owns final behavior.", UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS));
        requirements.put(Feature.CANONICAL_STATUS_METADATA_SNAPSHOT, requirement("Freeze ordered server-owned status identities plus scalar metadata. Expiry, ticking, cures, immunities and interpretation remain AutoPTU-Java-owned.", UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE, UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE));
        requirements.put(Feature.CANONICAL_STATUS_METADATA_BOOTSTRAP, requirement("Bind ordered status metadata to the exact reservation and roster without executing status behavior in Minecraft.", UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE, UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE));
        requirements.put(Feature.CANONICAL_TRAINER_FEATURE_SNAPSHOT, requirement("Freeze server-owned Trainer Feature identities into BattleTrainerSnapshot. Client/UI state may not grant battle Features.", UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS));
        requirements.put(Feature.CANONICAL_TRAINER_FEATURE_BOOTSTRAP, requirement("Bind frozen Trainer Feature identities to the battle reservation and trainer. Feature execution remains AutoPTU-Java-owned.", UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS, UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE));
        requirements.put(Feature.CANONICAL_TRAINER_RUNTIME_BOOTSTRAP, requirement("Freeze battle-start Trainer AP and Feature ownership from canonical trainer state and derive combatant-controller bindings only from the reserved Pokemon roster. Prepare the public TrainerRuntimeState/putTrainer/bindController boundary; Minecraft/client payloads may not set AP, grant Features, choose controllers, spend/restore AP or execute perk effects.", UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS, UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE, UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        requirements.put(Feature.CANONICAL_HELD_ITEM_BOOTSTRAP, requirement("Resolve each combatant held-item identity only from the frozen server battle reservation; all item effects remain core-owned.", UpstreamCompatibilityMatrix.Capability.ITEMS));
        requirements.put(Feature.AUTHORITATIVE_MOVE_CATALOG_PROJECTION, requirement("Resolve every frozen move ID through a server-owned move catalog into public MoveOption metadata. Client/Minecraft may not override move metadata.", UpstreamCompatibilityMatrix.Capability.CORE_TARGETING, UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR, UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        requirements.put(Feature.RUNTIME_COMBATANT_MATERIALIZATION_INPUT, requirement("Package only integration-frozen combatant inputs. Resolved MovementProfile, ActionBudget, dynamic accuracy/evasion flags and damage modifiers remain absent until AutoPTU-Java resolves them.", UpstreamCompatibilityMatrix.Capability.CORE_TARGETING, UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY, UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS, UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR, UpstreamCompatibilityMatrix.Capability.ABILITIES));
        requirements.put(Feature.RUNTIME_BATTLE_PREPARATION_ENVELOPE, requirement("Bind materialization inputs, authoritative move metadata, held-item identities and structured status metadata to one reservation while preserving explicit core-owned blockers. Trainer runtime state is prepared separately until the envelope adopts the public trainer/controller runtime boundary.", UpstreamCompatibilityMatrix.Capability.CORE_TARGETING, UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY, UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS, UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR, UpstreamCompatibilityMatrix.Capability.ABILITIES, UpstreamCompatibilityMatrix.Capability.ITEMS, UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE, UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE));
        requirements.put(Feature.BATTLEFIELD_WORLD_OBSERVATION_SNAPSHOT, requirement("Freeze adapter-observed block/fluid/collision/elevation facts against the reserved arena transform. PTU terrain semantics remain core-owned.", UpstreamCompatibilityMatrix.Capability.CORE_TARGETING, UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY, UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS));
        requirements.put(Feature.BATTLEFIELD_MOVEMENT_OBSERVATION_INPUT, requirement("Expose only adapter-neutral physical facts keyed by authoritative grid coordinates; stop before terrain cost, blocker, path or traversability semantics.", UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY, UpstreamCompatibilityMatrix.Capability.TERRAIN_WEATHER_HAZARDS_ZONES_REACTIONS));
        requirements.put(Feature.WORLD_RELOCATION_PROJECTION, requirement("Translate authoritative relocation endpoints into world coordinates only. Path legality, collision, terrain, forced movement and reactions remain core-owned.", UpstreamCompatibilityMatrix.Capability.CORE_TARGETING, UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        requirements.put(Feature.PLAYER_SHIFT_REQUEST, requirement("Send a requested destination; core owns path/movement legality.", UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY, UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        requirements.put(Feature.MOVE_SELECTION_REQUEST, requirement("Send move identity/target intent only; core owns loadout, frequency, targeting and resolution.", UpstreamCompatibilityMatrix.Capability.CORE_TARGETING, UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR, UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        requirements.put(Feature.ROUND_LIFECYCLE_PLAYBACK, requirement("Render only lifecycle events/state emitted through verified core behavior.", UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE));
        requirements.put(Feature.DAMAGE_RESULT_PLAYBACK, requirement("Render resolved HP/damage events without adapter-side modifiers.", UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS, UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE));
        requirements.put(Feature.STATUS_RESULT_PLAYBACK, requirement("Render only status behavior emitted or exposed by verified core contracts.", UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE));
        requirements.put(Feature.FORCED_MOVEMENT_PLAYBACK, requirement("Wait for core-owned push/pull/knockback/interception semantics.", UpstreamCompatibilityMatrix.Capability.COMPLETE_MOVEMENT_BEHAVIOR));
        requirements.put(Feature.ABILITY_EFFECT_PLAYBACK, requirement("Render only parity-backed ability events/results.", UpstreamCompatibilityMatrix.Capability.ABILITIES));
        requirements.put(Feature.ITEM_BATTLE_EFFECT_PLAYBACK, requirement("Render only parity-backed item events/results.", UpstreamCompatibilityMatrix.Capability.ITEMS));
        requirements.put(Feature.TRAINER_FEATURE_PLAYBACK, requirement("Render only authoritative Trainer Feature/perk semantic events emitted by the core. The adapter must not execute perks or mutate AP.", UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS));
        requirements.put(Feature.SEMANTIC_PRESENTATION_COMMANDS, requirement("Convert current authoritative semantic event contracts into rendering commands only; malformed or unknown semantics fail closed.", UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY, UpstreamCompatibilityMatrix.Capability.CORE_CALCULATIONS_AND_COMBAT_STATS, UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE, UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE, UpstreamCompatibilityMatrix.Capability.ABILITIES, UpstreamCompatibilityMatrix.Capability.ITEMS, UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS));
        requirements.put(Feature.AUTOBATTLER_LEGAL_CHOICE_INPUT, requirement("Consume the legal BattleChoice space produced from authoritative runtime state.", UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE));
        requirements.put(Feature.AUTOBATTLER_TACTICAL_POLICY, requirement("Wait for Python-equivalent tactical scoring/policy.", UpstreamCompatibilityMatrix.Capability.AI_TACTICAL_SCORING_POLICY));
        requirements.put(Feature.LIVE_MINECRAFT_BATTLE_ADAPTER, requirement("Requires an actually exercised Minecraft/Cobblemon/Craftics adapter.", UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));
        if (requirements.size() != Feature.values().length) throw new IllegalStateException("integration feature matrix must cover every feature");
        return Map.copyOf(requirements);
    }

    private static Requirement requirement(String scope, UpstreamCompatibilityMatrix.Capability... capabilities) {
        return new Requirement(Set.of(capabilities), scope);
    }
}
