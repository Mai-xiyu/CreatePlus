package org.xiyu.yee.createplus.features;

import net.minecraft.client.resources.language.I18n;

public abstract class CreativePlusFeature {
    protected final String name;
    protected final String description;
    protected boolean enabled = false;

    public CreativePlusFeature(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    // 获取翻译后的名字
    public String getTranslatedName() {
        String key = "feature.createplus." + this.name.toLowerCase().replace(" ", "_") + ".name";
        if (I18n.exists(key)) {
            return I18n.get(key);
        }
        return this.name;
    }

    // 获取翻译后的描述
    public String getTranslatedDescription() {
        String key = "feature.createplus." + this.name.toLowerCase().replace(" ", "_") + ".description";
        if (I18n.exists(key)) {
            return I18n.get(key);
        }
        return this.description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // === 修复 1: 补回 setEnabled 方法 ===
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    // === 修复 2: 补回 onTick 方法 (供子类重写) ===
    public void onTick() {
        // 默认空实现
    }

    // === 修复 3: 补回 handleClick 方法 (供子类重写) ===
    public void handleClick(boolean isRightClick) {
        // 默认空实现
    }

    public void onEnable() {
        // 默认空实现
    }

    public void onDisable() {
        // 默认空实现
    }

    public String getTranslationKey() {
        return "feature.createplus." + this.name.toLowerCase().replace(" ", "_");
    }
}