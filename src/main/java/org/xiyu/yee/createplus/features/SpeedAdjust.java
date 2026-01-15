package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import org.lwjgl.glfw.GLFW;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.util.UUID;

public class SpeedAdjust extends CreativePlusFeature implements SubHUDFeature {
    private final Map<SpeedType, Float> speedMultipliers = new HashMap<>();
    private SpeedType selectedType = SpeedType.WALK;
    private boolean showSubHUD = false;
    private float spinBotSpeed = 1.0f;
    private static final float MIN_SPINBOT_SPEED = 0.1f;
    private static final float MAX_SPINBOT_SPEED = 10.0f;
    private static final float SPINBOT_SPEED_STEP = 0.1f;
    
    // 为每种速度类型创建唯一的UUID
    private static final UUID SWIM_SPEED_UUID = UUID.fromString("9c33c313-0d71-4d44-b033-6c7fbaa5f034");
    private static final UUID ELYTRA_SPEED_UUID = UUID.fromString("3b8c1e6e-32dd-4b51-a807-7c5c0a4485c5");
    private static final UUID RIDE_SPEED_UUID = UUID.fromString("2c2d6933-3e3a-4b40-a923-b4906d7d2b5a");
    private static final UUID SPINBOT_SPEED_UUID = UUID.fromString("1d2e3f4a-5b6c-7d8e-9f0a-1b2c3d4e5f6a");

    public enum SpeedType {
        WALK("行走速度", 0.1f, 10.0f, 1.0f),
        FLY("飞行速度", 0.1f, 10.0f, 1.0f),
        RIDE("骑乘速度", 0.1f, 10.0f, 1.0f),
        SWIM("游泳速度", 0.1f, 10.0f, 1.0f),
        ELYTRA("鞘翅速度", 0.1f, 10.0f, 1.0f),
        SPINBOT("陀螺速度", MIN_SPINBOT_SPEED, MAX_SPINBOT_SPEED, 1.0f);

        final String name;
        final float min;
        final float max;
        final float defaultValue;

        SpeedType(String name, float min, float max, float defaultValue) {
            this.name = name;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
        }
    }

    public SpeedAdjust() {
        super("speed_adjust", "speed_adjust");
        for (SpeedType type : SpeedType.values()) {
            speedMultipliers.put(type, type.defaultValue);
        }
    }

    @Override
    public void handleClick(boolean isRightClick) {
        // 不需要处理点击事件
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder(super.getDescription());
        if (isEnabled()) {
            desc.append("\n§7当前选择: §e").append(selectedType.name);
            if (showSubHUD) {
                desc.append("\n§7↑↓ 选择速度类型");
                desc.append("\n§7+/- 调整速度值");
                desc.append("\n§7← 返回");
            }
        } else {
            desc.append("\n§7回车键启用");
        }
        return desc.toString();
    }


    @Override
    public boolean handleKeyPress(int keyCode) {
        if (!isEnabled()) return false;

        // 注意：现在 FeatureScreen 会处理 "进入/退出" 逻辑
        // 这里主要处理数值调整
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP:
                selectPreviousType();
                return true;
            case GLFW.GLFW_KEY_DOWN:
                selectNextType();
                return true;
            case GLFW.GLFW_KEY_EQUAL:      // =/+
            case GLFW.GLFW_KEY_KP_ADD:
            case GLFW.GLFW_KEY_RIGHT:      // 允许用右键增加
                adjustCurrentSpeed(0.1f);
                return true;
            case GLFW.GLFW_KEY_MINUS:      // -/_
            case GLFW.GLFW_KEY_KP_SUBTRACT:
            case GLFW.GLFW_KEY_LEFT:       // FeatureScreen会拦截左键用于返回，所以这里可能接不到左键，没关系
                adjustCurrentSpeed(-0.1f);
                return true;
        }
        return false;
    }

    @Override
    public boolean isSubHUDVisible() {
        return showSubHUD;
    }

    @Override
    public void toggleSubHUD() {
        showSubHUD = !showSubHUD;
    }



    @Override
    public void renderSubHUD(GuiGraphics graphics, int x, int y) {
        if (!showSubHUD) return;

        int width = 120; // 子菜单宽度
        int itemHeight = 14;
        int totalHeight = SpeedType.values().length * itemHeight + 14; // +14 for header

        // 1. 绘制背景 (与主菜单风格一致)
        RenderUtils.drawRect(graphics, x, y, width, totalHeight, 0x90000000);

        // 2. 绘制标题 "参数设置"
        RenderUtils.drawRect(graphics, x, y, width, itemHeight, 0xFF000000); // 标题黑底
        graphics.drawString(Minecraft.getInstance().font, "速度设置", x + 4, y + 3, 0xFF00A2FF, false);

        int currentY = y + itemHeight;

        // 3. 绘制每个选项
        for (int i = 0; i < SpeedType.values().length; i++) {
            SpeedType type = SpeedType.values()[i];

            // 选中高亮
            if (type == selectedType) {
                RenderUtils.drawRect(graphics, x, currentY, width, itemHeight, 0xFF00A2FF);
            }

            // 绘制文字 (名称: 数值)
            // 这里的 type.name 是 "行走速度" 等中文
            String text = String.format("%s: %.1f", type.name, speedMultipliers.get(type));
            int color = (type == selectedType) ? 0xFFFFFFFF : 0xFFAAAAAA;

            graphics.drawString(Minecraft.getInstance().font, text, x + 4, currentY + 3, color, false);

            currentY += itemHeight;
        }
    }

    private void selectPreviousType() {
        int prevIndex = (selectedType.ordinal() - 1 + SpeedType.values().length) % SpeedType.values().length;
        selectedType = SpeedType.values()[prevIndex];
    }

    private void selectNextType() {
        int nextIndex = (selectedType.ordinal() + 1) % SpeedType.values().length;
        selectedType = SpeedType.values()[nextIndex];
    }

    private void adjustCurrentSpeed(float delta) {
        float current = speedMultipliers.get(selectedType);
        setSpeedMultiplier(selectedType, 
            Math.max(selectedType.min, 
            Math.min(selectedType.max, current + delta)));
    }

    private void saveSettings() {
        // 这里可以添加保存到配置文件的逻辑
        // 目前只是保存在内存中
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            showSubHUD = false;  // 启用时不显示面板，等待用户按右方向键打开
            for (SpeedType type : SpeedType.values()) {
                setSpeedMultiplier(type, 2.0f);
            }
        }
    }
    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // 恢复默认速度
            for (SpeedType type : SpeedType.values()) {
                setSpeedMultiplier(type, type.defaultValue);
            }
            // 关闭子HUD
            showSubHUD = false;
        }
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // 检查并应用游泳速度
        if (player.isSwimming() || player.isInWater()) {
            float swimSpeed = speedMultipliers.getOrDefault(SpeedType.SWIM, 1.0f);
            // 直接修改玩家速度向量
            if (swimSpeed != 1.0f) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(swimSpeed, swimSpeed, swimSpeed));
            }
        }

        // 检查并应用鞘翅速度
        if (player.isFallFlying()) {
            float elytraSpeed = speedMultipliers.getOrDefault(SpeedType.ELYTRA, 1.0f);
            player.setDeltaMovement(player.getDeltaMovement().multiply(elytraSpeed, 1.0, elytraSpeed));
        }

        // 检查并应用骑乘速度
        if (player.getVehicle() instanceof LivingEntity mount) {
            float rideSpeed = speedMultipliers.getOrDefault(SpeedType.RIDE, 1.0f);
            if (rideSpeed != 1.0f) {
                mount.setDeltaMovement(mount.getDeltaMovement().multiply(rideSpeed, 1.0, rideSpeed));
            }
        }
    }

    /**
     * 设置指定类型的速度倍率
     * @param type 速度类型
     * @param multiplier 速度倍率
     */
    private void setSpeedMultiplier(SpeedType type, float multiplier) {
        // 确保倍率在有效范围内
        multiplier = Math.max(type.min, Math.min(type.max, multiplier));
        // 保存速度值
        speedMultipliers.put(type, multiplier);
        // 应用速度
        applySpeed(type, multiplier);
    }

    private void applySpeed(SpeedType type, float value) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        switch (type) {
            case WALK:
                player.getAbilities().setWalkingSpeed(0.1f * value);
                player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.1 * value);
                break;

            case FLY:
                player.getAbilities().setFlyingSpeed(0.05f * value);
                break;

            case SWIM:
                speedMultipliers.put(SpeedType.SWIM, value);
                break;

            case ELYTRA:
                speedMultipliers.put(SpeedType.ELYTRA, value);
                break;

            case RIDE:
                speedMultipliers.put(SpeedType.RIDE, value);
                break;

            case SPINBOT:
                speedMultipliers.put(SpeedType.SPINBOT, value);
                break;
        }

        player.onUpdateAbilities();
    }

    public float getSpinBotSpeed() {
        return speedMultipliers.getOrDefault(SpeedType.SPINBOT, SpeedType.SPINBOT.defaultValue);
    }

    public float getSpeedValue(SpeedType type) {
        return speedMultipliers.getOrDefault(type, type.defaultValue);
    }

    public void setSpeedValue(SpeedType type, float value) {
        setSpeedMultiplier(type, value);
    }
} 