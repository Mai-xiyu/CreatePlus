package org.xiyu.yee.createplus.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.features.SpeedAdjust;
import org.xiyu.yee.createplus.features.Performance;
import org.xiyu.yee.createplus.features.GammaOverride;
import org.xiyu.yee.createplus.features.PingDisplay;
import org.xiyu.yee.createplus.features.MiniHUD;
import org.xiyu.yee.createplus.ui.FeatureScreen;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "createplus/createplus.json";
    private static JsonObject config;

    public static void init() {
        loadConfig();
    }

    public static void loadConfig() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                saveConfig(); // 创建默认配置
                return;
            }

            String jsonStr = new String(Files.readAllBytes(configFile.toPath()));
            config = GSON.fromJson(jsonStr, JsonObject.class);

            // 加载所有功能的配置
            for (CreativePlusFeature feature : Createplus.FEATURE_MANAGER.getFeatures()) {
                loadFeatureConfig(feature);
            }
        } catch (Exception e) {
            e.printStackTrace();
            config = new JsonObject();
        }
    }

    public static void saveConfig() {
        try {
            if (config == null) {
                config = new JsonObject();
            }

            // 保存所有功能的配置
        for (CreativePlusFeature feature : Createplus.FEATURE_MANAGER.getFeatures()) {
                saveFeatureConfig(feature);
            }

            String jsonStr = GSON.toJson(config);
            Files.write(Path.of(CONFIG_FILE), jsonStr.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadFeatureConfig(CreativePlusFeature feature) {
        String featureName = feature.getName();
        if (!config.has(featureName)) return;

        JsonObject featureConfig = config.getAsJsonObject(featureName);
        
        // 加载功能启用状态
        if (featureConfig.has("enabled")) {
            boolean enabled = featureConfig.get("enabled").getAsBoolean();
            feature.setEnabled(enabled);
        }

        // 加载特定功能的配置
        if (feature instanceof SpeedAdjust) {
            loadSpeedAdjustConfig((SpeedAdjust) feature, featureConfig);
        } else if (feature instanceof Performance) {
            loadPerformanceConfig((Performance) feature, featureConfig);
        } else if (feature instanceof GammaOverride) {
            loadGammaOverrideConfig((GammaOverride) feature, featureConfig);
        } else if (feature instanceof PingDisplay) {
            loadPingDisplayConfig((PingDisplay) feature, featureConfig);
        } else if (feature instanceof MiniHUD) {
            loadMiniHUDConfig((MiniHUD) feature, featureConfig);
        }
    }

    private static void saveFeatureConfig(CreativePlusFeature feature) {
        String featureName = feature.getName();
        JsonObject featureConfig = new JsonObject();
        
        // 保存功能启用状态
        featureConfig.addProperty("enabled", feature.isEnabled());

        // 保存特定功能的配置
        if (feature instanceof SpeedAdjust) {
            saveSpeedAdjustConfig((SpeedAdjust) feature, featureConfig);
        } else if (feature instanceof Performance) {
            savePerformanceConfig((Performance) feature, featureConfig);
        } else if (feature instanceof GammaOverride) {
            saveGammaOverrideConfig((GammaOverride) feature, featureConfig);
        } else if (feature instanceof PingDisplay) {
            savePingDisplayConfig((PingDisplay) feature, featureConfig);
        } else if (feature instanceof MiniHUD) {
            saveMiniHUDConfig((MiniHUD) feature, featureConfig);
        }

        config.add(featureName, featureConfig);
    }

    private static void loadSpeedAdjustConfig(SpeedAdjust speedAdjust, JsonObject config) {
        if (config.has("speeds")) {
            JsonObject speeds = config.getAsJsonObject("speeds");
            for (SpeedAdjust.SpeedType type : SpeedAdjust.SpeedType.values()) {
                if (speeds.has(type.name())) {
                    float value = speeds.get(type.name()).getAsFloat();
                    speedAdjust.setSpeedValue(type, value);
                }
            }
        }
    }

    private static void saveSpeedAdjustConfig(SpeedAdjust speedAdjust, JsonObject config) {
        JsonObject speeds = new JsonObject();
        for (SpeedAdjust.SpeedType type : SpeedAdjust.SpeedType.values()) {
            speeds.addProperty(type.name(), speedAdjust.getSpeedValue(type));
        }
        config.add("speeds", speeds);
    }

    private static void loadPerformanceConfig(Performance performance, JsonObject config) {
        // 粒子效果
        if (config.has("disableParticles")) {
            performance.setDisableParticles(config.get("disableParticles").getAsBoolean());
        }
        
        // 方块破坏粒子
        if (config.has("disableBlockBreakingParticles")) {
            performance.setDisableBlockBreakingParticles(config.get("disableBlockBreakingParticles").getAsBoolean());
        }
        
        // 死亡生物渲染
        if (config.has("disableDeadMobRendering")) {
            performance.setDisableDeadMobRendering(config.get("disableDeadMobRendering").getAsBoolean());
        }
        
        // 实体渲染
        if (config.has("disableEntityRendering")) {
            performance.setDisableEntityRendering(config.get("disableEntityRendering").getAsBoolean());
        }
        
        // 掉落方块渲染
        if (config.has("disableFallingBlockEntityRendering")) {
            performance.setDisableFallingBlockEntityRendering(config.get("disableFallingBlockEntityRendering").getAsBoolean());
        }
        
        // 传送门GUI关闭
        if (config.has("disablePortalGuiClosing")) {
            performance.setDisablePortalGuiClosing(config.get("disablePortalGuiClosing").getAsBoolean());
        }
    }

    private static void savePerformanceConfig(Performance performance, JsonObject config) {
        // 粒子效果
        config.addProperty("disableParticles", performance.isDisableParticles());
        
        // 方块破坏粒子
        config.addProperty("disableBlockBreakingParticles", performance.isDisableBlockBreakingParticles());
        
        // 死亡生物渲染
        config.addProperty("disableDeadMobRendering", performance.isDisableDeadMobRendering());
        
        // 实体渲染
        config.addProperty("disableEntityRendering", performance.isDisableEntityRendering());
        
        // 掉落方块渲染
        config.addProperty("disableFallingBlockEntityRendering", performance.isDisableFallingBlockEntityRendering());
        
        // 传送门GUI关闭
        config.addProperty("disablePortalGuiClosing", performance.isDisablePortalGuiClosing());
    }

    private static void loadGammaOverrideConfig(GammaOverride gammaOverride, JsonObject config) {
        if (config.has("originalGamma")) {
            gammaOverride.setOriginalGamma(config.get("originalGamma").getAsDouble());
        }
        if (config.has("nightVisionGamma")) {
            gammaOverride.setNightVisionGamma(config.get("nightVisionGamma").getAsDouble());
        }
    }

    private static void saveGammaOverrideConfig(GammaOverride gammaOverride, JsonObject config) {
        config.addProperty("originalGamma", gammaOverride.getOriginalGamma());
        config.addProperty("nightVisionGamma", gammaOverride.getNightVisionGamma());
    }

    private static void loadPingDisplayConfig(PingDisplay pingDisplay, JsonObject config) {
        if (config.has("backgroundColor")) {
            pingDisplay.setBackgroundColor(config.get("backgroundColor").getAsInt());
        }
        if (config.has("backgroundOpacity")) {
            pingDisplay.setBackgroundOpacity(config.get("backgroundOpacity").getAsFloat());
        }
    }

    private static void savePingDisplayConfig(PingDisplay pingDisplay, JsonObject config) {
        config.addProperty("backgroundColor", pingDisplay.getBackgroundColor());
        config.addProperty("backgroundOpacity", pingDisplay.getBackgroundOpacity());
    }

    private static void loadMiniHUDConfig(MiniHUD miniHUD, JsonObject config) {
        // 加载文本颜色
        if (config.has("textColors")) {
            JsonObject textColors = config.getAsJsonObject("textColors");
            for (String element : new String[]{"fps", "time", "pos", "biome", "localtime"}) {
                if (textColors.has(element)) {
                    miniHUD.setTextColor(element, textColors.get(element).getAsInt());
                }
            }
        }

        // 加载数值颜色
        if (config.has("valueColors")) {
            JsonObject valueColors = config.getAsJsonObject("valueColors");
            for (String element : new String[]{"fps", "time", "pos", "biome", "localtime"}) {
                if (valueColors.has(element)) {
                    miniHUD.setValueColor(element, valueColors.get(element).getAsInt());
                }
            }
        }
    }

    private static void saveMiniHUDConfig(MiniHUD miniHUD, JsonObject config) {
        // 保存文本颜色
        JsonObject textColors = new JsonObject();
        for (String element : new String[]{"fps", "time", "pos", "biome", "localtime"}) {
            textColors.addProperty(element, miniHUD.getTextColor(element));
        }
        config.add("textColors", textColors);

        // 保存数值颜色
        JsonObject valueColors = new JsonObject();
        for (String element : new String[]{"fps", "time", "pos", "biome", "localtime"}) {
            valueColors.addProperty(element, miniHUD.getValueColor(element));
        }
        config.add("valueColors", valueColors);
    }

} 