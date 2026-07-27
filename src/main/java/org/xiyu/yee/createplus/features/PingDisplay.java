package org.xiyu.yee.createplus.features;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.utils.HudLayout;
import org.xiyu.yee.createplus.utils.RenderUtils;

/**
 * 在玩家名牌后面追加延迟，可选在屏幕上显示自己的延迟。
 */
public class PingDisplay extends CreativePlusFeature implements HudRenderFeature {

    private final BooleanSetting nameTags = boolSetting("name_tags", true);
    private final BooleanSetting ownPing = boolSetting("own_ping", false);
    private final IntSetting goodThreshold = intSetting("good_threshold", 60, 10, 300);
    private final IntSetting badThreshold = intSetting("bad_threshold", 200, 50, 1000);

    public PingDisplay() {
        super("ping_display", "在玩家名牌与 HUD 上显示网络延迟", Category.HUD);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onRenderNameTag(RenderNameTagEvent event) {
        if (!isEnabled() || !nameTags.get()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        int ping = pingOf(player);
        if (ping < 0) return;

        event.setContent(Component.empty()
            .append(event.getContent())
            .append(Component.literal(" "))
            .append(Component.literal(ping + "ms").withStyle(colorFor(ping))));
    }

    @Override
    public void renderHud(GuiGraphics graphics, int screenWidth, int screenHeight, float partialTick) {
        if (!ownPing.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int ping = pingOf(mc.player);
        if (ping < 0) return;

        String text = "Ping " + ping + "ms";
        if (HudLayout.hasCustom(HudLayout.PING)) {
            HudLayout.Pos pos = HudLayout.get(HudLayout.PING);
            graphics.drawString(mc.font, text, pos.x, pos.y, argbFor(ping), true);
        } else {
            RenderUtils.drawRightAlignedText(graphics, text, screenWidth - 4, 4, argbFor(ping), true);
        }
    }

    private int argbFor(int ping) {
        if (ping <= goodThreshold.value()) return 0xFF52D273;
        if (ping <= badThreshold.value()) return 0xFFFFC24B;
        return 0xFFFF6B81;
    }

    private static int pingOf(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return -1;
        PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    private ChatFormatting colorFor(int ping) {
        if (ping <= goodThreshold.value()) return ChatFormatting.GREEN;
        if (ping <= badThreshold.value()) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + String.format("\n§a≤%dms §7良好  §e≤%dms §7一般  §c更高 §7较差",
                goodThreshold.value(), badThreshold.value());
    }
}
