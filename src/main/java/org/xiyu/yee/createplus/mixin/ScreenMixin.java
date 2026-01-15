package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;

@Mixin(Screen.class)
    public class ScreenMixin {
    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
            private void onClose(CallbackInfo ci) {
            Performance performance = (Performance) Createplus.FEATURE_MANAGER.getFeature("性能优化");
                if (performance != null && performance.isEnabled() && performance.isDisablePortalGuiClosing() &&
                    Minecraft.getInstance().player != null &&
                    Minecraft.getInstance().player.hasEffect(MobEffects.CONFUSION)) {
                ci.cancel();
        }
    }
}