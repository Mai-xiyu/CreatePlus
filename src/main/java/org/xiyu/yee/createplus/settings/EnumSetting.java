package org.xiyu.yee.createplus.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.language.I18n;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] values;
    private final String enumKeyPrefix;

    public EnumSetting(String featureName, String id, E defaultValue) {
        super(featureName, id, defaultValue);
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
        this.enumKeyPrefix = "setting.createplus." + featureName + "." + id + ".";
    }

    public E[] getValues() {
        return values;
    }

    @Override
    public String getDisplayValue() {
        String key = enumKeyPrefix + get().name().toLowerCase(java.util.Locale.ROOT);
        return I18n.exists(key) ? I18n.get(key) : get().name();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get().name());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) return;
        String name = json.getAsString();
        for (E candidate : values) {
            if (candidate.name().equalsIgnoreCase(name)) {
                set(candidate);
                return;
            }
        }
    }

    @Override
    public void cycle(int direction) {
        int next = (get().ordinal() + direction) % values.length;
        if (next < 0) next += values.length;
        set(values[next]);
    }
}
