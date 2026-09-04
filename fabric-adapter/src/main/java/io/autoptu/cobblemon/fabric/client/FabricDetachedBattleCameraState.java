package io.autoptu.cobblemon.fabric.client;

import io.autoptu.cobblemon.fabric.network.FabricBattleCameraMode;
import io.autoptu.cobblemon.fabric.network.FabricBattleCameraPayload;

/**
 * Client presentation state for a camera detached from the player's eyes.
 *
 * <p>This class consumes only server-authored physical framing data. It never reads or derives PTU
 * legality, turn ownership, damage, HP, targeting, faint state or battle results.
 */
public final class FabricDetachedBattleCameraState {
    private static final double TRANSITION_ALPHA = 0.28D;

    private static boolean active;
    private static String battleId = "";
    private static FabricBattleCameraMode mode = FabricBattleCameraMode.TACTICAL_AERIAL;

    private static double focusX;
    private static double focusY;
    private static double focusZ;
    private static float yaw;
    private static float pitch;
    private static double distance;

    private static double targetFocusX;
    private static double targetFocusY;
    private static double targetFocusZ;
    private static float targetYaw;
    private static float targetPitch;
    private static double targetDistance;

    private FabricDetachedBattleCameraState() {}

    public static synchronized void apply(FabricBattleCameraPayload payload) {
        if (payload == null || !payload.active()) {
            clear();
            return;
        }

        battleId = payload.battleId();
        mode = payload.mode();
        targetFocusX = payload.focusX();
        targetFocusY = payload.focusY();
        targetFocusZ = payload.focusZ();
        targetYaw = normalizeYaw(payload.yaw());

        switch (payload.mode()) {
            case TACTICAL_AERIAL -> {
                targetPitch = 55.0F;
                targetDistance = clamp(Math.max(payload.width(), payload.height()) * 1.45D + 6.0D, 12.0D, 24.0D);
            }
            case TRAINER_EXTERNAL -> {
                targetPitch = 25.0F;
                targetDistance = 7.0D;
            }
            case ACTION_CINEMATIC -> {
                targetPitch = 32.0F;
                targetDistance = 9.0D;
            }
        }

        if (!active) {
            focusX = targetFocusX;
            focusY = targetFocusY;
            focusZ = targetFocusZ;
            yaw = targetYaw;
            pitch = targetPitch;
            distance = targetDistance;
        }
        active = true;
    }

    public static synchronized void tick() {
        if (!active) return;
        focusX = lerp(focusX, targetFocusX, TRANSITION_ALPHA);
        focusY = lerp(focusY, targetFocusY, TRANSITION_ALPHA);
        focusZ = lerp(focusZ, targetFocusZ, TRANSITION_ALPHA);
        yaw = lerpYaw(yaw, targetYaw, (float) TRANSITION_ALPHA);
        pitch = (float) lerp(pitch, targetPitch, TRANSITION_ALPHA);
        distance = lerp(distance, targetDistance, TRANSITION_ALPHA);
    }

    public static synchronized void clear() {
        active = false;
        battleId = "";
        mode = FabricBattleCameraMode.TACTICAL_AERIAL;
    }

    public static synchronized Snapshot snapshot() {
        if (!active) return null;
        return new Snapshot(battleId, mode, focusX, focusY, focusZ, yaw, pitch, distance);
    }

    private static double lerp(double current, double target, double alpha) {
        return current + (target - current) * alpha;
    }

    private static float lerpYaw(float current, float target, float alpha) {
        float delta = normalizeYaw(target - current);
        if (delta > 180.0F) delta -= 360.0F;
        return normalizeYaw(current + delta * alpha);
    }

    private static float normalizeYaw(float value) {
        float normalized = value % 360.0F;
        return normalized < 0.0F ? normalized + 360.0F : normalized;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Snapshot(
            String battleId,
            FabricBattleCameraMode mode,
            double focusX,
            double focusY,
            double focusZ,
            float yaw,
            float pitch,
            double distance
    ) {}
}
