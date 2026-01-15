package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MiniHUD extends CreativePlusFeature {
    private static final int SHADOW_COLOR = 0x000000;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    
    // 颜色配置
    private final Map<String, Integer> textColors = new HashMap<>();
    private final Map<String, Integer> valueColors = new HashMap<>();
    
    public MiniHUD() {
        super("mini_hud", "mini_hud");
        MinecraftForge.EVENT_BUS.register(this);
        initDefaultColors();
    }

    private void initDefaultColors() {
        // 默认文本颜色
        textColors.put("fps", 0xFFFFFF);
        textColors.put("time", 0xFFFFFF);
        textColors.put("pos", 0xFFFFFF);
        textColors.put("biome", 0xFFFFFF);
        textColors.put("localtime", 0xFFFFFF);

        // 默认数值颜色
        valueColors.put("fps", 0xFFFFFF);
        valueColors.put("time", 0xFFFFFF);
        valueColors.put("pos", 0xFFFFFF);
        valueColors.put("biome", 0xFFFFFF);
        valueColors.put("localtime", 0xFFFFFF);
    }

    @SubscribeEvent
    public void onClientChat(ClientChatEvent event) {
        String message = event.getMessage().trim();
        if (message.startsWith(".hudcolor ")) {
            event.setCanceled(true);
            handleColorCommand(message.substring(10));
        }
    }

    private void handleColorCommand(String args) {
        Minecraft mc = Minecraft.getInstance();
        String[] parts = args.split(" ");
        
        if (parts.length < 3) {
            sendHelp(mc);
            return;
        }

        try {
            String element = parts[0].toLowerCase();
            String type = parts[1].toLowerCase();
            int color = Integer.parseInt(parts[2], 16) | 0xFF000000; // 确保 alpha 通道为 FF

            if (!isValidElement(element)) {
                mc.player.displayClientMessage(
                    Component.literal("§c无效的元素。可用元素: fps, time, pos, biome, localtime"), 
                    false
                );
                return;
            }

            switch (type) {
                case "text" -> textColors.put(element, color);
                case "value" -> valueColors.put(element, color);
                default -> {
                    mc.player.displayClientMessage(
                        Component.literal("§c无效的类型。可用类型: text, value"), 
                        false
                    );
                    return;
                }
            }

            mc.player.displayClientMessage(
                Component.literal(String.format("§a已设置 %s 的%s颜色为: #%06X", 
                    element, type.equals("text") ? "文本" : "数值", color & 0xFFFFFF)), 
                false
            );

        } catch (NumberFormatException e) {
            mc.player.displayClientMessage(
                Component.literal("§c无效的颜色代码。请使用十六进制格式 (例如: FFFFFF)"), 
                false
            );
        }
    }

    private void sendHelp(Minecraft mc) {
        mc.player.displayClientMessage(Component.literal("§6=== MiniHUD 颜色设置帮助 ==="), false);
        mc.player.displayClientMessage(Component.literal("§7用法: .hudcolor <元素> <类型> <颜色>"), false);
        mc.player.displayClientMessage(Component.literal("§7元素: fps, time, pos, biome, localtime"), false);
        mc.player.displayClientMessage(Component.literal("§7类型: text (文本), value (数值)"), false);
        mc.player.displayClientMessage(Component.literal("§7颜色: 六位十六进制颜色代码"), false);
        mc.player.displayClientMessage(Component.literal("§7示例: .hudcolor fps value FF0000"), false);
    }

    private boolean isValidElement(String element) {
        return textColors.containsKey(element);
    }

    @Override
    public void onEnable() {
        // 不需要特殊初始化
    }

    @Override
    public void onDisable() {
        // 不需要特殊清理
    }

    @Override
    public void onTick() {
        // 不需要tick更新
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int y = 5;
        int lineHeight = 10;

        Player player = mc.player;
        BlockPos pos = player.blockPosition();
        String biome = mc.level.getBiome(pos).unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("unknown");

        // FPS
        renderSplitLine(graphics, "fps", String.valueOf(mc.getFps()), 
            "fps", 5, y);
        y += lineHeight;

        // 游戏时间
        renderSplitLine(graphics, "time", String.valueOf(mc.level.getDayTime() / 24000L), 
            "time", 5, y);
        renderSplitLine(graphics, "time", formatGameTime(mc.level.getDayTime() % 24000L), 
            "time", 5 + mc.font.width("天数: " + mc.level.getDayTime() / 24000L), y);
        y += lineHeight;

        // 坐标
        renderSplitLine(graphics, "pos", 
            String.format("%d %d %d", pos.getX(), pos.getY(), pos.getZ()),
            "pos", 5, y);
        y += lineHeight;

        // 生物群系
        renderSplitLine(graphics, "biome", formatBiomeName(biome),
            "biome", 5, y);
        y += lineHeight;

        // 本地时间
        renderSplitLine(graphics, "localtime", TIME_FORMAT.format(new Date()),
            "localtime", 5, y);
    }

    private void renderSplitLine(GuiGraphics graphics, String labelKey, String value, 
                               String element, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        // 渲染标签文本
        Component label = Component.translatable("minihud." + labelKey);
        graphics.drawString(mc.font, label, x, y, textColors.get(element));
        
        // 渲染数值
        graphics.drawString(mc.font, value, 
            x + mc.font.width(label.getString()) + 2, y, 
            valueColors.get(element));
    }

    private void renderText(GuiGraphics graphics, String text, int x, int y, int color) {
        // 渲染阴影
        graphics.drawString(Minecraft.getInstance().font, text, x + 1, y + 1, SHADOW_COLOR);
        // 渲染文本
        graphics.drawString(Minecraft.getInstance().font, text, x, y, color);
    }

    private String formatGameTime(long time) {
        int hours = (int) ((time / 1000 + 6) % 24);
        int minutes = (int) ((time % 1000) * 60 / 1000);
        return String.format("%02d:%02d", hours, minutes);
    }

    private String formatBiomeName(String biomeName) {
        // 将生物群系名称转换为更友好的显示格式
        return biomeName.replace('_', ' ')
                .replace("plains", "平原")
                .replace("forest", "森林")
                .replace("desert", "沙漠")
                .replace("ocean", "海洋")
                .replace("mountain", "山脉")
                .replace("river", "河流")
                .replace("beach", "海滩")
                .replace("jungle", "丛林")
                .replace("savanna", "热带草原")
                .replace("taiga", "针叶林")
                .replace("snowy", "雪地")
                .replace("frozen", "冰冻")
                .replace("deep", "深")
                .replace("cold", "寒冷")
                .replace("warm", "温暖")
                .replace("lukewarm", "温和");
    }

    public int getTextColor(String element) {
        return textColors.getOrDefault(element, 0xFFFFFF);
    }

    public void setTextColor(String element, int color) {
        textColors.put(element, color);
    }

    public int getValueColor(String element) {
        return valueColors.getOrDefault(element, 0xFFFFFF);
    }

    public void setValueColor(String element, int color) {
        valueColors.put(element, color);
    }
} 