package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.DoubleSetting;

/**
 * 让服务端看到的角色持续旋转，本地视角不受影响。
 *
 * 纯玩票功能，但也能用来给建筑录环绕镜头。
 */
public class SpinBot extends CreativePlusFeature {

    private final DoubleSetting degreesPerTick = doubleSetting("degrees_per_tick", 20.0D, 1.0D, 90.0D, 1.0D);
    private final BooleanSetting rotateBody = boolSetting("rotate_body", true);

    private float spinYaw;

    public SpinBot() {
        super("spinbot", "身体持续旋转（仅服务端视角）", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        LocalPlayer player = player();
        spinYaw = player != null ? player.getYRot() : 0.0F;
    }

    @Override
    public void onDisable() {
        // 把真实朝向同步回服务端，否则角色会停在一个奇怪的角度
        LocalPlayer player = player();
        if (player == null || player.connection == null) return;
        player.connection.send(new ServerboundMovePlayerPacket.Rot(
            player.getYRot(), player.getXRot(), player.onGround()));
        player.yBodyRot = player.getYRot();
        player.yHeadRot = player.getYRot();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.connection == null) return;

        spinYaw = wrap(spinYaw + (float) degreesPerTick.value());

        player.connection.send(new ServerboundMovePlayerPacket.Rot(
            spinYaw, player.getXRot(), player.onGround()));

        if (rotateBody.get()) {
            player.yBodyRot = spinYaw;
            player.yHeadRot = spinYaw;
        }
    }

    private static float wrap(float yaw) {
        float value = yaw % 360.0F;
        return value < 0.0F ? value + 360.0F : value;
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + String.format("\n§7转速: §f%.0f°/tick", degreesPerTick.value())
            + "\n§7第三人称下能看到效果";
    }
}
