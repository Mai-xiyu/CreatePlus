package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;

@Mixin(ParticleEngine.class)
public class ParticleMixin {
    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlock(BlockPos p_107356_, BlockState p_107357_, CallbackInfo ci) {
        Performance performance = (Performance) Createplus.FEATURE_MANAGER.getFeature("性能优化");
        if (performance != null && performance.isEnabled() && performance.isDisableBlockBreakingParticles()) {
            ci.cancel();
        }
    }
} 