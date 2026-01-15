package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {
    protected ClientLevelMixin() {
        super(null, null, null, null, null, false, false, 0L, 0);
    }

    // 已删除: onTickNonPassenger (那个导致实体被删除的依托答辩)

    @Inject(method = "addParticle*", at = @At("HEAD"), cancellable = true)
    private void onAddParticle(CallbackInfo ci) {
        Performance performance = (Performance) Createplus.FEATURE_MANAGER.getFeature("性能优化");
        if (performance != null && performance.isEnabled() && performance.isDisableParticles()) {
            ci.cancel();
        }
    }
}