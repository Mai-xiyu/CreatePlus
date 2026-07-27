package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.api.FreecamAPI;

/**
 * 灵魂出窍期间不把移动数据包发出去，服务端因此认为玩家一直站在原地。
 */
@Mixin(ClientPacketListener.class)
public class ConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void createplus$holdMovementInFreecam(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ServerboundMovePlayerPacket && FreecamAPI.isFreecam()) {
            ci.cancel();
        }
    }
}
