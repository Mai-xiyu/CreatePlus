package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.api.NoClipAPI;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 穿墙开启时丢掉服务端拉回，但仍回复传送确认，避免连接状态机卡死。
 * <p>
 * 地图画防护不放这里：联机对 {@code handleMapItemData} 的 HEAD cancel 注入
 * 会触发 VerifyError（CallbackInfo / this 错位）。改由 {@link ClientLevelMixin} 拦 overrideMapData。
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
}
