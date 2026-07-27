package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import org.xiyu.yee.createplus.ui.mapart.MapArtScreen;

/**
 * 图片 → 地图画 → 写入创造模式「已保存的快捷栏」。
 */
public class MapArt extends CreativePlusFeature {
    public MapArt() {
        super("map_art", "选择图片生成地图画，写入已保存的快捷栏（单人/多人客户端均可）", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.execute(() -> {
                mc.setScreen(new MapArtScreen());
                // 界面用完即关功能，避免一直保持开启
                setEnabled(false);
            });
        }
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7每张 128×128 像素；几乘几=墙面张数；等级 0~4=原版覆盖边长 128×2^n"
            + "\n§7可生成单张、清空已保存快捷栏地图画、自动放置墙+展示框+地图"
            + "\n§7多人客户端：像素本机可见；自动放置需创造/OP";
    }
}
