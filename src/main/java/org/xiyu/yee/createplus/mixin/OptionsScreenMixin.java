package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;
import org.xiyu.yee.createplus.ui.FeatureSettingsScreen;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void createplus$addPerformanceButton(CallbackInfo ci) {
        Performance performance = Createplus.performance();
        if (performance == null) return;

        // 以前只在功能已启用时才加按钮，于是玩家根本找不到开启它的地方
        Screen self = (Screen) (Object) this;
        addRenderableWidget(Button.builder(
                Component.translatable("createplus.performance.button"),
                button -> Minecraft.getInstance().setScreen(
                    new FeatureSettingsScreen(self, performance)))
            // 放在左上角空白处，不和原版居中排布的按钮抢位置
            .bounds(6, 6, 130, 20)
            .build());
    }
}
