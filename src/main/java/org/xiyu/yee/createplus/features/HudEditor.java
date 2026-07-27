package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import org.xiyu.yee.createplus.ui.hud.HudEditScreen;

/**
 * 打开后进入 HUD 编辑模式：可拖动信息面板、方块信息、Ping、记分板等。
 * 只能按 F8 退出（Esc 无效）。
 */
public class HudEditor extends CreativePlusFeature {
    public HudEditor() {
        super("hud_editor", "进入 HUD 编辑模式，拖动各 HUD 避免重叠；F8 退出", Category.HUD);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.execute(() -> mc.setScreen(new HudEditScreen()));
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.screen instanceof HudEditScreen) {
            // 由屏幕自己关；这里避免循环
            mc.execute(() -> {
                if (mc.screen instanceof HudEditScreen) {
                    ((HudEditScreen) mc.screen).exitByFeatureDisable();
                }
            });
        }
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription() + "\n§e开启即进入编辑 · 仅 F8 可退出";
    }
}
