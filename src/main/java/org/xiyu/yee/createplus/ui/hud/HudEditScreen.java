package org.xiyu.yee.createplus.ui.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.HudEditor;
import org.xiyu.yee.createplus.ui.Theme;
import org.xiyu.yee.createplus.utils.HudLayout;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD 布局编辑：拖动面板。Esc 无效，必须 F8 退出。
 */
public class HudEditScreen extends Screen {
    private final List<Panel> panels = new ArrayList<>();
    private Panel dragging;
    private double dragOffX;
    private double dragOffY;
    private boolean exiting;

    public HudEditScreen() {
        super(Component.translatable("createplus.hud_edit.title"));
    }

    @Override
    protected void init() {
        panels.clear();
        panels.add(absPanel(HudLayout.MINI_HUD, 4, 4, 140, 60));
        panels.add(absPanel(HudLayout.BLOCK_INFO, Math.max(4, width - 164), 4, 160, 80));
        panels.add(absPanel(HudLayout.PING, Math.max(4, width - 84), 4, 80, 16));

        panels.add(offsetPanel(HudLayout.SCOREBOARD, Math.max(4, width - 110), height / 3, 100, 80));
        panels.add(offsetPanel(HudLayout.HEALTH, width / 2 - 91, height - 39, 182, 28));
        panels.add(offsetPanel(HudLayout.HOTBAR, width / 2 - 91, height - 22, 182, 22));
        panels.add(offsetPanel(HudLayout.EXPERIENCE, width / 2 - 91, height - 29, 182, 5));
        panels.add(offsetPanel(HudLayout.BOSS_BAR, width / 2 - 91, 12, 182, 20));
        panels.add(offsetPanel(HudLayout.EFFECTS, Math.max(4, width - 28), 18, 24, 64));
    }

    private Panel absPanel(String id, int defX, int defY, int w, int h) {
        HudLayout.Pos pos = HudLayout.get(id);
        int x = (pos != null && pos.custom) ? pos.x : defX;
        int y = (pos != null && pos.custom) ? pos.y : defY;
        return new Panel(id, label(id), x, y, w, h, false, defX, defY);
    }

    private Panel offsetPanel(String id, int baseX, int baseY, int w, int h) {
        HudLayout.Pos pos = HudLayout.get(id);
        int x = baseX + (pos != null && pos.custom ? pos.x : 0);
        int y = baseY + (pos != null && pos.custom ? pos.y : 0);
        return new Panel(id, label(id), x, y, w, h, true, baseX, baseY);
    }

    private static String label(String id) {
        String key = "createplus.hud_edit.panel." + id;
        return I18n.exists(key) ? I18n.get(key) : id;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderUtils.drawRect(graphics, 0, 0, this.width, this.height, 0x66000000);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, Theme.ACCENT);
        graphics.drawCenteredString(this.font,
            Component.translatable("createplus.hud_edit.hint_f8").getString(),
            this.width / 2, 20, Theme.TEXT_MUTED);

        for (Panel p : panels) {
            boolean hover = p.contains(mouseX, mouseY) || p == dragging;
            RenderUtils.drawRoundedRect(graphics, p.x, p.y, p.w, p.h, Theme.CORNER,
                hover ? 0xC01B2130 : 0xA0141821);
            RenderUtils.drawBorder(graphics, p.x, p.y, p.w, p.h, 1,
                hover ? Theme.ACCENT : Theme.ACCENT_DIM);
            graphics.drawString(this.font, p.label, p.x + 4, p.y + 4, Theme.TEXT, false);
            String coord = p.offsetStyle
                ? ("Δ" + (p.x - p.baseX) + "," + (p.y - p.baseY))
                : (p.x + ", " + p.y);
            graphics.drawString(this.font, coord, p.x + 4, p.y + p.h - 12, Theme.TEXT_MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = panels.size() - 1; i >= 0; i--) {
                Panel p = panels.get(i);
                if (!p.contains(mouseX, mouseY)) continue;
                dragging = p;
                dragOffX = mouseX - p.x;
                dragOffY = mouseY - p.y;
                panels.remove(i);
                panels.add(p);
                return true;
            }
        }
        if (button == 1) {
            for (int i = panels.size() - 1; i >= 0; i--) {
                Panel p = panels.get(i);
                if (!p.contains(mouseX, mouseY)) continue;
                HudLayout.clear(p.id);
                p.x = p.baseX;
                p.y = p.baseY;
                Createplus.FEATURE_MANAGER.saveConfig();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            commit(dragging);
            dragging = null;
            Createplus.FEATURE_MANAGER.saveConfig();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging != null) {
            dragging.x = (int) Math.max(0, Math.min(this.width - dragging.w, mouseX - dragOffX));
            dragging.y = (int) Math.max(0, Math.min(this.height - dragging.h, mouseY - dragOffY));
            return true;
        }
        return true;
    }

    private void commit(Panel p) {
        if (p.offsetStyle) {
            HudLayout.set(p.id, p.x - p.baseX, p.y - p.baseY);
        } else {
            HudLayout.set(p.id, p.x, p.y);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F8 || KeyBindings.HUD_EDIT.matches(keyCode, scanCode)) {
            exitEdit();
            return true;
        }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        if (!exiting) return;
        super.onClose();
    }

    private void exitEdit() {
        if (exiting) return;
        exiting = true;
        for (Panel p : panels) commit(p);
        Createplus.FEATURE_MANAGER.saveConfig();
        if (this.minecraft != null) this.minecraft.setScreen(null);
        HudEditor editor = Createplus.FEATURE_MANAGER.get(HudEditor.class);
        if (editor != null && editor.isEnabled()) {
            editor.setEnabled(false);
            Createplus.FEATURE_MANAGER.saveConfig();
        }
    }

    public void exitByFeatureDisable() {
        if (exiting) return;
        exiting = true;
        for (Panel p : panels) commit(p);
        Createplus.FEATURE_MANAGER.saveConfig();
        if (this.minecraft != null) this.minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Panel {
        private final String id;
        private final String label;
        private int x;
        private int y;
        private final int w;
        private final int h;
        private final boolean offsetStyle;
        private final int baseX;
        private final int baseY;

        private Panel(String id, String label, int x, int y, int w, int h,
                      boolean offsetStyle, int baseX, int baseY) {
            this.id = id;
            this.label = label;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.offsetStyle = offsetStyle;
            this.baseX = baseX;
            this.baseY = baseY;
        }

        private boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }
}
