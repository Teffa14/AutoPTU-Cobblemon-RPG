package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and the bounded capability
 * deltas that landed after the last broad matrix refresh. It supplements, but never broadens,
 * UpstreamCompatibilityMatrix support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "3d7adc9ed7c3ca49d847c45f024046f62a5e159c";
    public static final String AUTOPTU_PYTHON_SHA = "e4bb0ca38b7018710af476ce365d515a387de4e7";

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
        result.put(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.VERIFIED,
                        "InitiativeRoundModifierResolution, InitiativeSpeedAbilityResolution, InitiativeAdditionalBonusResolution, TrainerInitiativeSpeedResolution, TrainerInitiativeEntryResolution and InitiativeOrderAssembly now cover bounded Python-parity Pokemon/Trainer initiative inputs, participation filtering, round modifiers, Trick Room ordering, League trainer-before-Pokemon ordering and deterministic ordered InitiativeEntry output.",
                        "Minecraft must not assemble, sort or inject initiative order. Remaining upstream gaps are limited to lifecycle installation/consumption and any Python interactions not yet wired into the authoritative runtime path."));
        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "InitiativeOrderAssembly can now produce the authoritative ordered entries and temporary-effect cleanup requests from parity-backed inputs.",
                        "Complete round lifecycle remains broader than order assembly: installation timing, remaining round-start effects, later lifecycle hooks and other Python start_round behavior remain incomplete."));
        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Slush Rush, Surge Surfer, Chlorophyll [Errata], Early Bird [Errata] and bounded initiative ability inputs participate in parity-backed initiative construction.",
                        "This is a bounded ability subset and does not complete the PTU ability library."));
        return Map.copyOf(result);
    }
}
