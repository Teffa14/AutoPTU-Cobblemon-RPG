package io.autoptu.cobblemon.fabric.mixin.client;

import io.autoptu.cobblemon.fabric.client.FabricDetachedBattleCameraState;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the presentation-only detached AutoPTU battle camera after vanilla camera placement. */
@Mixin(Camera.class)
public abstract class BattleDetachedCameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At("TAIL"))
    private void autoptu$applyDetachedBattleCamera(CallbackInfo ci) {
        FabricDetachedBattleCameraState.Snapshot camera = FabricDetachedBattleCameraState.snapshot();
        if (camera == null) return;

        double pitchRadians = Math.toRadians(camera.pitch());
        double yawRadians = Math.toRadians(camera.yaw());
        double forwardX = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double forwardY = -Math.sin(pitchRadians);
        double forwardZ = Math.cos(yawRadians) * Math.cos(pitchRadians);

        setRotation(camera.yaw(), camera.pitch());
        setPos(
                camera.focusX() - forwardX * camera.distance(),
                camera.focusY() - forwardY * camera.distance(),
                camera.focusZ() - forwardZ * camera.distance()
        );
    }
}
