package org.xiyu.yee.createplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.ChatFormatting;
import org.xiyu.yee.createplus.utils.KeyBindings;

/** 进入世界后的一次性提示。 */
public final class WelcomeMessage {

    private WelcomeMessage() {
    }

    public static void send() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        player.displayClientMessage(Component.literal("CreatePlus")
            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
            .append(Component.literal(" 已就绪").withStyle(ChatFormatting.GRAY)), false);

        player.displayClientMessage(Component.literal("  ")
            .append(key(KeyBindings.TOGGLE_HUD.getTranslatedKeyMessage().getString()))
            .append(Component.literal(" 快捷菜单    ").withStyle(ChatFormatting.GRAY))
            .append(key(KeyBindings.TOGGLE_MODULES.getTranslatedKeyMessage().getString()))
            .append(Component.literal(" 设置界面").withStyle(ChatFormatting.GRAY)), false);

        player.displayClientMessage(clickable("  [/createplus]", "/createplus")
            .append(Component.literal(" 查看全部功能    ").withStyle(ChatFormatting.GRAY))
            .append(clickable("[/createplus help]", "/createplus help"))
            .append(Component.literal(" 使用帮助").withStyle(ChatFormatting.GRAY)), false);
    }

    private static MutableComponent key(String label) {
        return Component.literal("[" + label + "]").withStyle(ChatFormatting.YELLOW);
    }

    private static MutableComponent clickable(String label, String command) {
        return Component.literal(label).withStyle(Style.EMPTY
            .withColor(ChatFormatting.GOLD)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }
}
