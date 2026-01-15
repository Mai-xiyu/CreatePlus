package org.xiyu.yee.createplus.ui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.features.SubHUDFeature;
import org.xiyu.yee.createplus.ui.FeatureScreen;
import org.xiyu.yee.createplus.utils.ConfigManager;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {

    private final List<Panel> panels = new ArrayList<>();

    // 当前正在调整设置的功能（右键触发）
    private SubHUDFeature activeSettingFeature = null;
    private int settingX, settingY;

    public ClickGuiScreen() {
        super(Component.literal("ClickGUI"));
        initPanels();
    }

    private void initPanels() {
        double x = 20;
        double y = 20;
        double width = 110;

        // 创建分类面板 (中文标题)
        Panel buildPanel = new Panel("建筑功能", x, y, width);
        Panel assistPanel = new Panel("辅助功能", x + 120, y, width);
        Panel displayPanel = new Panel("显示功能", x + 240, y, width);
        Panel systemPanel = new Panel("系统设置", x + 360, y, width);

        // 填充功能 (使用同样的分类逻辑)
        for (CreativePlusFeature f : Createplus.FEATURE_MANAGER.getFeatures()) {
            String id = f.getTranslationKey().toLowerCase();
            if (id.contains("area") || id.contains("build") || id.contains("place") || id.contains("scaffold") || id.contains("color") || id.contains("nucker")) {
                buildPanel.addButton(new ModuleButton(f, buildPanel));
            } else if (id.contains("fly") || id.contains("freecam") || id.contains("gamma") || id.contains("gibbon") || id.contains("speed") || id.contains("time") || id.contains("zoom") || id.contains("spin")) {
                assistPanel.addButton(new ModuleButton(f, assistPanel));
            } else if (id.contains("hud") || id.contains("ping")) {
                displayPanel.addButton(new ModuleButton(f, displayPanel));
            } else {
                // 其他归为系统
                systemPanel.addButton(new ModuleButton(f, systemPanel));
            }
        }

        panels.add(buildPanel);
        panels.add(assistPanel);
        panels.add(displayPanel);
        panels.add(systemPanel);

        // 设置面板 (透明度)
        Panel settingsPanel = new Panel("全局设置", x + 480, y, width);
        settingsPanel.addButton(new OpacitySlider(settingsPanel));
        panels.add(settingsPanel);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        // 渲染面板
        for (Panel panel : panels) {
            panel.render(graphics, mouseX, mouseY, partialTicks);
        }

        // 渲染 Tooltip (描述)
        for (Panel panel : panels) {
            if (panel.isExpanded) {
                for (ModuleButton btn : panel.buttons) {
                    if (btn.isHovered(mouseX, mouseY) && btn.feature != null) {
                        String desc = btn.feature.getTranslatedDescription();
                        if (desc != null && !desc.isEmpty()) {
                            // 在鼠标旁边渲染描述框
                            int boxW = font.width(desc) + 10;
                            int boxH = 16;
                            RenderUtils.drawRect(graphics, mouseX + 10, mouseY, boxW, boxH, 0xCC000000);
                            graphics.drawString(font, desc, mouseX + 15, mouseY + 4, 0xFFFFFFFF, false);
                        }
                    }
                }
            }
        }

        // 渲染子功能设置窗口 (如果激活)
        if (activeSettingFeature != null) {
            activeSettingFeature.renderSubHUD(graphics, settingX, settingY);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 如果点击了设置窗口区域之外，关闭设置
        if (activeSettingFeature != null) {
            // 这里应该判断点击区域，简化起见，点击空白处关闭
            // activeSettingFeature = null;
            // 暂时不关，让 Panel 逻辑去处理
        }

        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseClicked(mouseX, mouseY, button)) {
                Panel p = panels.remove(i);
                panels.add(p);
                return true;
            }
        }

        // 点击空白处关闭子设置
        if (button == 0) {
            activeSettingFeature = null;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Panel panel : panels) panel.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        for (Panel panel : panels) {
            if (panel.isDragging) {
                panel.x += dragX;
                panel.y += dragY;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // === 内部类 ===

    public class Panel {
        public String categoryName;
        public double x, y, width;
        public boolean isExpanded = true;
        public boolean isDragging = false;
        public List<ModuleButton> buttons = new ArrayList<>();

        public Panel(String name, double x, double y, double w) {
            this.categoryName = name;
            this.x = x; this.y = y; this.width = w;
        }

        public void addButton(ModuleButton btn) { buttons.add(btn); }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            int dx = (int)x; int dy = (int)y; int dw = (int)width;
            RenderUtils.drawRect(graphics, dx, dy, dw, 20, 0xFF00A2FF);
            RenderUtils.drawCenteredText(graphics, categoryName, dx + dw/2, dy + 6, 0xFFFFFFFF);

            if (isExpanded) {
                int h = buttons.size() * 18;
                RenderUtils.drawRect(graphics, dx, dy + 20, dw, h + 2, 0xCC000000);
                int by = dy + 20;
                for (ModuleButton btn : buttons) {
                    btn.render(graphics, dx, by, dw, 18, mouseX, mouseY);
                    by += 18;
                }
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20) {
                if (button == 1) isExpanded = !isExpanded;
                else if (button == 0) isDragging = true;
                return true;
            }
            if (isExpanded) {
                double by = y + 20;
                for (ModuleButton btn : buttons) {
                    if (mouseX >= x && mouseX <= x + width && mouseY >= by && mouseY <= by + 18) {
                        btn.mouseClicked(button, mouseX, mouseY);
                        return true;
                    }
                    by += 18;
                }
            }
            return false;
        }

        public void mouseReleased(double mouseX, double mouseY, int button) {
            isDragging = false;
            if (isExpanded) for (ModuleButton btn : buttons) btn.mouseReleased();
        }
    }

    public class ModuleButton {
        protected CreativePlusFeature feature;
        protected Panel parent;

        public ModuleButton(CreativePlusFeature feature, Panel parent) {
            this.feature = feature; this.parent = parent;
        }

        public boolean isHovered(int mouseX, int mouseY) {
            // 需要在 render 时更新坐标或者在这里重新计算，简化起见假设 render 每帧都调
            return false; // 实际 Tooltip 逻辑在 Panel 循环里写了
        }

        public void render(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
            if (feature == null) return;
            boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

            int color = feature.isEnabled() ? 0xFF00A2FF : 0x00000000;
            if (hovered) color = feature.isEnabled() ? 0xFF33B5FF : 0x33FFFFFF;

            RenderUtils.drawRect(graphics, x + 1, y, w - 2, h, color);

            // 使用中文名
            String name = feature.getTranslatedName();
            int textColor = feature.isEnabled() ? 0xFFFFFFFF : 0xFFAAAAAA;
            RenderUtils.drawText(graphics, name, x + 5, y + 5, textColor, false);

            if (feature instanceof SubHUDFeature) {
                RenderUtils.drawText(graphics, "+", x + w - 10, y + 5, 0xFFAAAAAA, false);
            }
        }

        public boolean isHovered(double mouseX, double mouseY, int x, int y, int w, int h) {
            return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        }

        public void mouseClicked(int button, double mouseX, double mouseY) {
            if (feature == null) return;
            if (button == 0) {
                feature.toggle();
            } else if (button == 1) {
                // 右键点击，如果支持子设置，则激活
                if (feature instanceof SubHUDFeature) {
                    activeSettingFeature = (SubHUDFeature) feature;
                    // 设置面板显示在鼠标位置
                    settingX = (int)mouseX + 10;
                    settingY = (int)mouseY;
                }
            }
        }
        public void mouseReleased() {}
    }

    public class OpacitySlider extends ModuleButton {
        private boolean dragging = false;
        public OpacitySlider(Panel parent) { super(null, parent); }
        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
            if (dragging) {
                float val = (float)(mouseX - x) / w;
                FeatureScreen.setOpacity(val);
            }
            RenderUtils.drawRect(graphics, x + 1, y, w - 2, h, 0x88000000);
            int barWidth = (int) ((w - 2) * FeatureScreen.getOpacity());
            RenderUtils.drawRect(graphics, x + 1, y, barWidth, h, 0xFF00A2FF);
            String text = String.format("透明度: %.0f%%", FeatureScreen.getOpacity() * 100);
            RenderUtils.drawText(graphics, text, x + 5, y + 5, 0xFFFFFFFF, true);
        }
        @Override
        public void mouseClicked(int button, double mouseX, double mouseY) {
            if (button == 0) dragging = true;
        }
        @Override
        public void mouseReleased() {
            if (dragging) { dragging = false; ConfigManager.saveConfig(); }
        }
    }
}