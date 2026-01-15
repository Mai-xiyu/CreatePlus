package org.xiyu.yee.createplus.ui;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.CycleButton;
import org.xiyu.yee.createplus.features.Performance;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;


public class PerformanceSettingsScreen extends Screen {
    private final Screen lastScreen;
    private final Performance performance;
    private final List<CycleButton<Boolean>> buttons = new ArrayList<>();

    public PerformanceSettingsScreen(Screen lastScreen, Performance performance) {
        super(Component.translatable("性能优化设置"));
        this.lastScreen = lastScreen;
        this.performance = performance;
    }

    @Override
    protected void init() {
        int y = 32;
        int spacing = 25;

        addBooleanOption("disableBlockBreakingParticles", 
            "关闭方块破坏粒子",
            "破坏方块不会显示粒子效果（对fps提升有奇效）",
            performance::isDisableBlockBreakingParticles,
            performance::setDisableBlockBreakingParticles,
            y);
        y += spacing;

        addBooleanOption("disableDeadMobRendering",
            "关闭死亡生物渲染",
            "不渲染已死亡的生物",
            performance::isDisableDeadMobRendering,
            performance::setDisableDeadMobRendering,
            y);
        y += spacing;

        addBooleanOption("disableEntityRendering",
            "关闭实体渲染",
            "不渲染非玩家实体",
            performance::isDisableEntityRendering,
            performance::setDisableEntityRendering,
            y);
        y += spacing;

        addBooleanOption("disableFallingBlockEntityRendering",
            "关闭掉落方块渲染",
            "不渲染掉落的方块实体",
            performance::isDisableFallingBlockEntityRendering,
            performance::setDisableFallingBlockEntityRendering,
            y);
        y += spacing;

        addBooleanOption("disableParticles",
            "关闭粒子效果",
            "禁用所有粒子效果",
            performance::isDisableParticles,
            performance::setDisableParticles,
            y);
        y += spacing;

        addBooleanOption("disablePortalGuiClosing",
            "禁用传送门GUI关闭",
            "防止传送门效果关闭GUI界面",
            performance::isDisablePortalGuiClosing,
            performance::setDisablePortalGuiClosing,
            y);
        y += spacing;

        // 添加返回按钮
        this.addRenderableWidget(Button.builder(
            Component.literal("完成"),
            button -> this.onClose()
        ).bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    private void addBooleanOption(String id, String name, String tooltip,
                                  BooleanSupplier getter, BooleanConsumer setter, int y) {
        CycleButton<Boolean> button = CycleButton.<Boolean>builder(value -> 
                Component.literal(Boolean.TRUE.equals(value) ? "开启" : "关闭"))
            .withValues(true, false)
            .withInitialValue(getter.getAsBoolean())
            .withTooltip(value -> Tooltip.create(Component.literal(tooltip)))
            .create(this.width / 2 - 155, y, 310, 20, 
                   Component.literal(name),
                   (button1, value) -> setter.accept(Boolean.TRUE.equals(value)));

        this.addRenderableWidget(button);
        buttons.add(button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 5, 16777215);
        super.render(graphics, mouseX, mouseY, partialTick);

        for (CycleButton<Boolean> button : buttons) {
            if (button.isHoveredOrFocused() && button.getTooltip() != null) {
                String tooltipText = button.getTooltip().toString()
                    .replace("Tooltip[", "")
                    .replace("]", "");
                
                graphics.renderTooltip(
                    this.font,
                    Component.literal(tooltipText),
                    mouseX,
                    mouseY
                );
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
} 