package org.xiyu.yee.createplus.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.settings.Setting;
import org.xiyu.yee.createplus.settings.Slidable;
import org.xiyu.yee.createplus.settings.StringSetting;
import org.xiyu.yee.createplus.ui.StringEditScreen;

import java.util.List;

/**
 * 用原版控件展示某个功能的全部设置。
 *
 * 从"选项"界面进来的入口用这个，键鼠操作和原版一致；
 * 游戏里快速调参则用 ClickGUI 或 F9 菜单。
 *
 * 设置多的功能（信息面板有二十多项）会自动分页，
 * 否则控件会一路排到屏幕外面去。
 */
public class FeatureSettingsScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_TOP = 70;
    private static final int FOOTER_HEIGHT = 40;
    private static final int PANEL_WIDTH = 300;

    private final Screen parent;
    private final CreativePlusFeature feature;

    private int page;

    public FeatureSettingsScreen(Screen parent, CreativePlusFeature feature) {
        super(Component.literal(feature.getTranslatedName()));
        this.parent = parent;
        this.feature = feature;
    }

    private int rowsPerPage() {
        int usable = this.height - LIST_TOP - FOOTER_HEIGHT;
        return Math.max(1, usable / ROW_HEIGHT);
    }

    private int pageCount() {
        int total = feature.getSettings().size();
        return Math.max(1, (total + rowsPerPage() - 1) / rowsPerPage());
    }

    @Override
    protected void init() {
        page = Math.min(page, pageCount() - 1);

        int x = this.width / 2 - PANEL_WIDTH / 2;

        addRenderableWidget(Button.builder(stateLabel(), button -> {
            Createplus.FEATURE_MANAGER.toggle(feature);
            button.setMessage(stateLabel());
        }).bounds(x, 42, PANEL_WIDTH, 20).build());

        List<Setting<?>> settings = feature.getSettings();
        int rows = rowsPerPage();
        int from = page * rows;
        int to = Math.min(settings.size(), from + rows);
        for (int i = from; i < to; i++) {
            addSettingWidget(settings.get(i), x, LIST_TOP + (i - from) * ROW_HEIGHT);
        }

        if (pageCount() > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), button -> turnPage(-1))
                .bounds(x - 24, LIST_TOP, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), button -> turnPage(1))
                .bounds(x + PANEL_WIDTH + 4, LIST_TOP, 20, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("恢复默认"), button -> {
            for (Setting<?> setting : feature.getSettings()) {
                setting.reset();
            }
            Createplus.FEATURE_MANAGER.saveConfig();
            rebuildWidgets();
        }).bounds(this.width / 2 - 152, this.height - 28, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("完成"), button -> onClose())
            .bounds(this.width / 2 + 2, this.height - 28, 150, 20).build());
    }

    private void turnPage(int direction) {
        int count = pageCount();
        page = Math.floorMod(page + direction, count);
        rebuildWidgets();
    }

    private Component stateLabel() {
        return Component.literal(feature.isEnabled() ? "功能状态: 已开启" : "功能状态: 已关闭");
    }

    private void addSettingWidget(Setting<?> setting, int x, int y) {
        AbstractWidget widget;
        if (setting instanceof org.xiyu.yee.createplus.settings.StringSetting stringSetting) {
            widget = Button.builder(label(setting), button ->
                this.minecraft.setScreen(new StringEditScreen(this, stringSetting))
            ).bounds(x, y, PANEL_WIDTH, 20).build();
        } else if (setting instanceof Slidable slidable) {
            widget = new SettingSlider(x, y, PANEL_WIDTH, setting, slidable);
        } else {
            widget = Button.builder(label(setting), button -> {
                setting.cycle(1);
                button.setMessage(label(setting));
                Createplus.FEATURE_MANAGER.saveConfig();
            }).bounds(x, y, PANEL_WIDTH, 20).build();
        }

        String tooltip = setting.getTooltip();
        if (tooltip != null) {
            widget.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        addRenderableWidget(widget);
    }

    private static Component label(Setting<?> setting) {
        return Component.literal(setting.getDisplayName() + ": " + setting.getDisplayValue());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (pageCount() > 1 && delta != 0.0D) {
            turnPage(delta > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        graphics.drawCenteredString(this.font, feature.getTranslatedDescription(),
            this.width / 2, 26, 0xFF9AA4B2);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (pageCount() > 1) {
            graphics.drawCenteredString(this.font,
                (page + 1) + " / " + pageCount(),
                this.width / 2, this.height - 40, 0xFF9AA4B2);
        }
    }

    @Override
    public void onClose() {
        Createplus.FEATURE_MANAGER.saveConfig();
        this.minecraft.setScreen(parent);
    }

    /** 把 Setting 的 0~1 归一化值接到原版滑条上。 */
    private static final class SettingSlider extends AbstractSliderButton {
        private final Setting<?> setting;
        private final Slidable slidable;

        private SettingSlider(int x, int y, int width, Setting<?> setting, Slidable slidable) {
            super(x, y, width, 20, Component.empty(), slidable.getFraction());
            this.setting = setting;
            this.slidable = slidable;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(label(setting));
        }

        @Override
        protected void applyValue() {
            slidable.setFraction(this.value);
            Createplus.FEATURE_MANAGER.saveConfig();
        }
    }
}
