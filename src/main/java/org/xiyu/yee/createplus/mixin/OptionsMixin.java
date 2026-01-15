package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.utils.GammaOption;

@Mixin(Options.class)
public class OptionsMixin {
    @Inject(method = "gamma", at = @At("HEAD"), cancellable = true)
    private void onGetGamma(CallbackInfoReturnable<OptionInstance<Double>> cir) {
        // 创建一个新的亮度选项实例，允许更高的最大值
        cir.setReturnValue(GammaOption.createGammaOption((Options)(Object)this));
    }
} 