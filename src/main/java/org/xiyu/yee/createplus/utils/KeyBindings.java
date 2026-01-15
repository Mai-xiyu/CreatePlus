package org.xiyu.yee.createplus.utils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.xiyu.yee.createplus.Createplus;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
        "key." + Createplus.MODID + ".toggle_hud", // 翻译键
        KeyConflictContext.IN_GAME,  // 只在游戏中生效
        InputConstants.Type.KEYSYM,   // 键盘按键
        InputConstants.KEY_F9,        // 默认F9
        "key.categories." + Createplus.MODID  // 按键分类
    );

    // 添加新的按键绑定
    public static final KeyMapping TOGGLE_FREECAM = new KeyMapping(
            "key.createplus.toggle_freecam",
            GLFW.GLFW_KEY_F6,
            "key.categories.createplus"
    );

    public static final KeyMapping TOGGLE_ZOOM = new KeyMapping(
            "key.createplus.toggle_zoom",
            GLFW.GLFW_KEY_C,
            "key.categories.createplus"
    );

    public static final KeyMapping MODIFIER = new KeyMapping(
        "key.createplus.modifier",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_ALT,
        "key.categories.createplus"
    );

    public static final KeyMapping TOGGLE_MODULES = new KeyMapping(
        "key.createplus.toggle_modules",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_RIGHT_SHIFT,
        "key.categories.createplus"
    );
} 