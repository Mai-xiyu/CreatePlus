package org.xiyu.yee.createplus.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class IntSetting extends Setting<Integer> implements Slidable {
    private final int min;
    private final int max;
    private final int step;
    private final String suffix;

    public IntSetting(String featureName, String id, int defaultValue, int min, int max) {
        this(featureName, id, defaultValue, min, max, 1, "");
    }

    public IntSetting(String featureName, String id, int defaultValue, int min, int max, int step, String suffix) {
        super(featureName, id, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
    }

    public int value() {
        return get();
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public void set(Integer newValue) {
        if (newValue == null) return;
        super.set(Math.max(min, Math.min(max, newValue)));
    }

    @Override
    public String getDisplayValue() {
        return get() + suffix;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            set(json.getAsInt());
        }
    }

    @Override
    public void cycle(int direction) {
        set(get() + step * direction);
    }

    @Override
    public double getFraction() {
        if (max - min <= 0) return 0.0D;
        return (double) (get() - min) / (double) (max - min);
    }

    @Override
    public void setFraction(double fraction) {
        double f = Math.max(0.0D, Math.min(1.0D, fraction));
        set((int) Math.round(min + (max - min) * f));
    }
}
