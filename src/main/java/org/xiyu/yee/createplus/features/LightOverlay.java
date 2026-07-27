package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 光照覆盖：把能刷怪的方块标出来，做大型建筑时用来补光。
 *
 * 1.18 之后的刷怪判定是"方块光照为 0"，而不是老版本的 7 以下，
 * 所以红叉的含义是"任何时候都能刷"，黄叉是"只有夜里/暗处能刷"。
 */
public class LightOverlay extends CreativePlusFeature implements WorldRenderFeature {

    private record Marker(BlockPos pos, boolean always) {
    }

    private final IntSetting radius = intSetting("radius", 12, 4, 32);
    private final IntSetting verticalRange = intSetting("vertical_range", 6, 2, 24);
    private final IntSetting refreshTicks = intSetting("refresh_ticks", 10, 1, 40);
    private final BooleanSetting showNightOnly = boolSetting("show_night_only", true);
    private final ColorSetting alwaysColor = colorSetting("always_color", 0xFFFF4444);
    private final ColorSetting nightColor = colorSetting("night_color", 0xFFFFD24B);

    /** 扫描结果缓存：整片区域每帧重算会直接把帧数打到地板上。 */
    private final List<Marker> markers = new ArrayList<>();
    private int tickCounter;

    public LightOverlay() {
        super("light_overlay", "标记可刷怪的方块，方便补光", Category.RENDER);
    }

    @Override
    public void onEnable() {
        tickCounter = 0;
        markers.clear();
    }

    @Override
    public void onDisable() {
        markers.clear();
    }

    @Override
    public void onTick() {
        if (--tickCounter > 0) return;
        tickCounter = refreshTicks.value();
        rescan();
    }

    private void rescan() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        markers.clear();
        if (player == null || level == null) return;

        int r = radius.value();
        int v = verticalRange.value();
        BlockPos origin = player.blockPosition();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -v; dy <= v; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!isSpawnableSurface(level, pos)) continue;

                    BlockPos above = pos.above();
                    int blockLight = level.getBrightness(LightLayer.BLOCK, above);
                    if (blockLight > 0) continue;

                    boolean always = level.getBrightness(LightLayer.SKY, above) == 0;
                    if (!always && !showNightOnly.get()) continue;
                    markers.add(new Marker(pos, always));
                }
            }
        }
    }

    private static boolean isSpawnableSurface(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (!state.isFaceSturdy(level, pos, Direction.UP)) return false;

        // 需要两格净空，否则怪物放不下
        return level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
            && level.getBlockState(pos.above(2)).getCollisionShape(level, pos.above(2)).isEmpty();
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (markers.isEmpty()) return;

        float[] always = rgb(alwaysColor.argb());
        float[] night = rgb(nightColor.argb());

        try (RenderUtils.LineBatch batch = RenderUtils.beginWorldLines(poseStack, camera)) {
            for (Marker marker : markers) {
                float[] c = marker.always() ? always : night;
                double x = marker.pos().getX();
                double y = marker.pos().getY() + 1.005D;
                double z = marker.pos().getZ();

                batch.line(x, y, z, x + 1, y, z + 1, c[0], c[1], c[2], 0.9F)
                    .line(x + 1, y, z, x, y, z + 1, c[0], c[1], c[2], 0.9F);
            }
        }
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
        return getTranslatedDescription()
            + "\n§c红叉§7: 任何时候都能刷怪  §e黄叉§7: 仅夜间"
            + "\n§7扫描范围: §f" + radius.value() + " §7格，上下 §f" + verticalRange.value()
            + "\n§7当前标记: §f" + markers.size() + " §7处";
    }
}
