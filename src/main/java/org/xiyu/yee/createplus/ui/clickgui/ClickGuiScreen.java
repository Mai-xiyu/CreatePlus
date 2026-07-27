package org.xiyu.yee.createplus.ui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.settings.Setting;
import org.xiyu.yee.createplus.settings.Slidable;
import org.xiyu.yee.createplus.ui.Theme;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 鼠标操作的功能面板（默认右 Shift）。
 *
 * 每个分类一个可拖动面板，左键开关功能，右键展开该功能的设置。
 * 顶部有搜索框，功能一多就靠它定位。
 */
public class ClickGuiScreen extends Screen {

    private static final int PANEL_WIDTH = 132;
    private static final int ROW = 15;

    /** 每帧重建的可点击区域。渲染时算好布局，点击时直接复用，避免两套坐标算法对不上。 */
    private final List<Hit> hits = new ArrayList<>();
    private final List<Panel> panels = new ArrayList<>();

    private Panel draggingPanel;
    private double dragOffsetX;
    private double dragOffsetY;

    private Hit draggingSlider;
    private String search = "";
    private boolean searchFocused;

    public ClickGuiScreen() {
        super(Component.literal("CreatePlus"));
    }

    @Override
    protected void init() {
        if (!panels.isEmpty()) return;

        int x = 12;
        int y = 34;
        for (CreativePlusFeature.Category category : CreativePlusFeature.Category.values()) {
            List<CreativePlusFeature> features = Createplus.FEATURE_MANAGER.getByCategory(category);
            if (features.isEmpty()) continue;
            panels.add(new Panel(category, features, x, y));
            x += PANEL_WIDTH + 8;
            if (x + PANEL_WIDTH > this.width - 12) {
                x = 12;
                y += 190;
            }
        }
    }

    // ==================================================================
    // 渲染
    // ==================================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        hits.clear();

        drawTopBar(graphics, mouseX, mouseY);

        for (Panel panel : panels) {
            panel.render(graphics, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        drawTooltip(graphics, mouseX, mouseY);
    }

    private void drawTopBar(GuiGraphics graphics, int mouseX, int mouseY) {
        RenderUtils.drawRect(graphics, 0, 0, this.width, 26, 0xE6101319);
        RenderUtils.drawRect(graphics, 0, 26, this.width, 1, Theme.ACCENT);
        RenderUtils.drawText(graphics, "CreatePlus", 12, 9, Theme.ACCENT, false);

        int boxX = 92;
        int boxWidth = 150;
        RenderUtils.drawRoundedRect(graphics, boxX, 5, boxWidth, 16, Theme.CORNER,
            searchFocused ? 0xFF20262F : 0xFF181D25);
        RenderUtils.drawBorder(graphics, boxX, 5, boxWidth, 16, 1,
            searchFocused ? Theme.ACCENT : Theme.OFF);
        String shown = search.isEmpty() && !searchFocused ? "搜索功能…" : search;
        RenderUtils.drawClippedText(graphics, shown + (searchFocused ? "_" : ""),
            boxX + 4, 9, boxWidth - 8,
            search.isEmpty() && !searchFocused ? Theme.TEXT_DISABLED : Theme.TEXT, false);
        hits.add(Hit.searchBox(boxX, 5, boxWidth, 16));

        // HUD 透明度已挪到「信息面板」设置；顶栏只保留重置
        int resetW = 62;
        int resetX = this.width - 12 - resetW;
        boolean hovered = mouseX >= resetX && mouseX <= resetX + resetW && mouseY >= 5 && mouseY <= 21;
        RenderUtils.drawRoundedRect(graphics, resetX, 5, resetW, 16, Theme.CORNER,
            hovered ? Theme.DANGER : 0xFF262C36);
        RenderUtils.drawCenteredText(graphics, "全部重置", resetX + resetW / 2, 9, Theme.TEXT);
        hits.add(Hit.button(resetX, 5, resetW, 16, button -> resetAll(), "把所有功能的设置恢复默认值"));
    }

    private void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (hit.tooltip == null || !hit.contains(mouseX, mouseY)) continue;

            int width = Math.min(200, this.font.width(hit.tooltip) + 8);
            int lines = RenderUtils.countWrappedLines(hit.tooltip, width - 8);
            int height = lines * 10 + 6;
            int x = Math.min(mouseX + 10, this.width - width - 4);
            int y = Math.min(mouseY + 8, this.height - height - 4);

            RenderUtils.drawRoundedRect(graphics, x, y, width, height, Theme.CORNER, 0xF01A1F28);
            RenderUtils.drawBorder(graphics, x, y, width, height, 1, Theme.ACCENT_DIM);
            RenderUtils.drawWrappedText(graphics, hit.tooltip, x + 4, y + 3,
                width - 8, 10, lines, Theme.TEXT);
            return;
        }
    }

    // ==================================================================
    // 输入
    // ==================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (!hit.contains(mouseX, mouseY)) continue;

            searchFocused = hit.isSearchBox;
            if (hit.isSearchBox) return true;

            if (hit.panel != null) {
                if (button == 1) {
                    hit.panel.expanded = !hit.panel.expanded;
                } else if (button == 0) {
                    draggingPanel = hit.panel;
                    dragOffsetX = mouseX - hit.panel.x;
                    dragOffsetY = mouseY - hit.panel.y;
                }
                bringToFront(hit.panel);
                return true;
            }

            if (hit.slider != null && button == 0) {
                draggingSlider = hit;
                applySlider(hit, mouseX);
                return true;
            }

            if (hit.action != null) {
                hit.action.accept(button);
                Createplus.FEATURE_MANAGER.saveConfig();
                return true;
            }
        }

        searchFocused = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingPanel != null || draggingSlider != null) {
            draggingPanel = null;
            draggingSlider = null;
            Createplus.FEATURE_MANAGER.saveConfig();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingPanel != null) {
            draggingPanel.x = (int) (mouseX - dragOffsetX);
            draggingPanel.y = (int) (mouseY - dragOffsetY);
            return true;
        }
        if (draggingSlider != null) {
            applySlider(draggingSlider, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (hit.slider == null || !hit.contains(mouseX, mouseY)) continue;
            hit.slider.cycle(delta > 0 ? 1 : -1);
            Createplus.FEATURE_MANAGER.saveConfig();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (searchFocused && character >= ' ') {
            search += character;
            return true;
        }
        return super.charTyped(character, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Createplus.FEATURE_MANAGER.saveConfig();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void applySlider(Hit hit, double mouseX) {
        double fraction = (mouseX - hit.x) / hit.w;
        if (hit.slider instanceof Slidable slidable) {
            slidable.setFraction(fraction);
        }
    }

    private void bringToFront(Panel panel) {
        panels.remove(panel);
        panels.add(panel);
    }

    private void resetAll() {
        for (CreativePlusFeature feature : Createplus.FEATURE_MANAGER.getFeatures()) {
            for (Setting<?> setting : feature.getSettings()) {
                setting.reset();
            }
        }
        Createplus.FEATURE_MANAGER.saveConfig();
    }

    private boolean matchesSearch(CreativePlusFeature feature) {
        if (search.isEmpty()) return true;
        String needle = search.toLowerCase();
        return feature.getName().toLowerCase().contains(needle)
            || feature.getTranslatedName().toLowerCase().contains(needle);
    }

    // ==================================================================
    // 面板
    // ==================================================================

    private final class Panel {
        private final CreativePlusFeature.Category category;
        private final List<CreativePlusFeature> features;
        private final List<CreativePlusFeature> expandedFeatures = new ArrayList<>();
        private int x;
        private int y;
        private boolean expanded = true;

        private Panel(CreativePlusFeature.Category category, List<CreativePlusFeature> features,
                      int x, int y) {
            this.category = category;
            this.features = features;
            this.x = x;
            this.y = y;
        }

        private void render(GuiGraphics graphics, int mouseX, int mouseY) {
            int color = category.getColor();

            RenderUtils.drawRoundedRect(graphics, x, y, PANEL_WIDTH, 18, Theme.CORNER, 0xF01B2130);
            RenderUtils.drawRect(graphics, x, y + 17, PANEL_WIDTH, 1, color);
            RenderUtils.drawText(graphics, category.getDisplayName(), x + 6, y + 5, color, false);
            RenderUtils.drawRightAlignedText(graphics, expanded ? "−" : "+",
                x + PANEL_WIDTH - 6, y + 5, Theme.TEXT_MUTED, false);
            hits.add(Hit.panel(x, y, PANEL_WIDTH, 18, this));

            if (!expanded) return;

            int rowY = y + 18;
            int bodyStart = rowY;
            List<CreativePlusFeature> shown = new ArrayList<>();
            for (CreativePlusFeature feature : features) {
                if (matchesSearch(feature)) shown.add(feature);
            }

            int bodyHeight = 2;
            for (CreativePlusFeature feature : shown) {
                bodyHeight += ROW;
                if (expandedFeatures.contains(feature)) {
                    bodyHeight += feature.getSettings().size() * ROW;
                }
            }
            RenderUtils.drawRect(graphics, x, bodyStart, PANEL_WIDTH, bodyHeight, 0xE6141821);
            rowY += 1;

            for (CreativePlusFeature feature : shown) {
                rowY = drawFeatureRow(graphics, feature, rowY, mouseX, mouseY, color);
            }
        }

        private int drawFeatureRow(GuiGraphics graphics, CreativePlusFeature feature, int rowY,
                                   int mouseX, int mouseY, int color) {
            boolean hovered = mouseX >= x && mouseX <= x + PANEL_WIDTH
                && mouseY >= rowY && mouseY <= rowY + ROW;
            boolean on = feature.isEnabled();

            if (on) {
                RenderUtils.drawRect(graphics, x, rowY, PANEL_WIDTH, ROW,
                    RenderUtils.setAlpha(color, hovered ? 90 : 60));
                RenderUtils.drawRect(graphics, x, rowY, 2, ROW, color);
            } else if (hovered) {
                RenderUtils.drawRect(graphics, x, rowY, PANEL_WIDTH, ROW, Theme.PANEL_ALT);
            }

            RenderUtils.drawClippedText(graphics, feature.getTranslatedName(), x + 6, rowY + 4,
                PANEL_WIDTH - 34, on ? Theme.TEXT : Theme.TEXT_MUTED, false);

            boolean open = expandedFeatures.contains(feature);
            if (feature.hasSettings()) {
                RenderUtils.drawText(graphics, open ? "▾" : "▸", x + PANEL_WIDTH - 22, rowY + 4,
                    Theme.TEXT_MUTED, false);
            }
            RenderUtils.drawToggleDot(graphics, x + PANEL_WIDTH - 13, rowY + 4, 7, on,
                Theme.ON, Theme.OFF);

            hits.add(Hit.button(x, rowY, PANEL_WIDTH, ROW, button -> {
                if (button == 0) {
                    Createplus.FEATURE_MANAGER.toggle(feature);
                } else if (button == 1 && feature.hasSettings()) {
                    if (!expandedFeatures.remove(feature)) expandedFeatures.add(feature);
                }
            }, feature.getDetailedDescription()));

            rowY += ROW;
            if (!open) return rowY;

            for (Setting<?> setting : feature.getSettings()) {
                drawSettingRow(graphics, setting, rowY, mouseX, mouseY, color);
                rowY += ROW;
            }
            return rowY;
        }

        private void drawSettingRow(GuiGraphics graphics, Setting<?> setting, int rowY,
                                    int mouseX, int mouseY, int color) {
            boolean hovered = mouseX >= x && mouseX <= x + PANEL_WIDTH
                && mouseY >= rowY && mouseY <= rowY + ROW;
            if (hovered) {
                RenderUtils.drawRect(graphics, x + 6, rowY, PANEL_WIDTH - 6, ROW, Theme.PANEL_ALT);
            }

            RenderUtils.drawClippedText(graphics, setting.getDisplayName(), x + 12, rowY + 4,
                PANEL_WIDTH - 62, Theme.TEXT_MUTED, false);
            RenderUtils.drawRightAlignedText(graphics, setting.getDisplayValue(),
                x + PANEL_WIDTH - 6, rowY + 4, color, false);

            if (setting instanceof Slidable slidable) {
                int barX = x + 12;
                int barWidth = PANEL_WIDTH - 24;
                RenderUtils.drawRect(graphics, barX, rowY + ROW - 3, barWidth, 2, Theme.OFF);
                RenderUtils.drawRect(graphics, barX, rowY + ROW - 3,
                    (int) Math.round(barWidth * slidable.getFraction()), 2, color);
                hits.add(Hit.slider(barX, rowY, barWidth, ROW, setting, setting.getTooltip()));
            } else {
                hits.add(Hit.button(x + 6, rowY, PANEL_WIDTH - 6, ROW, button -> {
                    if (setting instanceof org.xiyu.yee.createplus.settings.StringSetting stringSetting) {
                        minecraft.setScreen(new org.xiyu.yee.createplus.ui.StringEditScreen(
                            ClickGuiScreen.this, stringSetting));
                    } else {
                        setting.cycle(button == 1 ? -1 : 1);
                    }
                }, setting.getTooltip()));
            }
        }
    }

    // ==================================================================
    // 命中区域
    // ==================================================================

    private static final class Hit {
        private int x;
        private int y;
        private int w;
        private int h;
        private java.util.function.IntConsumer action;
        private Setting<?> slider;
        private Panel panel;
        private String tooltip;
        private boolean isOpacity;
        private boolean isSearchBox;

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        }

        private static Hit button(int x, int y, int w, int h,
                                  java.util.function.IntConsumer action, String tooltip) {
            Hit hit = new Hit();
            hit.x = x;
            hit.y = y;
            hit.w = w;
            hit.h = h;
            hit.action = action;
            hit.tooltip = tooltip;
            return hit;
        }

        private static Hit slider(int x, int y, int w, int h, Setting<?> setting, String tooltip) {
            Hit hit = new Hit();
            hit.x = x;
            hit.y = y;
            hit.w = w;
            hit.h = h;
            hit.slider = setting;
            hit.tooltip = tooltip;
            return hit;
        }

        private static Hit panel(int x, int y, int w, int h, Panel panel) {
            Hit hit = new Hit();
            hit.x = x;
            hit.y = y;
            hit.w = w;
            hit.h = h;
            hit.panel = panel;
            return hit;
        }

        private static Hit opacity(int x, int y, int w, int h) {
            Hit hit = new Hit();
            hit.x = x;
            hit.y = y;
            hit.w = w;
            hit.h = h;
            hit.isOpacity = true;
            return hit;
        }

        private static Hit searchBox(int x, int y, int w, int h) {
            Hit hit = new Hit();
            hit.x = x;
            hit.y = y;
            hit.w = w;
            hit.h = h;
            hit.isSearchBox = true;
            return hit;
        }
    }
}
