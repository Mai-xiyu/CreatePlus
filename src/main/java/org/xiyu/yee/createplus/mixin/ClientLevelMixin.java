package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    // 两个重载都要拦，写全描述符而不是 "addParticle*"：
    // 通配符要是没匹配上，Mixin 会在启动时直接抛错
    @Inject(
        method = {
            "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V"
        },
        at = @At("HEAD"),
        cancellable = true)
    private void createplus$skipParticles(CallbackInfo ci) {
        Performance performance = Createplus.performance();
        if (performance != null && performance.isEnabled() && performance.isDisableParticles()) {
            ci.cancel();
        }
    }
}
