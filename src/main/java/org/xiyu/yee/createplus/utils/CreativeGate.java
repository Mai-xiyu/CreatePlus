package org.xiyu.yee.createplus.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 创造模式硬性门禁 + 本机主机上的服务端玩家定位。
 *
 * 挂类功能统一走这里：不是创造就立刻停手，避免生存误开。
 * 局域网/单人里客户端还有一个同 UUID 的 {@link ServerPlayer}，
 * 只改 {@link LocalPlayer} 时服务端仍按原版碰撞/触及算，看起来就像"房主也无效"。
 */
public final class CreativeGate {

    private CreativeGate() {
    }

    public static boolean allow() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isCreative();
    }

    /** 当前客户端玩家，含未加载时的 null。 */
    public static LocalPlayer clientPlayer() {
        return Minecraft.getInstance().player;
    }

    /**
     * 本机集成服上、与本地玩家同 UUID 的服务端玩家。
     * 远端多人服没有这个对象，返回 null。
     */
    public static ServerPlayer integratedServerPlayer() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer local = mc.player;
        if (local == null || !mc.hasSingleplayerServer() || mc.getSingleplayerServer() == null) {
            return null;
        }
        return mc.getSingleplayerServer().getPlayerList().getPlayer(local.getUUID());
    }

    /** 这个实体是不是"就是我"（客户端或本机主机上的服务端分身）。 */
    public static boolean isLocalAvatar(Entity entity) {
        if (!(entity instanceof Player)) return false;
        LocalPlayer local = Minecraft.getInstance().player;
        return local != null && entity.getUUID().equals(local.getUUID());
    }
}
