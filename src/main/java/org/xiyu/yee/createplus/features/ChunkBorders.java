package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ChunkPos;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.utils.RenderUtils;

/**
 * 区块边界显示，MiniHUD 的招牌功能之一。
 *
 * 对红石与刷怪相关的建筑来说，知道区块边界在哪基本是刚需。
 */
public class ChunkBorders extends CreativePlusFeature implements WorldRenderFeature {

    private final IntSetting radius = intSetting("radius", 2, 0, 8);
    private final BooleanSetting verticalLines = boolSetting("vertical_lines", true);
    private final BooleanSetting subChunks = boolSetting("sub_chunks", false);
    private final BooleanSetting grid = boolSetting("grid", false);
    private final ColorSetting color = colorSetting("color", 0xFF00FFFF);
    private final ColorSetting subColor = colorSetting("sub_color", 0xFF808080);

    public ChunkBorders() {
        super("chunk_borders", "显示区块边界、子区块与 4x4 网格", Category.RENDER);
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        ChunkPos center = player.chunkPosition();
        int minY = mc.level.getMinBuildHeight();
        int maxY = mc.level.getMaxBuildHeight();
        int r = radius.value();
        double y = player.getY();

        float[] main = rgb(color.argb());
        float[] sub = rgb(subColor.argb());

        try (RenderUtils.LineBatch batch = RenderUtils.beginWorldLines(poseStack, camera)) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int x = (center.x + dx) << 4;
                    int z = (center.z + dz) << 4;

                    if (verticalLines.get()) {
                        batch.line(x, minY, z, x, maxY, z, main[0], main[1], main[2], 0.6F);
                        batch.line(x + 16, minY, z, x + 16, maxY, z, main[0], main[1], main[2], 0.6F);
                        batch.line(x, minY, z + 16, x, maxY, z + 16, main[0], main[1], main[2], 0.6F);
                        batch.line(x + 16, minY, z + 16, x + 16, maxY, z + 16, main[0], main[1], main[2], 0.6F);
                    }

                    // 玩家所在高度画一圈轮廓，飞在高处也能一眼看清网格
                    rect(batch, x, y, z, main, 0.9F);

                    if (grid.get()) {
                        for (int i = 4; i < 16; i += 4) {
                            batch.line(x + i, y, z, x + i, y, z + 16, sub[0], sub[1], sub[2], 0.35F);
                            batch.line(x, y, z + i, x + 16, y, z + i, sub[0], sub[1], sub[2], 0.35F);
                        }
                    }
                }
            }

            if (subChunks.get()) {
                int x = center.x << 4;
                int z = center.z << 4;
                for (int layer = minY; layer <= maxY; layer += 16) {
                    rect(batch, x, layer, z, sub, 0.5F);
                }
            }
        }
    }

    private static void rect(RenderUtils.LineBatch batch, int x, double y, int z, float[] c, float alpha) {
        batch.line(x, y, z, x + 16, y, z, c[0], c[1], c[2], alpha)
            .line(x + 16, y, z, x + 16, y, z + 16, c[0], c[1], c[2], alpha)
            .line(x + 16, y, z + 16, x, y, z + 16, c[0], c[1], c[2], alpha)
            .line(x, y, z + 16, x, y, z, c[0], c[1], c[2], alpha);
    }

    private static float[] rgb(int argb) {
        return new float[]{
            ((argb >> 16) & 0xFF) / 255.0F,
            ((argb >> 8) & 0xFF) / 255.0F,
            (argb & 0xFF) / 255.0F
        };
    }

    @Override
    public String getDetailedDescription() {
        Minecraft mc = Minecraft.getInstance();
        String pos = "-";
        if (mc.player != null) {
            pos = (mc.player.getBlockX() & 15) + ", " + (mc.player.getBlockZ() & 15);
        }
        return getTranslatedDescription()
            + "\n§7半径: §f" + radius.value() + " §7个区块"
            + "\n§7你在区块内的位置: §f" + pos;
    }
}
