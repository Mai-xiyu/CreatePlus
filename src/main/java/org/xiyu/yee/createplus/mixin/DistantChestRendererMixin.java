package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;

@Mixin(BlockEntityRenderDispatcher.class)
public class DistantChestRendererMixin {
    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void createplus$skipDistantChests(E entity,
                                                                      CallbackInfoReturnable<BlockEntityRenderer<E>> cir) {
        Performance performance = Createplus.performance();
        if (performance == null || !performance.isDistantChestSkip() || entity == null || entity.isRemoved()) {
            return;
        }
        if (!(entity instanceof ChestBlockEntity || entity instanceof EnderChestBlockEntity)) return;

        Level level = entity.getLevel();
        BlockPos pos = entity.getBlockPos();
        if (level == null) return;

        double max = performance.getDistantChestChunks() * 16.0D;
        Player nearest = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, max, false);
        if (nearest == null || nearest.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > max * max) {
            cir.setReturnValue(null);
        }
    }
}
