package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import org.xiyu.yee.createplus.ui.mapart.MapArtScreen;

/**
 * 地图画（filled_map）+ 方块像素画（全局可见备用）共用入口。
 */
public class MapArt extends CreativePlusFeature {
    public MapArt() {
        super("map_art", "地图画 / 方块像素画：选图生成，可写入快捷栏或放置到世界", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.execute(() -> {
                mc.setScreen(new MapArtScreen());
                setEnabled(false);
            });
        }
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7地图画：高清 filled_map，单人/房主他人可见；非房主联机仅自己可见"
            + "\n§7方块像素画：混凝土等拼图，原版全局可见（备用）"
            + "\n§7方块画可竖放或平铺地板/天花板；尺寸按面积上限放大，尽量 1:1"
            + "\n§7界面顶部可切换两种模式";
    }
}
