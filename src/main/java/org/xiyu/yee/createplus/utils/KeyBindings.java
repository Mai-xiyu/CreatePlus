package org.xiyu.yee.createplus.utils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import org.xiyu.yee.createplus.Createplus;

public final class KeyBindings {
    private static final String CATEGORY = "key.categories." + Createplus.MODID;

    /** F9：开关屏幕左上角的快捷功能菜单。 */
    public static final KeyMapping TOGGLE_HUD = create("toggle_hud", GLFW.GLFW_KEY_F9);

    /** 右 Shift：打开完整的功能设置界面。 */
    public static final KeyMapping TOGGLE_MODULES = create("toggle_modules", GLFW.GLFW_KEY_RIGHT_SHIFT);

    /** F6：进入/退出灵魂出窍。 */
    public static final KeyMapping TOGGLE_FREECAM = create("toggle_freecam", GLFW.GLFW_KEY_F6);

    /** C：按住缩放视角。 */
    public static final KeyMapping TOGGLE_ZOOM = create("toggle_zoom", GLFW.GLFW_KEY_C);

    /** 左 Ctrl：配合滚轮使用的功能修饰键（方块变色、范围调整等）。 */
    public static final KeyMapping MODIFIER = create("modifier", GLFW.GLFW_KEY_LEFT_CONTROL);

    /** 用当前手持方块填充选区。默认不绑定，避免和其他模组冲突。 */
    public static final KeyMapping FILL_SELECTION = create("fill_selection", InputConstants.UNKNOWN.getValue());

    /** 撤销上一次区域操作。 */
    public static final KeyMapping UNDO = create("undo", InputConstants.UNKNOWN.getValue());

    /** 打开命令工作室。 */
    public static final KeyMapping COMMAND_STUDIO = create("command_studio", GLFW.GLFW_KEY_F7);

    /** 退出 HUD 编辑模式（进入靠开启「HUD 编辑」功能）。 */
    public static final KeyMapping HUD_EDIT = create("hud_edit", GLFW.GLFW_KEY_F8);

    private KeyBindings() {
    }

    private static KeyMapping create(String id, int defaultKey) {
        return new KeyMapping(
            "key." + Createplus.MODID + "." + id,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            defaultKey,
            CATEGORY);
    }

    /** 所有需要注册到游戏里的按键。漏注册的按键在控制设置里看不到，也永远不会触发。 */
    public static KeyMapping[] all() {
        return new KeyMapping[]{
            TOGGLE_HUD, TOGGLE_MODULES, TOGGLE_FREECAM, TOGGLE_ZOOM,
            MODIFIER, FILL_SELECTION, UNDO, COMMAND_STUDIO, HUD_EDIT
        };
    }

    /** 修饰键是否按下。用于区分"滚轮切物品"和"滚轮调参数"。 */
    public static boolean modifierDown() {
        return MODIFIER.isDown();
    }
}
