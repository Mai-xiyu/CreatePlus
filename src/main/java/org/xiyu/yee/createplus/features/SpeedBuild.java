package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;

public class SpeedBuild extends CreativePlusFeature {
    public SpeedBuild() {
        super("speed_build", "移除放置冷却");
    }

    @Override
    public void onTick() {
        // 不需要在这里做任何事情，Mixin会处理
    }

    @Override
    public void onEnable() {
        // 不需要特殊初始化
    }

    @Override
    public void onDisable() {
        // 不需要特殊清理
    }
} 