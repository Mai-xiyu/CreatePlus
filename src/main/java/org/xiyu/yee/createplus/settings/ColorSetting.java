package org.xiyu.yee.createplus.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * ARGB 颜色设置项。
 *
 * 序列化为 "#AARRGGBB" 字符串，方便手动编辑配置文件。
 */
public class ColorSetting extends Setting<Integer> {
    /** 预设色板，用左右方向键在其中循环。 */
    private static final int[] PALETTE = {
        0xFFFFFFFF, 0xFFAAAAAA, 0xFF555555, 0xFF000000,
        0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55,
        0xFF00A2FF, 0xFF5555FF, 0xFFFF55FF, 0xFF00FFFF
    };

    public ColorSetting(String featureName, String id, int defaultArgb) {
        super(featureName, id, defaultArgb);
    }

    public int argb() {
        return get();
    }

    @Override
    public void set(Integer newValue) {
        if (newValue == null) return;
        // 没有 alpha 通道的颜色一律视为完全不透明，避免出现"看不见的文字"
        int argb = newValue;
        if ((argb & 0xFF000000) == 0) {
            argb |= 0xFF000000;
        }
        super.set(argb);
    }

    @Override
    public String getDisplayValue() {
        return String.format("#%06X", get() & 0xFFFFFF);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(String.format("#%08X", get()));
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) return;
        try {
            String raw = json.getAsString().trim();
            if (raw.startsWith("#")) {
                set((int) Long.parseLong(raw.substring(1), 16));
            } else {
                set(json.getAsInt());
            }
        } catch (NumberFormatException | UnsupportedOperationException ignored) {
            // 配置里写了非法颜色，保留默认值
        }
    }

    @Override
    public void cycle(int direction) {
        int current = -1;
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == get()) {
                current = i;
                break;
            }
        }
        int next = (current + direction) % PALETTE.length;
        if (next < 0) next += PALETTE.length;
        set(PALETTE[next]);
    }
}
