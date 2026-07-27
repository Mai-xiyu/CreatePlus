package org.xiyu.yee.createplus.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.api.NoClipAPI;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 穿墙：跳过本地玩家（以及本机主机上同 UUID 的 ServerPlayer）的方块碰撞。
 *
 * 旧实现只判断 {@code == Minecraft.player}，局域网房主的服务端分身仍在撞墙，
 * 于是客户端刚钻进去就被拉回来 —— 看起来像"房主也无效"。
 */
@Mixin(Entity.class)
public class EntityCollisionMixin {

    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void createplus$skipCollision(Vec3 motion, CallbackInfoReturnable<Vec3> cir) {
        if (!NoClipAPI.isActive() || !CreativeGate.allow()) return;
        if (CreativeGate.isLocalAvatar((Entity) (Object) this)) {
            cir.setReturnValue(motion);
        }
    }
}
