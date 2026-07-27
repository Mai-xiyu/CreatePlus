package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.api.NoClipAPI;
import org.xiyu.yee.createplus.mapart.MapArtStore;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 穿墙开启时丢掉服务端拉回，但仍回复传送确认，避免连接状态机卡死。
 * 同时保护 CreatePlus 高位地图画 ID，不被服务端 map 包覆盖。
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void createplus$ignoreRubberband(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if (!NoClipAPI.isActive() || !NoClipAPI.ignoreRubberband() || !CreativeGate.allow()) return;

        ClientPacketListener self = (ClientPacketListener) (Object) this;
        self.send(new ServerboundAcceptTeleportationPacket(packet.getId()));

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            self.send(new ServerboundMovePlayerPacket.PosRot(
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), false));
        }
        ci.cancel();
    }

    @Inject(method = "handleMapItemData", at = @At("HEAD"), cancellable = true)
    private void createplus$protectMapArt(ClientboundMapItemDataPacket packet, CallbackInfo ci) {
        if (MapArtStore.isOurId(packet.getMapId())) {
            ci.cancel();
        }
    }
}
