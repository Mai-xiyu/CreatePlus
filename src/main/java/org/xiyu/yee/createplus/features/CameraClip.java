package org.xiyu.yee.createplus.features;

import org.xiyu.yee.createplus.api.CameraClipAPI;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 镜头穿墙：第三人称相机不再被方块挡住（和玩家穿墙分开）。
 */
public class CameraClip extends CreativePlusFeature {

    public CameraClip() {
        super("camera_clip", "第三人称镜头穿过方块（需创造）", Category.RENDER);
    }

    @Override
    public void onEnable() {
        CameraClipAPI.setActive(CreativeGate.allow());
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7镜头穿墙只在创造模式下生效");
        }
    }

    @Override
    public void onDisable() {
        CameraClipAPI.setActive(false);
    }

    @Override
    public void onTick() {
        CameraClipAPI.setActive(isEnabled() && CreativeGate.allow());
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
