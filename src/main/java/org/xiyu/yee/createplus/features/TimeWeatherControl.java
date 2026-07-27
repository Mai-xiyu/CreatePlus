package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.settings.IntSetting;

/**
 * 本地时间与天气覆盖。
 *
 * 全部只作用于客户端渲染，不会真的改服务器 —— 用来给建筑拍照选光线，
 * 或者在雨天里安静地施工。
 */
public class TimeWeatherControl extends CreativePlusFeature {

    public enum Weather {
        /** 不干预。 */
        UNCHANGED,
        CLEAR,
        RAIN,
        THUNDER
    }

    /** 常用时刻，和原版 /time set 的取值一致。 */
    public static final int SUNRISE = 23000;
    public static final int DAY = 1000;
    public static final int NOON = 6000;
    public static final int SUNSET = 12000;
    public static final int NIGHT = 13000;
    public static final int MIDNIGHT = 18000;

    private final BooleanSetting freezeTime = boolSetting("freeze_time", true);
    private final IntSetting time = intSetting("time", NOON, 0, 23999, 100, "");
    private final EnumSetting<Weather> weather = enumSetting("weather", Weather.CLEAR);

    public TimeWeatherControl() {
        super("time_weather", "本地覆盖时间与天气（仅客户端可见）", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        if (freezeTime.get()) {
            // 保留天数，只改一天之内的时刻，否则月相和统计会乱掉
            long day = level.getDayTime() / 24000L;
            level.setDayTime(day * 24000L + time.value());
        }

        switch (weather.get()) {
            case CLEAR -> {
                level.setRainLevel(0.0F);
                level.setThunderLevel(0.0F);
            }
            case RAIN -> {
                level.setRainLevel(1.0F);
                level.setThunderLevel(0.0F);
            }
            case THUNDER -> {
                level.setRainLevel(1.0F);
                level.setThunderLevel(1.0F);
            }
            case UNCHANGED -> {
            }
        }
    }

    public void setTime(int ticks) {
        freezeTime.set(true);
        time.set(Math.floorMod(ticks, 24000));
    }

    public void setWeather(Weather value) {
        weather.set(value);
    }

    public int getTime() {
        return time.value();
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7时间: §f" + (freezeTime.get() ? time.value() + " 刻" : "不干预")
            + "\n§7天气: §f" + weather.getDisplayValue()
            + "\n§7/createplus time <预设|刻数> §8· §7/createplus weather <类型>";
    }
}
