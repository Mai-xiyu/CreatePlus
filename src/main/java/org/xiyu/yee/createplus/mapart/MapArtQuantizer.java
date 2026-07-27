package org.xiyu.yee.createplus.mapart;

import net.minecraft.world.level.material.MapColor;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 地图色量化：修正 {@link MapColor#calculateRGBColor} 的 ABGR 通道，
 * 用 CIELAB 感知距离匹配，再做 Floyd–Steinberg 抖动，减轻色带与偏差。
 */
public final class MapArtQuantizer {
    private static Entry[] palette;

    private record Entry(byte packedId, float r, float g, float b, float L, float a, float bLab) {
    }

    private MapArtQuantizer() {
    }

    /**
     * 缩放到目标尺寸后量化为地图色字节（行主序，长度 = w*h）。
     * 透明像素（alpha &lt; 16）写 0。
     */
    public static byte[] quantize(BufferedImage src, int targetW, int targetH) {
        ensurePalette();
        BufferedImage scaled = scale(src, targetW, targetH);
        float[][] r = new float[targetH][targetW];
        float[][] g = new float[targetH][targetW];
        float[][] b = new float[targetH][targetW];
        boolean[][] opaque = new boolean[targetH][targetW];

        for (int y = 0; y < targetH; y++) {
            for (int x = 0; x < targetW; x++) {
                int argb = scaled.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a < 16) {
                    opaque[y][x] = false;
                    continue;
                }
                opaque[y][x] = true;
                r[y][x] = (argb >> 16) & 0xFF;
                g[y][x] = (argb >> 8) & 0xFF;
                b[y][x] = argb & 0xFF;
            }
        }

        byte[] out = new byte[targetW * targetH];
        for (int y = 0; y < targetH; y++) {
            for (int x = 0; x < targetW; x++) {
                if (!opaque[y][x]) {
                    out[x + y * targetW] = 0;
                    continue;
                }

                float rr = clamp(r[y][x], 0, 255);
                float gg = clamp(g[y][x], 0, 255);
                float bb = clamp(b[y][x], 0, 255);
                Entry best = nearest(rr, gg, bb);
                out[x + y * targetW] = best.packedId();

                float er = rr - best.r();
                float eg = gg - best.g();
                float eb = bb - best.b();
                diffuse(r, g, b, opaque, x + 1, y, er, eg, eb, 7f / 16f, targetW, targetH);
                diffuse(r, g, b, opaque, x - 1, y + 1, er, eg, eb, 3f / 16f, targetW, targetH);
                diffuse(r, g, b, opaque, x, y + 1, er, eg, eb, 5f / 16f, targetW, targetH);
                diffuse(r, g, b, opaque, x + 1, y + 1, er, eg, eb, 1f / 16f, targetW, targetH);
            }
        }
        return out;
    }

    public static byte[] extractTile(byte[] full, int fullW, int ox, int oy, int tile) {
        byte[] colors = new byte[tile * tile];
        for (int y = 0; y < tile; y++) {
            System.arraycopy(full, (oy + y) * fullW + ox, colors, y * tile, tile);
        }
        return colors;
    }

    private static void diffuse(float[][] r, float[][] g, float[][] b, boolean[][] opaque,
                                int x, int y, float er, float eg, float eb, float w,
                                int width, int height) {
        if (x < 0 || y < 0 || x >= width || y >= height || !opaque[y][x]) return;
        r[y][x] += er * w;
        g[y][x] += eg * w;
        b[y][x] += eb * w;
    }

    private static Entry nearest(float r, float g, float b) {
        float[] lab = rgbToLab(r, g, b);
        Entry best = palette[0];
        float bestDist = Float.MAX_VALUE;
        for (Entry e : palette) {
            float dL = lab[0] - e.L();
            float da = lab[1] - e.a();
            float db = lab[2] - e.bLab();
            float dist = 1.2f * dL * dL + da * da + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }

    private static synchronized void ensurePalette() {
        if (palette != null) return;
        List<Entry> list = new ArrayList<>(256);
        for (int id = 1; id < 64; id++) {
            MapColor mc = MapColor.byId(id);
            if (mc == MapColor.NONE) continue;
            for (MapColor.Brightness br : MapColor.Brightness.values()) {
                // calculateRGBColor → 0xAABBGGRR（与地图纹理 / NativeImage 一致）
                int abgr = mc.calculateRGBColor(br);
                float r = abgr & 0xFF;
                float g = (abgr >> 8) & 0xFF;
                float b = (abgr >> 16) & 0xFF;
                float[] lab = rgbToLab(r, g, b);
                list.add(new Entry(mc.getPackedId(br), r, g, b, lab[0], lab[1], lab[2]));
            }
        }
        if (list.isEmpty()) {
            throw new IllegalStateException("MapColor palette empty");
        }
        palette = list.toArray(Entry[]::new);
    }

    private static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        BufferedImage cur = toArgb(src);
        int cw = cur.getWidth();
        int ch = cur.getHeight();
        // 大图先多级缩小，减少一次拉到 128 格时的色糊
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
        boolean upscale = cw <= w && ch <= h;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            upscale ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cur, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static BufferedImage toArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static float clamp(float v, float min, float max) {
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
