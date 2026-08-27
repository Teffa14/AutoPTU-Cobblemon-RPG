package io.autoptu.cobblemon.battlecore;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Fail-closed integration watch for the StatusController phase envelope.
 *
 * <p>AutoPTU-Java PR #231 is merged and freezes Python StatusController cross-system phase
 * ordering. Draft PR #232 makes that envelope executable inside Java core, but it still does not
 * port concrete held-item or food effects. Neither contract grants Minecraft authority to execute
 * PTU lifecycle rules.</p>
 */
public final class StatusControllerPhaseEnvelopeCompatibility {
    public static final int MERGED_ORDERING_PR = 231;
    public static final int DRAFT_DISPATCHER_PR = 232;
    public static final String AUTOPTU_JAVA_MAIN_SHA = "84505214d4bca41610f36f0a178e675ef0ab26ba";
    public static final String AUTOPTU_JAVA_DRAFT_HEAD_SHA = "fcbded4c966095c24a4f6124a435edcb790f8581";
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
        return "AutoPTU-Java PR #231 is merged on main at 84505214d4bca41610f36f0a178e675ef0ab26ba and freezes pinned-Python StatusController ordering: "
                + "START held-item start -> food regen -> food buff start -> combatant phase effects; END combatant phase effects -> held-item end; COMMAND/ACTION combatant phase effects only. "
                + "Draft PR #232 at fcbded4c966095c24a4f6124a435edcb790f8581 adds a generic Java-core dispatcher for that envelope, while deliberately leaving concrete held-item and food effects unported. "
                + "Status skip handling remains core-owned inside the combatant phase family. Minecraft/Cobblemon may not coordinate PTU phase order, execute held-item/food/status/ability/Trainer Feature effects, or synthesize lifecycle events from either contract.";
    }
}
