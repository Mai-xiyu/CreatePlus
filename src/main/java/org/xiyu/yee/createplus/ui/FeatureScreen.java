package org.xiyu.yee.createplus.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.features.SubHUDFeature;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.ConfigManager;

import java.util.List;

public class FeatureScreen {
    private static boolean visible = false;
    private static float opacity = 1.0f;
    private int selectedIndex = 0;
    private float targetScrollY = 0;
    private float currentScrollY = 0;
    private static final float SCROLL_SPEED = 0.3f;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int SELECTED_COLOR = 0x40FFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int DESCRIPTION_COLOR = 0xFFAAAAAA;
    
    private boolean isInSubHUD = false;  // 是否在子HUD中
    private SubHUDFeature currentSubHUD = null;  // 当前活动的子HUD
    
    // 添加新的成员变量
    private int panelWidth = 200;
    private int panelHeight = 300;
    private List<CreativePlusFeature> features;

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        features = Createplus.FEATURE_MANAGER.getFeatures();
        if (features.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 平滑滚动动画
        currentScrollY += (targetScrollY - currentScrollY) * SCROLL_SPEED;

        // 主面板
        panelWidth = 200;
        panelHeight = Math.min(300, screenHeight - 40);
        int panelX = 10;
        int panelY = (screenHeight - panelHeight) / 2;

        // 使用透明度渲染背景
        int bgAlpha = (int)(opacity * 0x80);
        int bgColor = (bgAlpha << 24) | (BACKGROUND_COLOR & 0xFFFFFF);
        renderRoundedRect(graphics, panelX, panelY, panelX + panelWidth, panelY + panelHeight, bgColor);

        // 渲染标题
        graphics.drawString(mc.font, 
            Component.translatable("createplus.title"),
            panelX + 5, panelY + 5, 0xFFFFFFFF);

        // 设置裁剪区域
        int contentY = panelY + 20;
        int contentHeight = panelHeight - 25;
        graphics.enableScissor(panelX, contentY, panelX + panelWidth, contentY + contentHeight);

        // 渲染功能列表
        int itemHeight = 20;
        int y = contentY - (int)currentScrollY;
        
        for (int i = 0; i < features.size(); i++) {
            CreativePlusFeature feature = features.get(i);
            
            if (y + itemHeight >= contentY && y <= contentY + contentHeight) {
                if (i == selectedIndex && !isInSubHUD) {
                    int selectedAlpha = (int)(opacity * 0x40);
                    int selectedColor = (selectedAlpha << 24) | (SELECTED_COLOR & 0xFFFFFF);
                    renderRoundedRect(graphics, panelX, y, panelX + panelWidth, y + itemHeight, selectedColor);
                }
                Component text = Component.translatable(feature.getTranslationKey())
                    .append(" ")
                    .append(Component.translatable(feature.isEnabled() ? 
                        "createplus.status.enabled" : 
                        "createplus.status.disabled"));
                
                graphics.drawString(mc.font, text, panelX + 5, y + 6, 0xFFFFFFFF);
            }
            y += itemHeight;
        }

        graphics.disableScissor();

        // 渲染描述面板
        if (selectedIndex >= 0 && selectedIndex < features.size()) {
            CreativePlusFeature selectedFeature = features.get(selectedIndex);
            int descPanelX = panelX + panelWidth + 10;
            int descPanelWidth = 200;
            
            // 渲染描述背景
            renderRoundedRect(graphics, descPanelX, panelY, 
                descPanelX + descPanelWidth, panelY + panelHeight, bgColor);

            // 渲染描述文本
            Component description = Component.translatable(selectedFeature.getDescriptionKey());
            int descY = panelY + 5;
            graphics.drawString(mc.font, description, descPanelX + 5, descY, 0xFFFFFFFF);

            // 如果是SubHUDFeature且启用，渲染子HUD
            if (selectedFeature instanceof SubHUDFeature subHUD && selectedFeature.isEnabled() && subHUD.isSubHUDVisible()) {
                subHUD.renderSubHUD(graphics, descPanelX + descPanelWidth + 10, panelY);
            }
        }
    }

    private void renderRoundedRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        // 主体
        graphics.fill(x1, y1, x2, y2, color);
        
        // 边缘淡化
        int fadeColor = (color & 0xFFFFFF) | ((color >>> 24) / 2 << 24);
        graphics.fill(x1, y1, x1 + 1, y2, fadeColor);
        graphics.fill(x2 - 1, y1, x2, y2, fadeColor);
        graphics.fill(x1, y1, x2, y1 + 1, fadeColor);
        graphics.fill(x1, y2 - 1, x2, y2, fadeColor);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        if (KeyBindings.TOGGLE_HUD.consumeClick()) {
            toggleVisibility();
            return;
        }

        if (!visible) return;

        features = Createplus.FEATURE_MANAGER.getFeatures();
        if (features.isEmpty()) return;

        CreativePlusFeature selectedFeature = features.get(selectedIndex);
        
        if (isInSubHUD && currentSubHUD != null) {
            // 在子HUD中，处理子HUD的按键
            switch (event.getKey()) {
                case GLFW.GLFW_KEY_LEFT -> {
                    isInSubHUD = false;
                    currentSubHUD = null;
                }
                default -> currentSubHUD.handleKeyPress(event.getKey());
            }
        } else {
            // 在主HUD中
            switch (event.getKey()) {
                case GLFW.GLFW_KEY_UP -> {
                    int prevIndex = selectedIndex;
                    selectedIndex = Math.floorMod(selectedIndex - 1, features.size());
                    updateScroll(prevIndex, selectedIndex);
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    int prevIndex = selectedIndex;
                    selectedIndex = Math.floorMod(selectedIndex + 1, features.size());
                    updateScroll(prevIndex, selectedIndex);
                }
                case GLFW.GLFW_KEY_ENTER -> {
                    if (selectedFeature.isEnabled()) {
                        Createplus.FEATURE_MANAGER.onDisable(selectedFeature);
                    } else {
                        Createplus.FEATURE_MANAGER.onEnable(selectedFeature);
                    }
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    if (selectedFeature instanceof SubHUDFeature subHUD && selectedFeature.isEnabled()) {
                        subHUD.toggleSubHUD();
                        if (subHUD.isSubHUDVisible()) {
                            isInSubHUD = true;
                            currentSubHUD = subHUD;
                        }
                    }
                }
            }
        }
    }

    private void updateScroll(int prevIndex, int newIndex) {
        int itemHeight = 20;
        int contentHeight = panelHeight - 25;  // 可视区域高度
        float maxScroll = Math.max(0, features.size() * itemHeight - contentHeight);  // 最大滚动距离

        // 计算目标位置
        float targetY = newIndex * itemHeight;

        // 如果是向上滚动且目标位置在可视区域上方
        if (newIndex < prevIndex && targetY < currentScrollY) {
            targetScrollY = Math.max(0, targetY);
        }
        // 如果是向下滚动且目标位置在可视区域下方
        else if (newIndex > prevIndex && targetY > currentScrollY + contentHeight - itemHeight) {
            targetScrollY = Math.min(maxScroll, targetY - contentHeight + itemHeight);
        }

        // 确保滚动位置不超出边界
        targetScrollY = Math.max(0, Math.min(targetScrollY, maxScroll));
    }

    public static void toggleVisibility() {
        visible = !visible;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void setVisible(boolean value) {
        visible = value;
    }

    public static float getOpacity() {
        return opacity;
    }

    public static void setOpacity(float value) {
        opacity = Math.max(0.1f, Math.min(1.0f, value));
        ConfigManager.saveConfig();
    }

    // 添加辅助方法
    private String getFeatureKey(String name) {
        return name.toLowerCase()
            .replace(' ', '_')
            .replace("(", "")
            .replace(")", "")
            .replace("[", "")
            .replace("]", "");
    }
} 