package org.xiyu.yee.createplus.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.commands.studio.AiSessionHistory;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.settings.Setting;
import org.xiyu.yee.createplus.ui.FeatureScreen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 配置读写。
 *
 * 格式：
 * <pre>
 * {
 *   "ui": { "opacity": 0.9 },
 *   "features": {
 *     "speed_adjust": { "enabled": true, "settings": { "fly": 2.0 } }
 *   }
 * }
 * </pre>
 *
 * 所有功能都通过通用的 Setting 序列化，新增功能不需要改动本类。
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "createplus.json";

    private ConfigManager() {
    }

    private static Path configPath() throws IOException {
        return FileUtils.getModDirectory().resolve(FILE_NAME);
    }

    public static synchronized void load() {
        try {
            Path path = configPath();
            if (!Files.exists(path)) {
                save();
                return;
            }

            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) return;

            if (root.has("ui") && root.get("ui").isJsonObject()) {
                readUi(root.getAsJsonObject("ui"));
            }

            if (root.has("features") && root.get("features").isJsonObject()) {
                JsonObject features = root.getAsJsonObject("features");
                for (CreativePlusFeature feature : Createplus.FEATURE_MANAGER.getFeatures()) {
                    if (!features.has(feature.getName())) continue;
                    readFeature(feature, features.getAsJsonObject(feature.getName()));
                }
            }

            if (root.has("hud_layout") && root.get("hud_layout").isJsonObject()) {
                HudLayout.fromJson(root.getAsJsonObject("hud_layout"));
            }

            if (root.has("ai_studio") && root.get("ai_studio").isJsonObject()) {
                AiSessionHistory.fromJson(root.getAsJsonObject("ai_studio"));
            }

            // 信息面板的透明度驱动 F9 菜单
            var mini = Createplus.FEATURE_MANAGER.get(
                org.xiyu.yee.createplus.features.MiniHUD.class);
            if (mini != null) mini.syncMenuOpacity();
        } catch (Exception e) {
            Createplus.LOGGER.error("CreatePlus: failed to load config, using defaults", e);
        }
    }

    public static synchronized void save() {
        try {
            JsonObject root = new JsonObject();
            root.add("ui", writeUi());
            root.add("hud_layout", HudLayout.toJson());
            root.add("ai_studio", AiSessionHistory.toJson());

            JsonObject features = new JsonObject();
            for (CreativePlusFeature feature : Createplus.FEATURE_MANAGER.getFeatures()) {
                features.add(feature.getName(), writeFeature(feature));
            }
            root.add("features", features);

            Path path = configPath();
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Createplus.LOGGER.error("CreatePlus: failed to save config", e);
        }
    }

    private static void readFeature(CreativePlusFeature feature, JsonObject json) {
        if (json.has("settings") && json.get("settings").isJsonObject()) {
            JsonObject settings = json.getAsJsonObject("settings");
            for (Setting<?> setting : feature.getSettings()) {
                if (settings.has(setting.getId())) {
                    setting.fromJson(settings.get(setting.getId()));
                }
            }
        }

        // 启用状态最后应用：这样 onEnable() 能读到已经加载好的设置值。
        // 此时玩家实体通常还不存在，需要玩家的功能会在 onWorldJoin() 里补上。
        if (json.has("enabled")) {
            boolean enabled = json.get("enabled").getAsBoolean();
            // HUD 编辑模式不应在启动时自动进入
            if (feature instanceof org.xiyu.yee.createplus.features.HudEditor) {
                enabled = false;
            }
            feature.setEnabled(enabled);
        }
    }

    private static JsonObject writeFeature(CreativePlusFeature feature) {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", feature.isEnabled());
        if (feature.hasSettings()) {
            JsonObject settings = new JsonObject();
            for (Setting<?> setting : feature.getSettings()) {
                settings.add(setting.getId(), setting.toJson());
            }
            json.add("settings", settings);
        }
        return json;
    }

    private static JsonObject writeUi() {
        JsonObject ui = new JsonObject();
        ui.addProperty("opacity", FeatureScreen.getOpacity());
        return ui;
    }

    private static void readUi(JsonObject ui) {
        if (ui.has("opacity")) {
            float opacity = ui.get("opacity").getAsFloat();
            FeatureScreen.setOpacity(opacity);
            // 兼容旧配置：把顶栏透明度迁到信息面板设置
            var mini = Createplus.FEATURE_MANAGER.get(
                org.xiyu.yee.createplus.features.MiniHUD.class);
            if (mini != null) {
                var setting = mini.getSetting("hud_opacity");
                if (setting instanceof org.xiyu.yee.createplus.settings.DoubleSetting ds) {
                    ds.set((double) opacity);
                }
            }
        }
    }
}
