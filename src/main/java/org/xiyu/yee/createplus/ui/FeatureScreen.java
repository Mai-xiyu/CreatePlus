package org.xiyu.yee.createplus.ui;

import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.settings.Setting;
import org.xiyu.yee.createplus.settings.Slidable;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 屏幕角落的快捷菜单（默认 F9）。
 *
 * 三层结构：分类 → 功能 → 设置，全部用方向键操作，不需要打开鼠标界面，
 * 因此可以边飞边调参数。
 */
public class FeatureScreen {
    private static final FeatureScreen INSTANCE = new FeatureScreen();

    private static final int X = 4;
    private static final int Y = 4;
    private static final int WIDTH = 152;
    private static final int MAX_ROWS = 10;
    private static final int DESC_MAX_LINES = 4;
    /** 高亮条收敛速度（越大越跟手），按真实秒做指数插值，跟帧率脱钩。 */
    private static final float HIGHLIGHT_SPEED = 18.0F;

    private static float opacity = 0.95F;

    private enum Level { CATEGORIES, FEATURES, SETTINGS }

    private boolean visible;
    private Level level = Level.CATEGORIES;
    private CreativePlusFeature.Category category = CreativePlusFeature.Category.BUILDING;
    private CreativePlusFeature feature;

    private int selected;
    private int scroll;
    /** 高亮条的当前 Y 偏移，在 render 里按帧插值，避免 20 TPS 卡顿感。 */
    private float highlight;
    private float highlightTarget;
    private long lastFrameNanos;

    /** 说明文字按 tick 刷新，别每帧拼 getDetailedDescription。 */
    private boolean descDirty = true;
    private String cachedDesc = "";

    public static FeatureScreen get() {
        return INSTANCE;
    }

    public static float getOpacity() {
        return opacity;
    }

    public static void setOpacity(float value) {
        opacity = Math.max(0.2F, Math.min(1.0F, value));
    }

    public boolean isVisible() {
        return visible;
    }

    public void toggleVisibility() {
        visible = !visible;
        if (visible) {
            level = Level.CATEGORIES;
            selected = 0;
            scroll = 0;
            highlight = 0.0F;
            highlightTarget = 0.0F;
            lastFrameNanos = 0L;
            descDirty = true;
        }
    }

    public void close() {
        visible = false;
    }

    // ==================================================================
    // 数据
    // ==================================================================

    private List<CreativePlusFeature.Category> categories() {
        List<CreativePlusFeature.Category> result = new ArrayList<>();
        for (CreativePlusFeature.Category value : CreativePlusFeature.Category.values()) {
            if (!Createplus.FEATURE_MANAGER.getByCategory(value).isEmpty()) result.add(value);
        }
        return result;
    }

    private List<CreativePlusFeature> features() {
        return Createplus.FEATURE_MANAGER.getByCategory(category);
    }

    private List<Setting<?>> settings() {
        return feature == null ? List.of() : feature.getSettings();
    }

    private int rowCount() {
        return switch (level) {
            case CATEGORIES -> categories().size();
            case FEATURES -> features().size();
            case SETTINGS -> settings().size();
        };
    }

    private int accentColor() {
        return level == Level.CATEGORIES ? Theme.ACCENT : category.getColor();
    }

    // ==================================================================
    // 输入
    // ==================================================================

    public void handleKeyPress(int key) {
        if (!visible) return;

        int count = rowCount();
        switch (key) {
            case GLFW.GLFW_KEY_UP -> move(-1, count);
            case GLFW.GLFW_KEY_DOWN -> move(1, count);
            case GLFW.GLFW_KEY_PAGE_UP -> move(-MAX_ROWS, count);
            case GLFW.GLFW_KEY_PAGE_DOWN -> move(MAX_ROWS, count);
            case GLFW.GLFW_KEY_HOME -> select(0, count);
            case GLFW.GLFW_KEY_END -> select(count - 1, count);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> activate();
            case GLFW.GLFW_KEY_RIGHT -> {
                switch (level) {
                    case SETTINGS -> adjust(1);
                    case FEATURES -> {
                        List<CreativePlusFeature> list = features();
                        if (!list.isEmpty()) openSettings(list.get(Math.min(selected, list.size() - 1)));
                    }
                    case CATEGORIES -> activate();
                }
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (level == Level.SETTINGS) {
                    adjust(-1);
                } else {
                    back();
                }
            }
            case GLFW.GLFW_KEY_BACKSPACE -> back();
            case GLFW.GLFW_KEY_R -> resetSelected();
            default -> {
            }
        }
    }

    /** @return true 表示滚轮被菜单消费 */
    public boolean handleScroll(double delta) {
        if (!visible) return false;
        move(delta > 0 ? -1 : 1, rowCount());
        return true;
    }

    private void move(int step, int count) {
        if (count <= 0) return;
        int next = selected + step;
        if (next < 0) next = step == -1 ? count - 1 : 0;
        if (next >= count) next = step == 1 ? 0 : count - 1;
        select(next, count);
    }

    private void select(int index, int count) {
        if (count <= 0) return;
        selected = Math.max(0, Math.min(count - 1, index));
        int visibleRows = Math.min(MAX_ROWS, count);
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + visibleRows) scroll = selected - visibleRows + 1;
        scroll = Math.max(0, Math.min(Math.max(0, count - visibleRows), scroll));
        highlightTarget = (selected - scroll) * Theme.ROW_HEIGHT;
        descDirty = true;
    }

    private void activate() {
        switch (level) {
            case CATEGORIES -> {
                List<CreativePlusFeature.Category> list = categories();
                if (list.isEmpty()) return;
                category = list.get(Math.min(selected, list.size() - 1));
                level = Level.FEATURES;
                selected = 0;
                scroll = 0;
                highlight = 0.0F;
                highlightTarget = 0.0F;
                descDirty = true;
            }
            case FEATURES -> {
                List<CreativePlusFeature> list = features();
                if (list.isEmpty()) return;
                Createplus.FEATURE_MANAGER.toggle(list.get(selected));
                descDirty = true;
            }
            case SETTINGS -> adjust(1);
        }
    }

    private void adjust(int direction) {
        List<Setting<?>> list = settings();
        if (list.isEmpty()) return;
        list.get(Math.min(selected, list.size() - 1)).cycle(direction);
        Createplus.FEATURE_MANAGER.saveConfig();
        descDirty = true;
    }

    private void resetSelected() {
        if (level == Level.SETTINGS) {
            List<Setting<?>> list = settings();
            if (list.isEmpty()) return;
            list.get(Math.min(selected, list.size() - 1)).reset();
        } else if (level == Level.FEATURES) {
            List<CreativePlusFeature> list = features();
            if (list.isEmpty()) return;
            for (Setting<?> setting : list.get(selected).getSettings()) {
                setting.reset();
            }
        } else {
            return;
        }
        Createplus.FEATURE_MANAGER.saveConfig();
        descDirty = true;
    }

    private void back() {
        switch (level) {
            case SETTINGS -> {
                level = Level.FEATURES;
                selected = Math.max(0, features().indexOf(feature));
                scroll = 0;
                feature = null;
                highlight = 0.0F;
                highlightTarget = selected * Theme.ROW_HEIGHT;
                descDirty = true;
            }
            case FEATURES -> {
                level = Level.CATEGORIES;
                selected = Math.max(0, categories().indexOf(category));
                scroll = 0;
                highlight = 0.0F;
                highlightTarget = selected * Theme.ROW_HEIGHT;
                descDirty = true;
            }
            case CATEGORIES -> visible = false;
        }
    }

    /** 在功能层按右键进入设置。由 {@link #handleKeyPress} 的 RIGHT 分支间接调用。 */
    private void openSettings(CreativePlusFeature target) {
        if (!target.hasSettings()) return;
        feature = target;
        level = Level.SETTINGS;
        selected = 0;
        scroll = 0;
        highlight = 0.0F;
        highlightTarget = 0.0F;
        descDirty = true;
    }

    public void tick() {
        // 说明文字跟着功能状态变（开关、预览方块数），按 tick 刷新就够
        if (visible) descDirty = true;
    }

    /** 高亮条按真实时间插值，120 帧和 30 帧手感一致。 */
    private void animateHighlight() {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0.0F : (now - lastFrameNanos) / 1_000_000_000.0F;
        lastFrameNanos = now;
        // 切后台或卡顿时别一次飞过头
        dt = Math.min(dt, 0.05F);

        highlightTarget = (selected - scroll) * Theme.ROW_HEIGHT;
        if (dt <= 0.0F) return;

        float t = 1.0F - (float) Math.exp(-HIGHLIGHT_SPEED * dt);
        highlight += (highlightTarget - highlight) * t;
        if (Math.abs(highlightTarget - highlight) < 0.15F) highlight = highlightTarget;
    }

    // ==================================================================
    // 渲染
    // ==================================================================

    public void render(GuiGraphics graphics, int screenWidth, int screenHeight, float partialTick) {
        if (!visible || Createplus.FEATURE_MANAGER == null) return;

        animateHighlight();

        int accent = accentColor();
        int count = rowCount();
        int visibleRows = Math.max(1, Math.min(MAX_ROWS, count));
        int listHeight = visibleRows * Theme.ROW_HEIGHT + 2;

        int y = Y;
        drawHeader(graphics, y, accent);
        y += Theme.HEADER_HEIGHT;

        RenderUtils.drawRect(graphics, X, y, WIDTH, listHeight, alpha(Theme.PANEL));
        drawRows(graphics, y + 1, visibleRows, count, accent);
        drawScrollbar(graphics, y + 1, listHeight - 2, visibleRows, count, accent);
        y += listHeight;

        drawDescription(graphics, y + 2, accent);
    }

    private void drawHeader(GuiGraphics graphics, int y, int accent) {
        RenderUtils.drawRoundedRect(graphics, X, y, WIDTH, Theme.HEADER_HEIGHT,
            Theme.CORNER, alpha(Theme.PANEL_HEADER));
        RenderUtils.drawRect(graphics, X, y + Theme.HEADER_HEIGHT - 1, WIDTH, 1, alpha(accent));

        RenderUtils.drawText(graphics, "CreatePlus", X + 5, y + 6, alpha(accent), false);

        String crumb = switch (level) {
            case CATEGORIES -> "分类";
            case FEATURES -> category.getDisplayName();
            case SETTINGS -> feature == null ? "" : feature.getTranslatedName();
        };
        RenderUtils.drawClippedText(graphics, crumb, X + 62, y + 6, WIDTH - 68,
            alpha(Theme.TEXT_MUTED), false);
    }

    private void drawRows(GuiGraphics graphics, int top, int visibleRows, int count, int accent) {
        if (count == 0) {
            RenderUtils.drawText(graphics, "（没有可用项目）", X + 6, top + 3,
                alpha(Theme.TEXT_DISABLED), false);
            return;
        }

        RenderUtils.beginClip(graphics, X, top, WIDTH, visibleRows * Theme.ROW_HEIGHT);

        // 先画高亮条，再画文字，这样选中行的文字不会被盖住
        int highlightY = top + Math.round(highlight);
        RenderUtils.drawRect(graphics, X, highlightY, WIDTH, Theme.ROW_HEIGHT,
            RenderUtils.setAlpha(accent, (int) (110 * opacity)));
        RenderUtils.drawRect(graphics, X, highlightY, 2, Theme.ROW_HEIGHT, alpha(accent));

        for (int i = 0; i < visibleRows; i++) {
            int index = scroll + i;
            if (index >= count) break;
            drawRow(graphics, index, top + i * Theme.ROW_HEIGHT, index == selected, accent);
        }

        RenderUtils.endClip(graphics);
    }

    private void drawRow(GuiGraphics graphics, int index, int rowY, boolean isSelected, int accent) {
        int textY = rowY + 3;
        int textX = X + 6;

        switch (level) {
            case CATEGORIES -> {
                CreativePlusFeature.Category cat = categories().get(index);
                int enabled = 0;
                List<CreativePlusFeature> list = Createplus.FEATURE_MANAGER.getByCategory(cat);
                for (CreativePlusFeature f : list) {
                    if (f.isEnabled()) enabled++;
                }
                RenderUtils.drawRect(graphics, X + 3, rowY + 4, 2, 6, alpha(cat.getColor()));
                RenderUtils.drawClippedText(graphics, cat.getDisplayName(), textX + 2, textY,
                    WIDTH - 50, alpha(isSelected ? Theme.TEXT : Theme.TEXT_MUTED), false);
                RenderUtils.drawRightAlignedText(graphics, enabled + "/" + list.size(),
                    X + WIDTH - 14, textY, alpha(enabled > 0 ? Theme.ON : Theme.TEXT_DISABLED), false);
                RenderUtils.drawText(graphics, ">", X + WIDTH - 9, textY, alpha(Theme.TEXT_MUTED), false);
            }
            case FEATURES -> {
                CreativePlusFeature f = features().get(index);
                boolean on = f.isEnabled();
                RenderUtils.drawClippedText(graphics, f.getTranslatedName(), textX, textY,
                    WIDTH - 40, alpha(on ? Theme.TEXT : Theme.TEXT_MUTED), false);
                if (f.hasSettings()) {
                    RenderUtils.drawText(graphics, "»", X + WIDTH - 22, textY,
                        alpha(isSelected ? Theme.TEXT : Theme.TEXT_DISABLED), false);
                }
                RenderUtils.drawToggleDot(graphics, X + WIDTH - 13, rowY + 4, 6, on,
                    alpha(Theme.ON), alpha(Theme.OFF));
            }
            case SETTINGS -> {
                Setting<?> setting = settings().get(index);
                RenderUtils.drawClippedText(graphics, setting.getDisplayName(), textX, textY,
                    WIDTH - 70, alpha(isSelected ? Theme.TEXT : Theme.TEXT_MUTED), false);
                RenderUtils.drawRightAlignedText(graphics, setting.getDisplayValue(),
                    X + WIDTH - 6, textY, alpha(isSelected ? accent : Theme.TEXT_MUTED), false);

                if (setting instanceof Slidable slidable) {
                    RenderUtils.drawRect(graphics, X + 6, rowY + Theme.ROW_HEIGHT - 2,
                        WIDTH - 12, 1, alpha(Theme.OFF));
                    int filled = (int) Math.round((WIDTH - 12) * slidable.getFraction());
                    RenderUtils.drawRect(graphics, X + 6, rowY + Theme.ROW_HEIGHT - 2,
                        filled, 1, alpha(accent));
                }
            }
        }
    }

    private void drawScrollbar(GuiGraphics graphics, int top, int height,
                               int visibleRows, int count, int accent) {
        if (count <= visibleRows) return;

        int trackX = X + WIDTH - 2;
        RenderUtils.drawRect(graphics, trackX, top, 2, height, alpha(Theme.SCROLLBAR));

        int thumbHeight = Math.max(6, height * visibleRows / count);
        int maxScroll = count - visibleRows;
        int thumbY = top + (height - thumbHeight) * scroll / Math.max(1, maxScroll);
        RenderUtils.drawRect(graphics, trackX, thumbY, 2, thumbHeight, alpha(accent));
    }

    private void drawDescription(GuiGraphics graphics, int top, int accent) {
        String text = describeSelection();
        String hint = hintLine();

        int lines = Math.min(DESC_MAX_LINES, RenderUtils.countWrappedLines(text, WIDTH - 10));
        int boxHeight = lines * 9 + 14;

        RenderUtils.drawRoundedRect(graphics, X, top, WIDTH, boxHeight, Theme.CORNER,
            alpha(Theme.PANEL_HEADER));
        RenderUtils.drawRect(graphics, X, top, WIDTH, 1, alpha(accent));

        RenderUtils.drawWrappedText(graphics, text, X + 5, top + 4,
            WIDTH - 10, 9, DESC_MAX_LINES, alpha(Theme.TEXT_MUTED));
        RenderUtils.drawText(graphics, hint, X + 5, top + boxHeight - 9,
            alpha(Theme.TEXT_DISABLED), false);
    }

    private String describeSelection() {
        if (!descDirty) return cachedDesc;
        descDirty = false;

        int count = rowCount();
        if (count == 0) {
            cachedDesc = "这个分类下暂时没有功能。";
            return cachedDesc;
        }

        cachedDesc = switch (level) {
            case CATEGORIES -> "共 " + Createplus.FEATURE_MANAGER.getByCategory(
                categories().get(Math.min(selected, count - 1))).size() + " 个功能，回车进入。";
            case FEATURES -> features().get(Math.min(selected, count - 1)).getDetailedDescription();
            case SETTINGS -> {
                Setting<?> setting = settings().get(Math.min(selected, count - 1));
                String tooltip = setting.getTooltip();
                yield tooltip != null ? tooltip
                    : setting.getDisplayName() + "：当前 " + setting.getDisplayValue();
            }
        };
        return cachedDesc;
    }

    private String hintLine() {
        return switch (level) {
            case CATEGORIES -> "↑↓ 选择   → 进入   ⌫ 关闭";
            case FEATURES -> "⏎ 开关   → 设置   R 重置   ⌫ 返回";
            case SETTINGS -> "←→ 调整   R 重置   ⌫ 返回";
        };
    }

    private static int alpha(int color) {
        return RenderUtils.withAlpha(color, opacity);
    }
}
