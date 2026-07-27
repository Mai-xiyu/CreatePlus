package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.api.CameraClipAPI;

/** 第三人称镜头不被方块挡住。 */
@Mixin(Camera.class)
public class CameraMixin {

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void createplus$cameraClip(double startingDistance, CallbackInfoReturnable<Double> cir) {
        if (CameraClipAPI.isActive()) {
            cir.setReturnValue(startingDistance);
        }
    }
}
