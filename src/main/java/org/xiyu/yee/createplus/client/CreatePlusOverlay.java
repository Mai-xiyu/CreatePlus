package org.xiyu.yee.createplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.ui.FeatureScreen;

/** CreatePlus 的 HUD 图层，每帧调用一次。 */
public final class CreatePlusOverlay {

    private CreatePlusOverlay() {
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                              int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        // F1 隐藏界面、F3 调试屏幕打开时都让位
        if (mc.options.hideGui || mc.options.renderDebug) return;

        if (Createplus.FEATURE_MANAGER != null) {
            Createplus.FEATURE_MANAGER.renderHud(graphics, screenWidth, screenHeight, partialTick);
        }
        FeatureScreen.get().render(graphics, screenWidth, screenHeight, partialTick);
    }
}
