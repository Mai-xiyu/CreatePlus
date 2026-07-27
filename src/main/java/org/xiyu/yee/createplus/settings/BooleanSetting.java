package org.xiyu.yee.createplus.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.language.I18n;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String featureName, String id, boolean defaultValue) {
        super(featureName, id, defaultValue);
    }

    @Override
    public String getDisplayValue() {
        String key = get() ? "createplus.state.on" : "createplus.state.off";
        return I18n.exists(key) ? I18n.get(key) : (get() ? "ON" : "OFF");
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            set(json.getAsBoolean());
        }
    }

    @Override
    public void cycle(int direction) {
        set(!get());
    }
}
