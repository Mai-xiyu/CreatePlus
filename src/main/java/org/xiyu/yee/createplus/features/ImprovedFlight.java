package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class ImprovedFlight extends CreativePlusFeature {
    private static final float DEFAULT_FLY_SPEED = 0.05f;
    private float originalFlySpeed;

    public ImprovedFlight() {
        super("improved_flight", "提供更快的飞行速度和更精确的控制");
    }

    @Override
    public void onEnable() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            originalFlySpeed = player.getAbilities().getFlyingSpeed();
            player.getAbilities().setFlyingSpeed(DEFAULT_FLY_SPEED * 5.0f);
        }
    }

    @Override
    public void onDisable() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.getAbilities().setFlyingSpeed(originalFlySpeed);
        }
    }

    @Override
    public void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        // 检查是否有任何移动输入
        boolean hasInput = player.input.forwardImpulse != 0 ||
                player.input.leftImpulse != 0 ||
                player.input.jumping ||
                player.input.shiftKeyDown;

        if (hasInput) {
            // 如果有输入（特别是跳跃），保持原有的加速逻辑
            if (player.input.jumping) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1.1, 1.1, 1.1));
            }
        } else {
            // 如果没有任何控制输入，直接消除惯性，立刻停止
            player.setDeltaMovement(Vec3.ZERO);
        }
    }
}