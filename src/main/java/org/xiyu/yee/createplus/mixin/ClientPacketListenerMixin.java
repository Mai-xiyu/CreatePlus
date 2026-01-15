package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    
    @Inject(method = "handleSetEquipment", at = @At("RETURN"))
    private void onEquipmentPacket(ClientboundSetEquipmentPacket packet, CallbackInfo ci) {

    }
} 