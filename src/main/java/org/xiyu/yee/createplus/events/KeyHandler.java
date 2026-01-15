package org.xiyu.yee.createplus.events;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.ui.FeatureScreen;
import org.xiyu.yee.createplus.ui.clickgui.ClickGuiScreen; // 记得导入新的 ClickGui
import org.xiyu.yee.createplus.utils.KeyBindings;

@Mod.EventBusSubscriber(modid = Createplus.MODID, value = Dist.CLIENT)
public class KeyHandler {

    // 缓存 ClickGui 实例，保留面板位置状态
    private static ClickGuiScreen clickGui;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        // F9 切换 GTA V 风格菜单 (Overlay)
        if (mc.screen == null && KeyBindings.TOGGLE_HUD.consumeClick()) {
            FeatureScreen.toggleVisibility();
        }

        // 右 Shift 打开 Click GUI
        if (mc.screen == null && KeyBindings.TOGGLE_MODULES.consumeClick()) {
            if (clickGui == null) {
                clickGui = new ClickGuiScreen();
            }
            mc.setScreen(clickGui);
        }
    }
}