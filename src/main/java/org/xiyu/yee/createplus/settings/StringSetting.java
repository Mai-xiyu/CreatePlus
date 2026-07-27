package org.xiyu.yee.createplus.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** 自由文本设置（API 地址、密钥等）。GUI 里通过编辑框修改，cycle 为空操作。 */
public class StringSetting extends Setting<String> {
    private final int maxLength;

    public StringSetting(String featureName, String id, String defaultValue) {
        this(featureName, id, defaultValue, 512);
    }

    public StringSetting(String featureName, String id, String defaultValue, int maxLength) {
        super(featureName, id, defaultValue == null ? "" : defaultValue);
        this.maxLength = Math.max(1, maxLength);
    }

    @Override
    public void set(String newValue) {
        if (newValue == null) newValue = "";
        if (newValue.length() > maxLength) {
            newValue = newValue.substring(0, maxLength);
        }
        super.set(newValue);
    }

    @Override
    public String getDisplayValue() {
        String v = get();
        if (v.isEmpty()) return "—";
        if (getId().contains("key") || getId().contains("secret") || getId().contains("token")) {
            return v.length() <= 4 ? "****" : "****" + v.substring(v.length() - 4);
        }
        return v.length() > 18 ? v.substring(0, 15) + "…" : v;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            set(json.getAsString());
        }
    }

    @Override
    public void cycle(int direction) {
        // 文本设置不适合滚轮切换
    }

    public int getMaxLength() {
        return maxLength;
    }
}
