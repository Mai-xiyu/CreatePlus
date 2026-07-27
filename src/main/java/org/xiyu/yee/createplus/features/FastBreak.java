package org.xiyu.yee.createplus.features;

import org.xiyu.yee.createplus.api.FastBreakAPI;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 创造瞬间破坏：去掉原版创造里偶发的破坏延迟手感。
 */
public class FastBreak extends CreativePlusFeature {

    public FastBreak() {
        super("fast_break", "创造模式瞬间挖掉方块（需创造）", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        FastBreakAPI.setActive(CreativeGate.allow());
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7快速破坏只在创造模式下生效");
        }
    }

    @Override
    public void onDisable() {
        FastBreakAPI.setActive(false);
    }

    @Override
    public void onTick() {
        FastBreakAPI.setActive(isEnabled() && CreativeGate.allow());
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
