package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.DoubleSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.settings.Setting;
import org.xiyu.yee.createplus.settings.StringSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class CreativePlusFeature {

    /** 功能分类。取代了以前靠功能名字符串匹配的脆弱分类方式。 */
    public enum Category {
        BUILDING("building", 0xFF00A2FF),
        MOVEMENT("movement", 0xFF52D273),
        RENDER("render", 0xFFC792EA),
        HUD("hud", 0xFFFFC24B),
        SYSTEM("system", 0xFFFF6B81);

        private final String id;
        private final int color;

        Category(String id, int color) {
            this.id = id;
            this.color = color;
        }

        public String getId() {
            return id;
        }

        /** 分类主题色，用于 GUI 高亮。 */
        public int getColor() {
            return color;
        }

        public String getDisplayName() {
            String key = "createplus.category." + id;
            return I18n.exists(key) ? I18n.get(key) : id;
        }
    }

    protected final String name;
    protected final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    protected boolean enabled = false;

    protected CreativePlusFeature(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    // === 设置项 ===

    /** 注册一个设置项，返回它本身以便直接赋值给字段。 */
    protected <T extends Setting<?>> T register(T setting) {
        settings.add(setting);
        return setting;
    }

    protected BooleanSetting boolSetting(String id, boolean def) {
        return register(new BooleanSetting(name, id, def));
    }

    protected DoubleSetting doubleSetting(String id, double def, double min, double max, double step) {
        return register(new DoubleSetting(name, id, def, min, max, step));
    }

    protected IntSetting intSetting(String id, int def, int min, int max) {
        return register(new IntSetting(name, id, def, min, max));
    }

    protected IntSetting intSetting(String id, int def, int min, int max, int step, String suffix) {
        return register(new IntSetting(name, id, def, min, max, step, suffix));
    }

    protected ColorSetting colorSetting(String id, int def) {
        return register(new ColorSetting(name, id, def));
    }

    protected StringSetting stringSetting(String id, String def) {
        return register(new StringSetting(name, id, def));
    }

    protected StringSetting stringSetting(String id, String def, int maxLength) {
        return register(new StringSetting(name, id, def, maxLength));
    }

    protected <E extends Enum<E>> EnumSetting<E> enumSetting(String id, E def) {
        return register(new EnumSetting<>(name, id, def));
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public boolean hasSettings() {
        return !settings.isEmpty();
    }

    public Setting<?> getSetting(String id) {
        for (Setting<?> setting : settings) {
            if (setting.getId().equals(id)) return setting;
        }
        return null;
    }

    // === 本地化 ===

    public String getTranslatedName() {
        String key = "feature.createplus." + name + ".name";
        return I18n.exists(key) ? I18n.get(key) : name;
    }

    public String getTranslatedDescription() {
        String key = "feature.createplus." + name + ".description";
        return I18n.exists(key) ? I18n.get(key) : description;
    }

    /** 描述 + 运行时状态提示，供 GUI 与 /features help 使用。 */
    public String getDetailedDescription() {
        return getTranslatedDescription();
    }

    public String getTranslationKey() {
        return "feature.createplus." + name;
    }

    // === 生命周期 ===

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        try {
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
        } catch (Exception e) {
            // 单个功能出错不应该带崩整个客户端
            org.xiyu.yee.createplus.Createplus.LOGGER.error(
                "CreatePlus feature '{}' failed to {}", name, enabled ? "enable" : "disable", e);
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void onTick() {
    }

    /**
     * 鼠标点击。
     *
     * @param isRightClick true 为右键
     */
    public void handleClick(boolean isRightClick) {
    }

    /**
     * 鼠标滚轮。
     *
     * @param delta 正数向上滚
     * @return true 表示已消费该事件，滚轮不再切换物品栏
     */
    public boolean handleScroll(double delta) {
        return false;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    /**
     * 玩家进入世界时调用（仅对已启用的功能）。
     *
     * 配置是在主菜单阶段加载的，那时玩家实体还不存在，
     * 所以任何需要玩家的初始化都应该放在这里而不是 {@link #onEnable()}。
     */
    public void onWorldJoin() {
    }

    // === 工具方法 ===

    /** 空指针安全的玩家获取。世界未加载时返回 null。 */
    protected static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    /** 发送一条只有自己能看到的聊天提示，玩家不存在时静默跳过。 */
    protected static void notifyPlayer(String message) {
        LocalPlayer player = player();
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
    }

    /** 在物品栏上方显示提示，适合频繁变化的状态。 */
    protected static void notifyActionBar(String message) {
        LocalPlayer player = player();
        if (player != null) {
            player.displayClientMessage(Component.literal(message), true);
        }
    }

    /** 该功能是否处于可工作状态：已启用、世界已加载、且处于创造模式。 */
    protected static boolean inCreative() {
        LocalPlayer player = player();
        return player != null && player.isCreative();
    }
}
