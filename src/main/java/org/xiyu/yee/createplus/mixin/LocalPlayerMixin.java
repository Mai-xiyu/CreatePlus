package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.api.FreecamAPI;

/**
 * 灵魂出窍期间把本地玩家伪装成旁观者。
 *
 * 原版 LocalPlayer.tick() 每刻都会执行 noPhysics = isSpectator()，
 * 所以只要这里返回 true 就自动获得穿墙能力，不需要另外的碰撞 mixin。
 * 只影响客户端自己这一个实体，其他玩家的判定保持原样。
 */
@Mixin(AbstractClientPlayer.class)
public class LocalPlayerMixin {

    @Inject(method = "isSpectator", at = @At("RETURN"), cancellable = true)
    private void createplus$spectatorInFreecam(CallbackInfoReturnable<Boolean> cir) {
        if (!FreecamAPI.isFreecam()) return;
        Entity self = (Entity) (Object) this;
        if (self == Minecraft.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}
