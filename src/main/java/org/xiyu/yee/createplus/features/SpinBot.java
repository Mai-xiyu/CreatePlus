package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.client.player.LocalPlayer;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.SpeedAdjust;

public class SpinBot extends CreativePlusFeature {
    private float lastYaw = 0.0f;
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

        // 使用 SpeedAdjust 中的速度设置，如果未找到则使用默认速度
        float spinSpeed = speedAdjust != null ? 
            speedAdjust.getSpinBotSpeed() : DEFAULT_SPIN_SPEED;
        
        // 更新旋转
        lastYaw = mc.player.getYRot();
        float newYaw = lastYaw + (10.0f * spinSpeed);
        
        // 确保角度在 0-360 范围内
        if (newYaw > 360.0f) {
            newYaw -= 360.0f;
        }

        // 设置新的旋转角度
        mc.player.setYRot(newYaw);

        // 发送旋转数据包到服务器
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                newYaw,
                mc.player.getXRot(),
                mc.player.onGround()
            ));
        }
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
        
        // 延迟初始化 SpeedAdjust
        if (speedAdjust == null) {
            speedAdjust = (SpeedAdjust) Createplus.FEATURE_MANAGER.getFeatures().stream()
                .filter(f -> f instanceof SpeedAdjust)
                .findFirst()
                .orElse(null);

            // 设置初始速度
            if (speedAdjust != null) {
                speedAdjust.setSpeedValue(SpeedAdjust.SpeedType.SPINBOT, DEFAULT_SPIN_SPEED);
            }
        }

        if (Minecraft.getInstance().player != null) {
            lastYaw = Minecraft.getInstance().player.getYRot();
            Minecraft.getInstance().player.displayClientMessage(
                Component.translatable("message.createplus.spinbot.enabled"), 
                true
            );
        }
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this); // 取消注册事件监听器
        // 发送最后一个旋转数据包，确保其他玩家看到正确的朝向
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                mc.player.getYRot(),
                mc.player.getXRot(),
                mc.player.onGround()
            ));
        }
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity() == Minecraft.getInstance().player && isEnabled()) {
            // 在渲染时设置玩家模型的旋转
            event.getPoseStack().mulPose(
                com.mojang.math.Axis.YP.rotationDegrees(lastYaw)
            );
        }
    }
} 