package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TimeWeatherControl extends CreativePlusFeature {
    private static final List<String> WEATHER_TYPES = Arrays.asList("晴天", "雨天", "雷暴");
    private static final List<String> TIME_PRESETS = Arrays.asList("日出", "中午", "日落", "前夜", "午夜");
    
    public TimeWeatherControl() {
        super("time_weather", "控制本地时间和天气");
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientChat(ClientChatEvent event) {
        String message = event.getMessage().trim();
        
        if (message.startsWith(".weather ")) {
            event.setCanceled(true);
            handleWeatherCommand(message.substring(9));
        } else if (message.startsWith(".time ")) {
            event.setCanceled(true);
            handleTimeCommand(message.substring(6));
        }
    }

    private void handleWeatherCommand(String type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        switch (type) {
            case "晴天" -> mc.level.setRainLevel(0);
            case "雨天" -> {
                mc.level.setRainLevel(1);
                mc.level.setThunderLevel(0);
            }
            case "雷暴" -> {
                mc.level.setRainLevel(1);
                mc.level.setThunderLevel(1);
            }
            default -> mc.player.displayClientMessage(
                Component.literal("§c无效的天气类型。可用类型: 晴天, 雨天, 雷暴"), 
                false
            );
        }
    }

    private void handleTimeCommand(String timeStr) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        try {
            long time = switch (timeStr) {
                case "日出" -> 0;
                case "中午" -> 6000;
                case "日落" -> 12000;
                case "前夜" -> 13000;
                case "午夜" -> 18000;
                default -> Long.parseLong(timeStr);
            };
            
            if (time >= 0 && time <= 24000) {
                mc.level.setDayTime(time);
            } else {
                mc.player.displayClientMessage(
                    Component.literal("§c时间必须在 0-24000 之间"), 
                    false
                );
            }
        } catch (NumberFormatException e) {
            mc.player.displayClientMessage(
                Component.literal("§c无效的时间格式。可用预设: 日出, 中午, 日落, 前夜, 午夜"), 
                false
            );
        }
    }

    // TAB补全支持
    public static List<String> getWeatherSuggestions(String input) {
        return WEATHER_TYPES.stream()
            .filter(type -> type.startsWith(input))
            .collect(Collectors.toList());
    }

    public static List<String> getTimeSuggestions(String input) {
        return TIME_PRESETS.stream()
            .filter(preset -> preset.startsWith(input))
            .collect(Collectors.toList());
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
} 