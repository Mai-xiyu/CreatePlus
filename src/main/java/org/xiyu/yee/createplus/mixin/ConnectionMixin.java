package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.api.FreecamAPI;

@Mixin(ClientPacketListener.class)
public class ConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onSendMovePacket(Packet<?> packet, CallbackInfo ci) {
        // If in freecam mode, cancel sending the move packet
        if (packet instanceof ServerboundMovePlayerPacket && FreecamAPI.isFreecam()) {
            ci.cancel();
        }
    }
}