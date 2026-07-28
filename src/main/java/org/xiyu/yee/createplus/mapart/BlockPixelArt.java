package org.xiyu.yee.createplus.mapart;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 方块像素画：把图片量化成混凝土/陶瓦等实心方块，放置后原版全局可见。
 * <p>
 * 单边上限 {@link #MAX_SIDE}，总格数上限 {@link #MAX_AREA}（像素画可到 512×512）；
 * 小图尽量 1 像素 = 1 格，避免强制压成马赛克。
 */
public final class BlockPixelArt {
    public static final int MAX_SIDE = 512;
    /** 像素画专属体积上限（可 512×512）；不绑普通填充的 32768。 */
    public static final int MAX_AREA = MAX_SIDE * MAX_SIDE;
    /** @deprecated 用 {@link #MAX_SIDE}；保留以免外部旧引用编译失败 */
    @Deprecated
    public static final int MAX_SIZE = MAX_SIDE;

    public enum Palette {
        CONCRETE("混凝土"),
        TERRACOTTA("陶瓦"),
        WOOL("羊毛"),
        MIXED("混合(更准)");

        public final String label;

        Palette(String label) {
            this.label = label;
        }
    }

    public record Probe(int width, int height, int suggestW, int suggestH) {
        public String summary() {
            boolean nativeRes = suggestW == width && suggestH == height;
            return width + "×" + height + " → 建议 " + suggestW + "×" + suggestH + " 格"
                + (nativeRes ? "（1:1 原图像素）" : "（按比例缩进面积上限）");
        }
    }

    public record Grid(int width, int height, BlockState[] cells) {
        public BlockState get(int x, int y) {
            return cells[x + y * width];
        }
    }

    private record Swatch(BlockState state, float r, float g, float b, float L, float a, float bLab) {
    }

    private BlockPixelArt() {
    }

    public static boolean fits(int w, int h) {
        return w >= 1 && h >= 1 && w <= MAX_SIDE && h <= MAX_SIDE && (long) w * h <= MAX_AREA;
    }

    /** 把宽高压进面积/单边预算，尽量保持比例。 */
    public static int[] clampToBudget(int w, int h) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        if (fits(w, h)) return new int[]{w, h};
        double aspect = w / (double) h;
        double scale = Math.min(MAX_SIDE / (double) w, MAX_SIDE / (double) h);
        scale = Math.min(scale, Math.sqrt(MAX_AREA / (double) ((long) w * h)));
        int nw = Math.max(1, (int) Math.floor(w * scale));
        int nh = Math.max(1, (int) Math.floor(h * scale));
        nw = clamp(nw, 1, MAX_SIDE);
        nh = clamp(nh, 1, MAX_SIDE);
        while ((long) nw * nh > MAX_AREA) {
            if (nw / aspect >= nh) nw--;
            else nh--;
            nw = Math.max(1, nw);
            nh = Math.max(1, nh);
        }
        // 微调比例
        int altH = clamp((int) Math.round(nw / aspect), 1, MAX_SIDE);
        if (fits(nw, altH)) nh = altH;
        int altW = clamp((int) Math.round(nh * aspect), 1, MAX_SIDE);
        if (fits(altW, nh) && Math.abs(altW / (double) nh - aspect) < Math.abs(nw / (double) nh - aspect)) {
            nw = altW;
        }
        return new int[]{nw, nh};
    }

    /**
     * 优先 1 像素 = 1 格；装不下再按比例缩进 {@link #MAX_AREA}/{@link #MAX_SIDE}。
     */
    public static Probe probe(BufferedImage src) {
        int w = Math.max(1, src.getWidth());
        int h = Math.max(1, src.getHeight());
        double aspect = w / (double) h;

        int[] seed = clampToBudget(w, h);
        int bestW = seed[0];
        int bestH = seed[1];
        double best = score(aspect, bestW, bestH, w, h);

        // 只在种子附近微调，避免扫 512²
        int radius = 48;
        for (int dw = -radius; dw <= radius; dw++) {
            int cw = bestW + dw;
            if (cw < 1 || cw > MAX_SIDE) continue;
            for (int dh = -2; dh <= 2; dh++) {
                int ch = clamp((int) Math.round(cw / aspect) + dh, 1, MAX_SIDE);
                if (!fits(cw, ch)) continue;
                double s = score(aspect, cw, ch, w, h);
                if (s < best) {
                    best = s;
                    bestW = cw;
                    bestH = ch;
                }
            }
        }
        for (int dh = -radius; dh <= radius; dh++) {
            int ch = bestH + dh;
            if (ch < 1 || ch > MAX_SIDE) continue;
            for (int dw = -2; dw <= 2; dw++) {
                int cw = clamp((int) Math.round(ch * aspect) + dw, 1, MAX_SIDE);
                if (!fits(cw, ch)) continue;
                double s = score(aspect, cw, ch, w, h);
                if (s < best) {
                    best = s;
                    bestW = cw;
                    bestH = ch;
                }
            }
        }
        return new Probe(w, h, bestW, bestH);
    }

    private static double score(double aspect, int cw, int ch, int imgW, int imgH) {
        double gridAspect = cw / (double) ch;
        double aspectPenalty = Math.abs(Math.log(gridAspect / aspect));
        double scaleX = imgW / (double) cw;
        double scaleY = imgH / (double) ch;
        double scale = Math.max(scaleX, scaleY);
        // 强烈偏好接近 1:1，缩小越狠越像马赛克
        double scalePenalty = Math.abs(Math.log(Math.max(scale, 1e-6)));
        if (scale > 1.0) scalePenalty *= 1.8;
        return aspectPenalty * 2.0 + scalePenalty;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static Grid quantize(BufferedImage src, int tw, int th, Palette palette, boolean dither) {
        int[] sized = clampToBudget(tw, th);
        tw = sized[0];
        th = sized[1];
        Swatch[] swatches = buildPalette(palette);
        BufferedImage scaled = scale(src, tw, th);

        float[][] r = new float[th][tw];
        float[][] g = new float[th][tw];
        float[][] b = new float[th][tw];
        boolean[][] opaque = new boolean[th][tw];
        for (int y = 0; y < th; y++) {
            for (int x = 0; x < tw; x++) {
                int argb = scaled.getRGB(x, y);
                if (((argb >>> 24) & 0xFF) < 16) {
                    opaque[y][x] = false;
                    continue;
                }
                opaque[y][x] = true;
                r[y][x] = (argb >> 16) & 0xFF;
                g[y][x] = (argb >> 8) & 0xFF;
                b[y][x] = argb & 0xFF;
            }
        }

        BlockState[] cells = new BlockState[tw * th];
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int y = 0; y < th; y++) {
            for (int x = 0; x < tw; x++) {
                if (!opaque[y][x]) {
                    cells[x + y * tw] = air;
                    continue;
                }
                float rr = clampF(r[y][x], 0, 255);
                float gg = clampF(g[y][x], 0, 255);
                float bb = clampF(b[y][x], 0, 255);
                Swatch best = nearest(swatches, rr, gg, bb);
                cells[x + y * tw] = best.state();
                if (!dither) continue;
                float er = rr - best.r();
                float eg = gg - best.g();
                float eb = bb - best.b();
                diffuse(r, g, b, opaque, x + 1, y, er, eg, eb, 7f / 16f, tw, th);
                diffuse(r, g, b, opaque, x - 1, y + 1, er, eg, eb, 3f / 16f, tw, th);
                diffuse(r, g, b, opaque, x, y + 1, er, eg, eb, 5f / 16f, tw, th);
                diffuse(r, g, b, opaque, x + 1, y + 1, er, eg, eb, 1f / 16f, tw, th);
            }
        }
        return new Grid(tw, th, cells);
    }

    private static Swatch nearest(Swatch[] swatches, float r, float g, float b) {
        float[] lab = rgbToLab(r, g, b);
        Swatch best = swatches[0];
        float bestDist = Float.MAX_VALUE;
        for (Swatch s : swatches) {
            float dL = lab[0] - s.L();
            float da = lab[1] - s.a();
            float db = lab[2] - s.bLab();
            float dist = 1.2f * dL * dL + da * da + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = s;
            }
        }
        return best;
    }

    private static Swatch[] buildPalette(Palette palette) {
        List<Block> blocks = new ArrayList<>();
        switch (palette) {
            case CONCRETE -> addConcrete(blocks);
            case TERRACOTTA -> addTerracotta(blocks);
            case WOOL -> addWool(blocks);
            case MIXED -> {
                addConcrete(blocks);
                addTerracotta(blocks);
                addWool(blocks);
            }
        }
        List<Swatch> list = new ArrayList<>(blocks.size());
        for (Block block : blocks) {
            int rgb = approximateBlockRgb(block);
            float r = (rgb >> 16) & 0xFF;
            float g = (rgb >> 8) & 0xFF;
            float b = rgb & 0xFF;
            float[] lab = rgbToLab(r, g, b);
            list.add(new Swatch(block.defaultBlockState(), r, g, b, lab[0], lab[1], lab[2]));
        }
        return list.toArray(Swatch[]::new);
    }

    /** 用方块的地图色估算显示色（与地图渲染同套 ABGR 规则）。 */
    private static int approximateBlockRgb(Block block) {
        var mc = block.defaultMapColor();
        int abgr = mc.calculateRGBColor(net.minecraft.world.level.material.MapColor.Brightness.NORMAL);
        int r = abgr & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int b = (abgr >> 16) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    private static void addConcrete(List<Block> blocks) {
        blocks.add(Blocks.WHITE_CONCRETE);
        blocks.add(Blocks.ORANGE_CONCRETE);
        blocks.add(Blocks.MAGENTA_CONCRETE);
        blocks.add(Blocks.LIGHT_BLUE_CONCRETE);
        blocks.add(Blocks.YELLOW_CONCRETE);
        blocks.add(Blocks.LIME_CONCRETE);
        blocks.add(Blocks.PINK_CONCRETE);
        blocks.add(Blocks.GRAY_CONCRETE);
        blocks.add(Blocks.LIGHT_GRAY_CONCRETE);
        blocks.add(Blocks.CYAN_CONCRETE);
        blocks.add(Blocks.PURPLE_CONCRETE);
        blocks.add(Blocks.BLUE_CONCRETE);
        blocks.add(Blocks.BROWN_CONCRETE);
        blocks.add(Blocks.GREEN_CONCRETE);
        blocks.add(Blocks.RED_CONCRETE);
        blocks.add(Blocks.BLACK_CONCRETE);
    }

    private static void addTerracotta(List<Block> blocks) {
        blocks.add(Blocks.TERRACOTTA);
        blocks.add(Blocks.WHITE_TERRACOTTA);
        blocks.add(Blocks.ORANGE_TERRACOTTA);
        blocks.add(Blocks.MAGENTA_TERRACOTTA);
        blocks.add(Blocks.LIGHT_BLUE_TERRACOTTA);
        blocks.add(Blocks.YELLOW_TERRACOTTA);
        blocks.add(Blocks.LIME_TERRACOTTA);
        blocks.add(Blocks.PINK_TERRACOTTA);
        blocks.add(Blocks.GRAY_TERRACOTTA);
        blocks.add(Blocks.LIGHT_GRAY_TERRACOTTA);
        blocks.add(Blocks.CYAN_TERRACOTTA);
        blocks.add(Blocks.PURPLE_TERRACOTTA);
        blocks.add(Blocks.BLUE_TERRACOTTA);
        blocks.add(Blocks.BROWN_TERRACOTTA);
        blocks.add(Blocks.GREEN_TERRACOTTA);
        blocks.add(Blocks.RED_TERRACOTTA);
        blocks.add(Blocks.BLACK_TERRACOTTA);
    }

    private static void addWool(List<Block> blocks) {
        blocks.add(Blocks.WHITE_WOOL);
        blocks.add(Blocks.ORANGE_WOOL);
        blocks.add(Blocks.MAGENTA_WOOL);
        blocks.add(Blocks.LIGHT_BLUE_WOOL);
        blocks.add(Blocks.YELLOW_WOOL);
        blocks.add(Blocks.LIME_WOOL);
        blocks.add(Blocks.PINK_WOOL);
        blocks.add(Blocks.GRAY_WOOL);
        blocks.add(Blocks.LIGHT_GRAY_WOOL);
        blocks.add(Blocks.CYAN_WOOL);
        blocks.add(Blocks.PURPLE_WOOL);
        blocks.add(Blocks.BLUE_WOOL);
        blocks.add(Blocks.BROWN_WOOL);
        blocks.add(Blocks.GREEN_WOOL);
        blocks.add(Blocks.RED_WOOL);
        blocks.add(Blocks.BLACK_WOOL);
    }

    private static void diffuse(float[][] r, float[][] g, float[][] b, boolean[][] opaque,
                                int x, int y, float er, float eg, float eb, float w,
                                int width, int height) {
        if (x < 0 || y < 0 || x >= width || y >= height || !opaque[y][x]) return;
        r[y][x] += er * w;
        g[y][x] += eg * w;
        b[y][x] += eb * w;
    }

    private static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        BufferedImage cur = src.getType() == BufferedImage.TYPE_INT_ARGB ? src : copyArgb(src);
        int cw = cur.getWidth(), ch = cur.getHeight();
        while ((cw > w * 2 || ch > h * 2) && cw / 2 >= w && ch / 2 >= h) {
            int nw = Math.max(w, cw / 2);
            int nh = Math.max(h, ch / 2);
            BufferedImage next = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = next.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(cur, 0, 0, nw, nh, null);
            g2.dispose();
            cur = next;
            cw = nw;
            ch = nh;
        }
        boolean up = cw <= w && ch <= h;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            up ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cur, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static BufferedImage copyArgb(BufferedImage src) {
        BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = c.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return c;
    }

    private static float clampF(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float[] rgbToLab(float r, float g, float b) {
        float sr = pivotRgb(r / 255f);
        float sg = pivotRgb(g / 255f);
        float sb = pivotRgb(b / 255f);
        float x = (0.4124564f * sr + 0.3575761f * sg + 0.1804375f * sb) / 0.95047f;
        float y = (0.2126729f * sr + 0.7151522f * sg + 0.0721750f * sb);
        float z = (0.0193339f * sr + 0.1191920f * sg + 0.9503041f * sb) / 1.08883f;
        x = pivotLab(x);
        y = pivotLab(y);
        z = pivotLab(z);
        return new float[]{116f * y - 16f, 500f * (x - y), 200f * (y - z)};
    }

    private static float pivotRgb(float u) {
        return u <= 0.04045f ? u / 12.92f : (float) Math.pow((u + 0.055f) / 1.055f, 2.4);
    }

    private static float pivotLab(float t) {
        return t > 0.008856f ? (float) Math.cbrt(t) : (7.787f * t + 16f / 116f);
    }
}
