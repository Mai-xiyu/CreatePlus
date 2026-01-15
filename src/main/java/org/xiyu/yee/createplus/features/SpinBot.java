package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.Createplus;

public class SpinBot extends CreativePlusFeature {
    // 使用一个独立的变量来记录旋转角度，而不是读取玩家当前的Yaw
    private float spinningYaw = 0.0f;
    private SpeedAdjust speedAdjust;
    private static final float DEFAULT_SPIN_SPEED = 2.0f;

    public SpinBot() {
        super("spinbot", "spinbot");
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 获取旋转速度
        float spinSpeed = speedAdjust != null ?
                speedAdjust.getSpinBotSpeed() : DEFAULT_SPIN_SPEED;

        // 更新旋转角度 (独立于玩家视角)
        spinningYaw += (10.0f * spinSpeed);

        // 确保角度在 0-360 范围内
        if (spinningYaw > 360.0f) {
            spinningYaw -= 360.0f;
        }

        // 关键修改：不要设置 mc.player.setYRot(newYaw)
        // 而是只发送数据包给服务器，告诉服务器我们在旋转
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                    spinningYaw,
                    mc.player.getXRot(), // 保持当前的抬头/低头角度
                    mc.player.onGround()
            ));
        }

        // 可选：为了让本地玩家在第三人称下看到身体旋转，设置 bodyRot
        // (这不会影响第一人称视角)
        mc.player.yBodyRot = spinningYaw;
        mc.player.yHeadRot = spinningYaw;
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder(super.getDescription());
        if (isEnabled() && speedAdjust != null) {
            desc.append("\n§7当前速度: §e").append(String.format("%.1f", speedAdjust.getSpinBotSpeed()));
            desc.append("\n§7在速度调整中可以调节旋转速度");
        }
        return desc.toString();
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);

        if (speedAdjust == null) {
            speedAdjust = (SpeedAdjust) Createplus.FEATURE_MANAGER.getFeatures().stream()
                    .filter(f -> f instanceof SpeedAdjust)
                    .findFirst()
                    .orElse(null);

            if (speedAdjust != null) {
                speedAdjust.setSpeedValue(SpeedAdjust.SpeedType.SPINBOT, DEFAULT_SPIN_SPEED);
            }
        }

        if (Minecraft.getInstance().player != null) {
            // 初始化旋转角度为当前角度
            spinningYaw = Minecraft.getInstance().player.getYRot();
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("message.createplus.spinbot.enabled"),
                    true
            );
        }
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null) {
            // 恢复正常的视角数据同步
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                    mc.player.getYRot(),
                    mc.player.getXRot(),
                    mc.player.onGround()
            ));
        }
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // 如果是渲染本地玩家，且功能已启用
        if (event.getEntity() == Minecraft.getInstance().player && isEnabled()) {
            // 这里不需要手动旋转 PoseStack 了，因为我们在 onTick 里设置了 yBodyRot 和 yHeadRot
            // Minecraft 的渲染器会自动处理这些字段。
            // 之前的 mulPose 可能会导致双重旋转或者坐标系混乱。

            // 如果一定要手动干预渲染，可以在这里写，但通常设置 yBodyRot 就够了。
        }
    }
}