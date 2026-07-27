package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;

/**
 * 按性能设置跳过实体渲染。
 *
 * 注入点选 shouldRender 而不是 EntityRenderer.render：后者只是基类实现
 * （名牌、拴绳），绝大多数渲染器都自己重写了 render 并在最后才调 super，
 * 拦下它只能去掉名牌，模型照画。这里直接让分发器认为实体不该渲染，
 * 连阴影、火焰、碰撞箱一起省掉。
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void createplus$skipEntityRender(Entity entity, Frustum frustum, double camX, double camY, double camZ,
                                             CallbackInfoReturnable<Boolean> cir) {
        Performance performance = Createplus.performance();
        if (performance == null || !performance.isEnabled()) return;

        boolean skip = (performance.isDisableEntityRendering() && !(entity instanceof Player))
            || (performance.isDisableDeadMobRendering()
                && entity instanceof LivingEntity living && living.isDeadOrDying())
            || (performance.isDisableFallingBlockRendering() && entity instanceof FallingBlockEntity);

        if (skip) cir.setReturnValue(false);
    }
}
