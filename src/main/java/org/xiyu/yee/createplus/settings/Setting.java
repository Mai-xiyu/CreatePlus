package org.xiyu.yee.createplus.settings;

import com.google.gson.JsonElement;
import net.minecraft.client.resources.language.I18n;

import java.util.function.Consumer;

/**
 * 功能设置项的基类。
 *
 * 所有设置项都能自我描述（名称/当前值）、自我序列化，
 * 因此 ConfigManager 与各个 GUI 都不需要为每个功能写专门的代码。
 */
public abstract class Setting<T> {
    private final String id;
    private final String translationKey;
    protected final T defaultValue;
    protected T value;
    private Consumer<T> listener;

    protected Setting(String featureName, String id, T defaultValue) {
        this.id = id;
        this.translationKey = "setting.createplus." + featureName + "." + id;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getId() {
        return id;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        if (newValue == null || newValue.equals(this.value)) return;
        this.value = newValue;
        if (listener != null) listener.accept(newValue);
    }

    public T getDefault() {
        return defaultValue;
    }

    public void reset() {
        set(defaultValue);
    }

    /** 注册值变化回调，返回自身以便链式调用。 */
    public Setting<T> onChange(Consumer<T> listener) {
        this.listener = listener;
        return this;
    }

    public String getDisplayName() {
        return I18n.exists(translationKey) ? I18n.get(translationKey) : id;
    }

    public String getTooltip() {
        String key = translationKey + ".tooltip";
        return I18n.exists(key) ? I18n.get(key) : null;
    }

    /** 在 GUI 中显示的当前值。 */
    public abstract String getDisplayValue();

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement json);

    /**
     * 键盘左右方向键 / 鼠标滚轮调整。
     *
     * @param direction +1 增加，-1 减少
     */
    public abstract void cycle(int direction);
}
