package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.api.FastBreakAPI;
import org.xiyu.yee.createplus.api.FreecamAPI;
import org.xiyu.yee.createplus.api.ReachAPI;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 灵魂出窍飞行锁定、超距选取、创造瞬间破坏。
 */
@Mixin(MultiPlayerGameMode.class)
public class GameModeMixin {

    @Shadow private int destroyDelay;

    @Inject(method = "isAlwaysFlying", at = @At("HEAD"), cancellable = true)
    private void createplus$alwaysFlyInFreecam(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamAPI.isFreecam()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getPickRange", at = @At("HEAD"), cancellable = true)
    private void createplus$extendedPickRange(CallbackInfoReturnable<Float> cir) {
        if (ReachAPI.isActive() && CreativeGate.allow()) {
            cir.setReturnValue((float) ReachAPI.blockReach());
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void createplus$fastBreak(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (FastBreakAPI.isActive() && CreativeGate.allow()) {
            destroyDelay = 0;
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void createplus$fastBreakStart(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (FastBreakAPI.isActive() && CreativeGate.allow()) {
            destroyDelay = 0;
        }
    }
}
