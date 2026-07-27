package org.xiyu.yee.createplus.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class DoubleSetting extends Setting<Double> implements Slidable {
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;

    public DoubleSetting(String featureName, String id, double defaultValue, double min, double max, double step) {
        this(featureName, id, defaultValue, min, max, step, "");
    }

    public DoubleSetting(String featureName, String id, double defaultValue,
                         double min, double max, double step, String suffix) {
        super(featureName, id, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    /** 便捷访问器，避免到处拆箱。 */
    public double value() {
        return get();
    }

    public float floatValue() {
        return get().floatValue();
    }

    @Override
    public void set(Double newValue) {
        if (newValue == null) return;
        double clamped = Math.max(min, Math.min(max, newValue));
        if (step > 0.0D) {
            // 以 min 为基准对齐步长：直接对 0 取整的话，min 不是步长整数倍时
            // （例如 min=0.1、step=0.3）永远调不到最小值
            clamped = min + Math.round((clamped - min) / step) * step;
            // 0.1 这类步长累加会留下 0.7000000000000001，抹掉尾数
            clamped = Math.round(clamped * 1.0E6D) / 1.0E6D;
            clamped = Math.max(min, Math.min(max, clamped));
        }
        super.set(clamped);
    }

    @Override
    public String getDisplayValue() {
        return trim(get()) + suffix;
    }

    private static String trim(double v) {
        if (Math.abs(v - Math.rint(v)) < 1.0E-4D) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format("%.2f", v);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive()) {
            set(json.getAsDouble());
        }
    }

    @Override
    public void cycle(int direction) {
        set(get() + step * direction);
    }

    @Override
    public double getFraction() {
        if (max - min <= 0.0D) return 0.0D;
        return (get() - min) / (max - min);
    }

    @Override
    public void setFraction(double fraction) {
        double f = Math.max(0.0D, Math.min(1.0D, fraction));
        set(min + (max - min) * f);
    }
}
