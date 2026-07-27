package org.xiyu.yee.createplus.features;

import org.xiyu.yee.createplus.settings.IntSetting;

/**
 * 移除方块放置冷却。
 *
 * 实际生效逻辑在 MinecraftMixin 里，那边每 tick 会把 rightClickDelay 压到设定值。
 */
public class SpeedBuild extends CreativePlusFeature {
    /** 原版是 4 tick，0 表示完全没有冷却。 */
    private final IntSetting delay = intSetting("delay", 0, 0, 4);

    public SpeedBuild() {
        super("speed_build", "移除方块放置冷却，实现快速建造", Category.BUILDING);
    }

    public int getDelay() {
        return delay.value();
    }
}
