package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.settings.DoubleSetting;
import org.xiyu.yee.createplus.settings.IntSetting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用一组方块按连续噪声混合上色，而不是只刷单一材质。
 *
 * 大面积单一石砖看着就是"填出来的"。真人建筑师会往里掺一点苔石、裂石砖、
 * 安山岩，而且是成片掺不是随机撒盐 —— 这里用三维噪声取值，相邻方块的噪声
 * 值接近，同种材质自然连成一块块斑块。
 *
 * 调色板直接从快捷栏抓：同一种方块占几格就有几份权重，想让石砖多一点
 * 就多放两格，比记住 {@code 60%stone,40%cobble} 这种写法直观得多。
 */
public class PaintPalette extends CreativePlusFeature {

    /** 噪声只用两个倍频：再多在方块尺度上已经看不出区别，纯粹浪费。 */
    private static final double OCTAVE_SCALE = 2.3D;
    private static final double OCTAVE_WEIGHT = 0.35D;

    private final DoubleSetting patchSize = doubleSetting("patch_size", 5.0D, 1.0D, 32.0D, 0.5D);
    private final IntSetting seed = intSetting("seed", 1, 1, 9999);

    /** 展开后的调色板：一个方块占几格快捷栏，这里就出现几次。 */
    private final List<BlockState> entries = new ArrayList<>();

    public PaintPalette() {
        super("paint_palette", "用快捷栏里的多种方块按噪声混合上色，替代单一材质", Category.BUILDING);
    }

    @Override
    public void onDisable() {
        entries.clear();
    }

    /** 当前生效的调色板；没开启或没抓取过就返回 null，调用方退回单一方块。 */
    public static PaintPalette active() {
        PaintPalette palette = Createplus.FEATURE_MANAGER.get(PaintPalette.class);
        return palette != null && palette.isEnabled() && !palette.entries.isEmpty() ? palette : null;
    }

    /**
     * 给某个位置挑一种方块。
     *
     * @param fallback 调色板为空时用的方块
     */
    public BlockState pick(BlockPos pos, BlockState fallback) {
        if (entries.isEmpty()) return fallback;
        if (entries.size() == 1) return entries.get(0);

        double scale = patchSize.value();
        double n = fractal(pos.getX() / scale, pos.getY() / scale, pos.getZ() / scale);
        // 三线性插值的结果向 0.5 聚拢，不拉伸的话调色板首尾两种方块几乎不会出现
        n = Mth.clamp((n - 0.5D) * 1.9D + 0.5D, 0.0D, 0.99999D);
        return entries.get((int) (n * entries.size()));
    }

    /** 从快捷栏抓取调色板，返回抓到的格数。 */
    public int grabFromHotbar() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;

        entries.clear();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof BlockItem item) {
                entries.add(item.getBlock().defaultBlockState());
            }
        }
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    /** 每种方块各占多少格，用于展示权重。 */
    public Map<String, Integer> weights() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (BlockState state : entries) {
            counts.merge(state.getBlock().getName().getString(), 1, Integer::sum);
        }
        return counts;
    }

    // ==================================================================
    // 三维值噪声
    // ==================================================================

    private double fractal(double x, double y, double z) {
        double base = valueNoise(x, y, z);
        double detail = valueNoise(x * OCTAVE_SCALE, y * OCTAVE_SCALE, z * OCTAVE_SCALE);
        return base * (1.0D - OCTAVE_WEIGHT) + detail * OCTAVE_WEIGHT;
    }

    private double valueNoise(double x, double y, double z) {
        int xi = Mth.floor(x), yi = Mth.floor(y), zi = Mth.floor(z);
        double xf = fade(x - xi), yf = fade(y - yi), zf = fade(z - zi);

        double x00 = lerp(hash(xi, yi, zi), hash(xi + 1, yi, zi), xf);
        double x10 = lerp(hash(xi, yi + 1, zi), hash(xi + 1, yi + 1, zi), xf);
        double x01 = lerp(hash(xi, yi, zi + 1), hash(xi + 1, yi, zi + 1), xf);
        double x11 = lerp(hash(xi, yi + 1, zi + 1), hash(xi + 1, yi + 1, zi + 1), xf);

        return lerp(lerp(x00, x10, yf), lerp(x01, x11, yf), zf);
    }

    /** 五次平滑，一阶二阶导都连续，斑块边缘不会出现方向性条纹。 */
    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private double hash(int x, int y, int z) {
        long h = seed.value() * 0x9E3779B97F4A7C15L;
        h ^= x * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= y * 0x165667B19E3779F9L;
        h ^= h >>> 32;
        h *= 0x94D049BB133111EBL;
        h ^= z * 0x27D4EB2F165667C5L;
        h ^= h >>> 31;
        return (h >>> 11) / (double) (1L << 53);
    }

    @Override
    public String getDetailedDescription() {
        StringBuilder text = new StringBuilder(getTranslatedDescription());
        text.append("\n§7/createplus palette grab §8· §7clear §8· §7list");
        if (entries.isEmpty()) {
            text.append("\n§7调色板: 空（把要混的方块摆进快捷栏再 grab）");
        } else {
            text.append("\n§7调色板: §f").append(entries.size()).append(" §7份");
            weights().forEach((name, count) ->
                text.append("\n  §f").append(name).append(" §8×").append(count));
        }
        text.append("\n§7斑块尺寸: §f").append(patchSize.getDisplayValue());
        text.append("\n§7对区域填充、几何体工坊、样条曲线同时生效");
        return text.toString();
    }
}
