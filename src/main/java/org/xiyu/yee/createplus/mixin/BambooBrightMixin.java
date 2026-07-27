package org.xiyu.yee.createplus.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;

@Mixin(BlockBehaviour.class)
public class BambooBrightMixin {
    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
    private void createplus$bambooFullBright(BlockState state, BlockGetter level, BlockPos pos,
                                             CallbackInfoReturnable<Float> cir) {
        if (!(state.getBlock() instanceof BambooStalkBlock)) return;
        Performance performance = Createplus.performance();
        if (performance != null && performance.isBambooFullBright()) {
            cir.setReturnValue(1.0F);
        }
    }
}
