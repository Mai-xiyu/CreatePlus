package org.xiyu.yee.createplus.mixin;

        import com.mojang.blaze3d.vertex.PoseStack;
        import net.minecraft.client.renderer.MultiBufferSource;
        import net.minecraft.client.renderer.entity.EntityRenderer;
        import net.minecraft.world.entity.Entity;
        import net.minecraft.world.entity.LivingEntity;
        import net.minecraft.world.entity.item.FallingBlockEntity;
        import net.minecraft.world.entity.player.Player;
        import org.spongepowered.asm.mixin.Mixin;
        import org.spongepowered.asm.mixin.injection.At;
        import org.spongepowered.asm.mixin.injection.Inject;
        import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
        import org.xiyu.yee.createplus.Createplus;
        import org.xiyu.yee.createplus.features.Performance;

        @Mixin(EntityRenderer.class)
        public class EntityRendererMixin<T extends Entity> {
            @Inject(method = "render", at = @At("HEAD"), cancellable = true)
            private void onRender(T p_114485_, float p_114486_, float p_114487_, PoseStack p_114488_, MultiBufferSource p_114489_, int p_114490_, CallbackInfo ci) {
                Performance performance = (Performance) Createplus.FEATURE_MANAGER.getFeature("性能优化");
                if (performance != null && performance.isEnabled()) {
                    if ((performance.isDisableEntityRendering() && !(p_114485_ instanceof Player)) ||
                        (performance.isDisableDeadMobRendering() && p_114485_ instanceof LivingEntity && ((LivingEntity)p_114485_).isDeadOrDying()) ||
                        (performance.isDisableFallingBlockEntityRendering() && p_114485_ instanceof FallingBlockEntity)) {
                        ci.cancel();
                    }
                }
            }
        }