package org.xiyu.yee.createplus.features;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 带有子HUD的功能接口
 */
public interface SubHUDFeature {
    /**
     * 处理键盘输入
     * @param keyCode 按键代码
     * @return 是否处理了该按键
     */
    boolean handleKeyPress(int keyCode);

    /**
     * 检查子HUD是否可见
     */
    boolean isSubHUDVisible();

    /**
     * 切换子HUD的显示状态
     */
    void toggleSubHUD();

    /**
     * 渲染子HUD
     * @param graphics GuiGraphics实例
     * @param x 渲染位置X
     * @param y 渲染位置Y
     */
    void renderSubHUD(GuiGraphics graphics, int x, int y);
}