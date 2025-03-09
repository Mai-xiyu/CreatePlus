package org.xiyu.yee.createplus.features;

import net.minecraft.network.chat.Component;
import org.xiyu.yee.createplus.Createplus;

public abstract class CreativePlusFeature {
    private final String name;
    private final String translationKey;
    private final String descriptionKey;
    private boolean enabled = false;

    public CreativePlusFeature(String baseName, String baseDesc) {
        this.name = baseName.toLowerCase().replace(' ', '_');
        String baseKey = "feature." + Createplus.MODID + "." + this.name;
        
        this.translationKey = baseKey + ".name";
        this.descriptionKey = baseKey + ".description";
    }

    public abstract void onEnable();
    public abstract void onDisable();
    public abstract void onTick();

    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
        // 通知FeatureManager保存配置
        Createplus.FEATURE_MANAGER.onFeatureToggle();
    }

    public void enable() {
        enabled = true;
        onEnable();
    }

    public void disable() {
        enabled = false;
        onDisable();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return Component.translatable(translationKey).getString();
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public String getDescription() {
        return Component.translatable(descriptionKey).getString();
    }

    public void handleClick(boolean isRightClick) {
        // 默认空实现，子类可以重写
    }

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
} 