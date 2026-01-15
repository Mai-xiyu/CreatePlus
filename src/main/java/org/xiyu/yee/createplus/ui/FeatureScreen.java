package org.xiyu.yee.createplus.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.features.SubHUDFeature;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public class FeatureScreen {
    private static boolean visible = false;
    private static float hudOpacity = 1.0f;

    // --- 布局配置 ---
    private static final int MENU_X = 4;
    private static final int MENU_Y = 4;
    private static final int WIDTH = 120;
    private static final int HEADER_H = 18;
    private static final int LIST_MAX_H = 150; // 列表最大高度，防止太长
    private static final int ITEM_H = 14;
    private static final int DESC_BOX_H = 40;  // 底部描述框高度
    private static final int PADDING = 2;      // 间距

    // 颜色配置
    private static final int COL_HEADER = 0xFF111111;
    private static final int COL_BG = 0x90000000;
    private static final int COL_ACCENT = 0xFF00A2FF; // CreatePlus 蓝
    private static final int COL_TEXT_ON = 0xFFFFFFFF;
    private static final int COL_TEXT_OFF = 0xFFAAAAAA;

    private enum MenuState { MAIN, CATEGORY, SUB_SETTING }
    private MenuState state = MenuState.MAIN;

    private int selectedIndex = 0;
    private String currentCategory = "";

    private final List<String> categories = new ArrayList<>();
    private final List<CreativePlusFeature> currentFeatures = new ArrayList<>();
    private SubHUDFeature activeSubFeature = null;

    public FeatureScreen() {
        categories.add("Building");
        categories.add("Assist");
        categories.add("Display");
        categories.add("System");
    }

    public static float getOpacity() { return hudOpacity; }
    public static void setOpacity(float value) { hudOpacity = Math.max(0.1f, Math.min(1.0f, value)); }
    public static void toggleVisibility() { visible = !visible; }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();
        int currentY = MENU_Y;

        // 1. 标题栏
        RenderUtils.drawRect(graphics, MENU_X, currentY, WIDTH, HEADER_H, COL_HEADER);
        RenderUtils.drawRect(graphics, MENU_X, currentY + HEADER_H - 1, WIDTH, 1, COL_ACCENT); // 蓝条装饰
        graphics.drawString(mc.font, "CreatePlus", MENU_X + 4, currentY + 5, COL_ACCENT, false);

        String subTitle = state == MenuState.MAIN ? "主菜单" : getCategoryNameCN(currentCategory);
        graphics.drawString(mc.font, subTitle, MENU_X + WIDTH - mc.font.width(subTitle) - 4, currentY + 5, 0xFFAAAAAA, false);
        currentY += HEADER_H;

        // 2. 列表内容
        int listSize = getListSize();
        // 简单的滚动视图逻辑（这里简化为只显示前10个或者全部，为了代码简洁先全部显示，实际建议加 scroll）
        int listDrawHeight = listSize * ITEM_H;

        RenderUtils.drawRect(graphics, MENU_X, currentY, WIDTH, listDrawHeight, COL_BG);

        for (int i = 0; i < listSize; i++) {
            boolean isSelected = (i == selectedIndex);
            int itemY = currentY + (i * ITEM_H);

            if (isSelected) {
                RenderUtils.drawRect(graphics, MENU_X, itemY, WIDTH, ITEM_H, COL_ACCENT);
            }

            String name = getItemName(i);
            int textColor = isSelected ? 0xFFFFFFFF : 0xFFAAAAAA;
            if (state == MenuState.CATEGORY) {
                // 功能状态颜色
                boolean enabled = currentFeatures.get(i).isEnabled();
                textColor = enabled ? 0xFFFFFFFF : 0xFFAAAAAA;
            }

            graphics.drawString(mc.font, name, MENU_X + 4, itemY + 3, textColor, false);

            // 右侧指示器
            if (state == MenuState.MAIN) {
                graphics.drawString(mc.font, ">", MENU_X + WIDTH - 10, itemY + 3, textColor, false);
            } else {
                CreativePlusFeature f = currentFeatures.get(i);
                if (f instanceof SubHUDFeature) {
                    graphics.drawString(mc.font, "+", MENU_X + WIDTH - 10, itemY + 3, 0xFFCCCCCC, false);
                } else if (f.isEnabled()) {
                    RenderUtils.drawRect(graphics, MENU_X + WIDTH - 4, itemY + 2, 2, ITEM_H - 4, 0xFF00FF00);
                }
            }
        }
        currentY += listDrawHeight;

        // 3. 介绍框 (Description Box)
        // 留一点间隙
        currentY += PADDING;

        RenderUtils.drawRect(graphics, MENU_X, currentY, WIDTH, DESC_BOX_H, COL_HEADER); // 深色背景
        RenderUtils.drawRect(graphics, MENU_X, currentY, WIDTH, 1, COL_ACCENT); // 顶部分隔线

        // 获取描述文本
        String descText = "";
        if (state == MenuState.MAIN) {
            descText = "选择一个分类以查看功能列表。";
        } else if (!currentFeatures.isEmpty()) {
            descText = currentFeatures.get(selectedIndex).getTranslatedDescription();
        }

        // 简单的文本换行渲染 (支持 \n)
        int textY = currentY + 4;
        String[] lines = descText.split("\n"); // 支持lang文件里的 \n 换行
        for (String line : lines) {
            // 这里也可以加自动换行逻辑 (mc.font.split)
            graphics.drawString(mc.font, line, MENU_X + 4, textY, 0xFFCCCCCC, false);
            textY += 10;
            if (textY > currentY + DESC_BOX_H) break; // 防止溢出
        }

        // 4. 子菜单渲染 (参数调整)
        if (state == MenuState.SUB_SETTING && activeSubFeature != null) {
            int subMenuX = MENU_X + WIDTH + PADDING;
            int subMenuY = MENU_Y + HEADER_H + (selectedIndex * ITEM_H);
            activeSubFeature.renderSubHUD(graphics, subMenuX, subMenuY);
        }
    }

    // ... 按键逻辑保持不变，只需复制上一轮的按键处理部分 ...
    // 为了完整性，这里简写一下核心逻辑
    private int getListSize() { return state == MenuState.MAIN ? categories.size() : currentFeatures.size(); }

    private String getItemName(int index) {
        return state == MenuState.MAIN ? getCategoryNameCN(categories.get(index)) : currentFeatures.get(index).getTranslatedName();
    }

    private String getCategoryNameCN(String cat) {
        return switch (cat) {
            case "Building" -> "建筑功能";
            case "Assist" -> "辅助功能";
            case "Display" -> "显示功能";
            case "System" -> "系统设置";
            default -> cat;
        };
    }

    // 更新列表逻辑 (保持上一轮的分类判断)
    private void updateCurrentFeatures() {
        currentFeatures.clear();
        List<CreativePlusFeature> all = Createplus.FEATURE_MANAGER.getFeatures();
        String cat = currentCategory;
        for (CreativePlusFeature f : all) {
            String id = f.getTranslationKey().toLowerCase();
            boolean match = false;
            if (cat.equals("Building") && (id.contains("area") || id.contains("build") || id.contains("place") || id.contains("scaffold") || id.contains("color") || id.contains("nucker"))) match = true;
            else if (cat.equals("Assist") && (id.contains("fly") || id.contains("freecam") || id.contains("gamma") || id.contains("gibbon") || id.contains("speed") || id.contains("time") || id.contains("zoom") || id.contains("spin"))) match = true;
            else if (cat.equals("Display") && (id.contains("hud") || id.contains("ping"))) match = true;
            else if (cat.equals("System") && (id.contains("performance"))) match = true;
            if (match) currentFeatures.add(f);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_REPEAT) return;
        if (KeyBindings.TOGGLE_HUD.consumeClick()) { toggleVisibility(); return; }
        if (!visible) return;

        if (state == MenuState.SUB_SETTING && activeSubFeature != null) {
            if (activeSubFeature.handleKeyPress(event.getKey())) return;
            if (event.getKey() == GLFW.GLFW_KEY_LEFT || event.getKey() == GLFW.GLFW_KEY_BACKSPACE) {
                state = MenuState.CATEGORY;
                activeSubFeature.toggleSubHUD();
                activeSubFeature = null;
                return;
            }
        }

        switch (event.getKey()) {
            case GLFW.GLFW_KEY_UP -> { selectedIndex--; if(selectedIndex < 0) selectedIndex = getListSize()-1; }
            case GLFW.GLFW_KEY_DOWN -> { selectedIndex++; if(selectedIndex >= getListSize()) selectedIndex = 0; }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_RIGHT -> handleAction();
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_BACKSPACE -> handleBack();
        }
    }

    private void handleAction() {
        if (state == MenuState.MAIN) {
            currentCategory = categories.get(selectedIndex);
            updateCurrentFeatures();
            if (!currentFeatures.isEmpty()) { state = MenuState.CATEGORY; selectedIndex = 0; }
        } else {
            CreativePlusFeature f = currentFeatures.get(selectedIndex);
            if (f instanceof SubHUDFeature) {
                state = MenuState.SUB_SETTING;
                activeSubFeature = (SubHUDFeature) f;
                activeSubFeature.toggleSubHUD();
            } else {
                f.toggle();
            }
        }
    }

    private void handleBack() {
        if (state == MenuState.CATEGORY) { state = MenuState.MAIN; selectedIndex = 0; }
        else if (state == MenuState.MAIN) visible = false;
    }
}