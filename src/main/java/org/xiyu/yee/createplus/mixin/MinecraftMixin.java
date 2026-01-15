package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.features.SpeedBuild;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow private int rightClickDelay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // 检查SpeedBuild功能是否启用
        if (Createplus.FEATURE_MANAGER.getFeatures().stream()
                .filter(f -> f instanceof SpeedBuild)
                .findFirst()
                .map(CreativePlusFeature::isEnabled)
                .orElse(false)) {
            rightClickDelay = 0;
        }
    }
} 