package io.autoptu.cobblemon.battlecore;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Fail-closed integration watch for AutoPTU-Java PR #231.
 *
 * <p>The upstream draft freezes Python StatusController cross-system phase ordering only. It does
 * not port concrete held-item/food effects or provide an authoritative runtime envelope that the
 * Minecraft adapter may execute.</p>
 */
public final class StatusControllerPhaseEnvelopeCompatibility {
    public static final int AUTOPTU_JAVA_PR = 231;
    public static final String AUTOPTU_JAVA_MAIN_SHA = "57c7c2a9751cf02facf5d176b9d0f95b996a9bd1";
    public static final String AUTOPTU_JAVA_PR_HEAD_SHA = "74d9e31fcb390531f8837f41985f81923506bcc9";
    public static final String PINNED_PYTHON_ORACLE_SHA = "16d228efa63aabecb67fa788959a359aac7f8f03";

    private static final Set<UpstreamCompatibilityMatrix.Capability> DEPENDENCIES = Collections.unmodifiableSet(
            EnumSet.of(
                    UpstreamCompatibilityMatrix.Capability.FULL_TURN_ROUND_LIFECYCLE,
                    UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE,
                    UpstreamCompatibilityMatrix.Capability.ABILITIES,
                    UpstreamCompatibilityMatrix.Capability.ITEMS,
                    UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS,
                    UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));

    private StatusControllerPhaseEnvelopeCompatibility() {}

    public static Set<UpstreamCompatibilityMatrix.Capability> dependencies() {
        return DEPENDENCIES;
    }

    public static boolean mayProjectOrExecutePhaseEnvelope() {
        return false;
    }

    public static String boundary() {
        return "AutoPTU-Java PR #231 is draft/open at 74d9e31fcb390531f8837f41985f81923506bcc9. "
                + "It freezes pinned-Python StatusController ordering only: START held-item start -> food regen -> food buff start -> combatant phase effects; "
                + "END combatant phase effects -> held-item end; COMMAND/ACTION combatant phase effects only. "
                + "Concrete held-item, food, status, ability and Trainer Feature effects remain separate/incomplete, and no merged generic runtime phase-envelope dispatcher grants Minecraft execution authority. "
                + "Minecraft/Cobblemon may not coordinate PTU phase order, invoke those effects, or synthesize lifecycle events from this draft contract.";
    }
}
