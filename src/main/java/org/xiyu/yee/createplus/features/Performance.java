package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import org.xiyu.yee.createplus.ui.PerformanceSettingsScreen;

public class Performance extends CreativePlusFeature {
    private boolean disableBlockBreakingParticles = false;
    private boolean disableDeadMobRendering = false;
    private boolean disableEntityRendering = false;
    private boolean disableFallingBlockEntityRendering = false;
    private boolean disableParticles = false;
    private boolean disablePortalGuiClosing = false;

    public Performance() {
        super("performance", "提供多种性能优化选项");
    }

    public void openSettings(Screen parent) {
        Minecraft.getInstance().setScreen(new PerformanceSettingsScreen(parent, this));
    }

    public Button createSettingsButton(Screen parent) {
        return Button.builder(
            Component.literal("性能优化..."),
            button -> Minecraft.getInstance().setScreen(new PerformanceSettingsScreen(parent, this))
        ).bounds(0, 0, 200, 20).build();
    }

    // Getters and Setters
    public boolean isDisableBlockBreakingParticles() {
        return disableBlockBreakingParticles;
    }

    public void setDisableBlockBreakingParticles(boolean value) {
        this.disableBlockBreakingParticles = value;
    }

    public boolean isDisableDeadMobRendering() {
        return disableDeadMobRendering;
    }

    public void setDisableDeadMobRendering(boolean value) {
        this.disableDeadMobRendering = value;
    }

    public boolean isDisableEntityRendering() {
        return disableEntityRendering;
    }

    public void setDisableEntityRendering(boolean value) {
        this.disableEntityRendering = value;
    }

    public boolean isDisableFallingBlockEntityRendering() {
        return disableFallingBlockEntityRendering;
    }

    public void setDisableFallingBlockEntityRendering(boolean value) {
        this.disableFallingBlockEntityRendering = value;
    }

    public boolean isDisableParticles() {
        return disableParticles;
    }

    public void setDisableParticles(boolean value) {
        this.disableParticles = value;
    }

    public boolean isDisablePortalGuiClosing() {
        return disablePortalGuiClosing;
    }

    public void setDisablePortalGuiClosing(boolean value) {
        this.disablePortalGuiClosing = value;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + "\n§7在选项中可以找到性能优化设置";
    }

    @Override
    public void handleClick(boolean isRightClick) {
        // 移除左键打开设置界面的功能
        // 现在只能通过选项菜单中的按钮打开设置界面
    }

    @Override
    public void onTick() {
        // 不需要tick更新
    }

    @Override
    public void onEnable() {
        // 不需要特殊初始化
    }

    @Override
    public void onDisable() {
        // 不需要特殊清理
    }
} 