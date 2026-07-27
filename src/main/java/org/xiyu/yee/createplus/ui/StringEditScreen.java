package org.xiyu.yee.createplus.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.settings.StringSetting;

/** 简单的字符串设置编辑页。 */
public class StringEditScreen extends Screen {
    private final Screen parent;
    private final StringSetting setting;
    private EditBox box;

    public StringEditScreen(Screen parent, StringSetting setting) {
        super(Component.literal(setting.getDisplayName()));
        this.parent = parent;
        this.setting = setting;
    }

    @Override
    protected void init() {
        int w = Math.min(360, this.width - 40);
        int x = (this.width - w) / 2;
        box = new EditBox(this.font, x, this.height / 2 - 10, w, 20, Component.literal(setting.getId()));
        box.setMaxLength(setting.getMaxLength());
        box.setValue(setting.get());
        addRenderableWidget(box);
        setInitialFocus(box);

        addRenderableWidget(Button.builder(Component.literal("完成"), b -> {
            setting.set(box.getValue());
            Createplus.FEATURE_MANAGER.saveConfig();
            onClose();
        }).bounds(this.width / 2 - 100, this.height / 2 + 24, 96, 20).build());

        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
            .bounds(this.width / 2 + 4, this.height / 2 + 24, 96, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 36, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
