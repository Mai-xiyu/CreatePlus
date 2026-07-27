package org.xiyu.yee.createplus.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;

@Mixin(LevelRenderer.class)
public class LevelRendererSkyMixin {
    @Unique private int createplus$skyCheck;
    @Unique private boolean createplus$underwater;
    @Unique private final BlockPos.MutableBlockPos createplus$lastPos = new BlockPos.MutableBlockPos();

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void createplus$skipUnderwaterSky(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick,
                                              Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        Performance performance = Createplus.performance();
        if (performance == null || !performance.isSkipUnderwaterSky()) return;

        if (++createplus$skyCheck % 5 == 0 || !createplus$lastPos.equals(camera.getBlockPosition())) {
            BlockPos pos = camera.getBlockPosition();
            createplus$lastPos.set(pos);
            FluidState fluid = camera.getEntity().level().getFluidState(pos);
            createplus$underwater = fluid.is(FluidTags.WATER);
        }
        if (createplus$underwater) ci.cancel();
    }
}
