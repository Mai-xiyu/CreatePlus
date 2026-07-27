package org.xiyu.yee.createplus.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;

@Mixin(EntityRenderDispatcher.class)
public class EntityOutlineSkipMixin {
    @Inject(
        method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void createplus$skipUselessOutline(Entity entity, double x, double y, double z, float rotationYaw,
                                               float partialTicks, PoseStack poseStack,
                                               MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        Performance performance = Createplus.performance();
        if (performance != null && performance.isSkipOutlineWhenNoGlow()
            && buffer instanceof OutlineBufferSource && !entity.isCurrentlyGlowing()) {
            ci.cancel();
        }
    }
}
