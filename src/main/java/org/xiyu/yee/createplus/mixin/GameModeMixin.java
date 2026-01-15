package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.api.FreecamAPI;

@Mixin(MultiPlayerGameMode.class)
public class GameModeMixin {
    
    @Inject(method = "isAlwaysFlying", at = @At("HEAD"), cancellable = true)
    private void onIsAlwaysFlying(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamAPI.isFreecam()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getPlayerMode", at = @At("RETURN"), cancellable = true)
    private void onGetPlayerMode(CallbackInfoReturnable<GameType> cir) {
        if (FreecamAPI.isFreecam() && cir.getReturnValue() == GameType.SPECTATOR) {
            // 在灵魂出窍状态下，让旁观模式表现得像创造模式一样
            cir.setReturnValue(GameType.CREATIVE);
        }
    }
} 