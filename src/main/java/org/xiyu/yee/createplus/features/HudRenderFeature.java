package org.xiyu.yee.createplus.features;

import net.minecraft.client.gui.GuiGraphics;

/** 需要在 HUD（2D 屏幕空间）上绘制内容的功能。 */
public interface HudRenderFeature {
    void renderHud(GuiGraphics graphics, int screenWidth, int screenHeight, float partialTick);
}
