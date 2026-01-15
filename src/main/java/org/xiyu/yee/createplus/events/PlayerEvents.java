package org.xiyu.yee.createplus.events;

import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xiyu.yee.createplus.Createplus;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

@Mod.EventBusSubscriber(modid = Createplus.MODID)
public class PlayerEvents {
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        event.getEntity().sendSystemMessage(Component.literal("=== CreatePlus 模组帮助 ===").withStyle(ChatFormatting.GOLD));
        event.getEntity().sendSystemMessage(Component.literal("按 F9 打开功能菜单").withStyle(ChatFormatting.YELLOW));
        event.getEntity().sendSystemMessage(Component.literal("功能菜单中:").withStyle(ChatFormatting.GRAY));
        event.getEntity().sendSystemMessage(Component.literal("- 上下方向键选择功能").withStyle(ChatFormatting.GRAY));
        event.getEntity().sendSystemMessage(Component.literal("- 回车键开启/关闭功能").withStyle(ChatFormatting.GRAY));
        event.getEntity().sendSystemMessage(Component.literal("- 右方向键打开子菜单(如果有)").withStyle(ChatFormatting.GRAY));
        event.getEntity().sendSystemMessage(Component.literal("建筑相关功能:").withStyle(ChatFormatting.AQUA));
        event.getEntity().sendSystemMessage(Component.literal("- 使用木铲选择区域").withStyle(ChatFormatting.GRAY));
        event.getEntity().sendSystemMessage(Component.literal("- /exportbuilding <名称> 导出建筑").withStyle(ChatFormatting.GRAY));
        event.getEntity().sendSystemMessage(Component.literal("- /importbuilding <名称> 导入建筑").withStyle(ChatFormatting.GRAY));
        event.getEntity().sendSystemMessage(Component.literal("更多帮助请使用 /features help").withStyle(ChatFormatting.GREEN));
        event.getEntity().sendSystemMessage(Component.literal("===================").withStyle(ChatFormatting.GOLD));
    }
}