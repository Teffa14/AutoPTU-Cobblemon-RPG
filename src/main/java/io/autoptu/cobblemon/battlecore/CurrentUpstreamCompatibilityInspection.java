package io.autoptu.cobblemon.battlecore;

import java.util.EnumMap;
import java.util.Map;

/**
 * Records the exact upstream heads inspected for this integration slice and the bounded capability
 * deltas that landed after the last broad matrix refresh. It supplements, but never broadens,
 * UpstreamCompatibilityMatrix support classifications.
 */
public final class CurrentUpstreamCompatibilityInspection {
    public static final String AUTOPTU_JAVA_SHA = "3e26f9d856da02a23403164f49bb46ea296ecd99";
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
                        "InitiativeRoundModifierResolution, InitiativeSpeedAbilityResolution, InitiativeAdditionalBonusResolution and TrainerInitiativeSpeedResolution now cover Rocket Initiative, initiative_penalty expiry/application, Inner Focus [Errata], Slush Rush, Surge Surfer, Chlorophyll [Errata], Early Bird [Errata], Agility Training/rider doubling, the supplied Hardened Initiative contribution, explicit Trainer Speed and fastest controlled-Pokemon Trainer Speed fallback with Python-oracle parity.",
                        "Complete trainer initiative entry construction, internal Hardened Initiative computation and final complete initiative-order rebuild/installation remain outside this bounded support."));
        result.put(UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Initiative rebuild now has parity-safe Pokemon entry modifiers, weather/terrain Speed ability contracts, post-Speed bonus resolution and Trainer initiative Speed resolution.",
                        "The complete Python round rebuild, trainer initiative entry construction and broader lifecycle remain incomplete, so Minecraft must not rebuild initiative or promote lifecycle to complete support."));
        result.put(UpstreamCompatibilityMatrix.Capability.ABILITIES,
                new Evidence(
                        UpstreamCompatibilityMatrix.Support.PARTIAL,
                        "Slush Rush, Surge Surfer, Chlorophyll [Errata] and Early Bird [Errata] initiative behavior now have parity-safe Java contracts.",
                        "This is a bounded ability subset and does not complete the PTU ability library."));
        return Map.copyOf(result);
    }
}
