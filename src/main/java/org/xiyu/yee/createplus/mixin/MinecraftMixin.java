package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;
import org.xiyu.yee.createplus.features.SpeedBuild;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow private int rightClickDelay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void createplus$overridePlaceDelay(CallbackInfo ci) {
        SpeedBuild speedBuild = Createplus.speedBuild();
        if (speedBuild != null && speedBuild.isEnabled()) {
            rightClickDelay = Math.min(rightClickDelay, speedBuild.getDelay());
        }
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void createplus$inactiveFps(CallbackInfoReturnable<Integer> cir) {
        Performance performance = Createplus.performance();
        Minecraft self = (Minecraft) (Object) this;
        if (performance != null && performance.isInactiveFpsLimit() && !self.isWindowActive()) {
            cir.setReturnValue(performance.getInactiveFps());
        }
    }
}
