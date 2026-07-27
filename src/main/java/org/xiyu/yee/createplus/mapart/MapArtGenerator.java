package org.xiyu.yee.createplus.mapart;

import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 把图片量化成地图色，生成 locked filled_map，并写入「已保存的快捷栏」。
 * <ul>
 *   <li>单人 / 局域网主机：写入 {@link ServerLevel}（世界持久化）</li>
 *   <li>远端多人客户端：{@link ClientLevel#overrideMapData} + 本地缓存 + 物品 NBT 像素</li>
 * </ul>
 * 地图等级（scale 0~4）写入 {@link MapItemSavedData}：覆盖 128×2^scale 方块边长；
 * 每张画像素仍是 128×128，展示框墙面大小由网格几乘几决定。
 */
public final class MapArtGenerator {
    public static final int MAP_SIZE = 128;
    public static final int MAX_GRID = 9;
    public static final int MAX_SCALE = 4;

    public record Result(boolean ok, String message, int mapsCreated, List<ItemStack> maps, int cols, int rows) {
        public static Result fail(String message, int created) {
            return new Result(false, message, created, List.of(), 0, 0);
        }
    }

    public record ImageProbe(int width, int height, int suggestCols, int suggestRows) {
        public String summary() {
            return width + "×" + height + " → 建议 " + suggestCols + "×" + suggestRows
                + "（输出 " + (suggestCols * MAP_SIZE) + "×" + (suggestRows * MAP_SIZE) + "）";
        }
    }

    private MapArtGenerator() {
    }

    public static ImageProbe probe(File imageFile) throws Exception {
        BufferedImage src = ImageIO.read(imageFile);
        if (src == null) throw new IllegalArgumentException("无法读取图片");
        int w = src.getWidth();
        int h = src.getHeight();
        int cols = clamp(Math.max(1, (int) Math.round(w / (double) MAP_SIZE)), 1, MAX_GRID);
        int rows = clamp(Math.max(1, (int) Math.round(h / (double) MAP_SIZE)), 1, MAX_GRID);
        double aspect = w / (double) h;
        int bestC = cols, bestR = rows;
        double best = score(aspect, cols, rows, w, h);
        for (int c = 1; c <= MAX_GRID; c++) {
            for (int r = 1; r <= MAX_GRID; r++) {
                double s = score(aspect, c, r, w, h);
                if (s < best) {
                    best = s;
                    bestC = c;
                    bestR = r;
                }
            }
        }
        return new ImageProbe(w, h, bestC, bestR);
    }

    private static double score(double aspect, int c, int r, int w, int h) {
        double gridAspect = c / (double) r;
        double aspectPenalty = Math.abs(Math.log(gridAspect / aspect));
        double scaleX = w / (double) (c * MAP_SIZE);
        double scaleY = h / (double) (r * MAP_SIZE);
        double scale = Math.max(scaleX, scaleY);
        double scalePenalty = Math.abs(Math.log(Math.max(scale, 1e-6)));
        return aspectPenalty * 2.0 + scalePenalty;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static Result generate(File imageFile, int cols, int rows, int startGroup) {
        return generate(imageFile, cols, rows, startGroup, 0);
    }

    public static Result generate(File imageFile, int cols, int rows, int startGroup, int mapScale) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return Result.fail("需要进入世界后再生成", 0);
        }
        if (cols < 1 || rows < 1 || cols > MAX_GRID || rows > MAX_GRID) {
            return Result.fail("网格需在 1×1 ~ " + MAX_GRID + "×" + MAX_GRID, 0);
        }
        byte scale = (byte) clamp(mapScale, 0, MAX_SCALE);
        int total = cols * rows;
        if (startGroup < 0 || startGroup >= HotbarManager.NUM_HOTBAR_GROUPS) {
            return Result.fail("起始快捷栏行无效 (0-8)", 0);
        }
        if (startGroup * 9 + total > HotbarManager.NUM_HOTBAR_GROUPS * 9) {
            return Result.fail("格子不够：需要 " + total + " 格，从第 " + (startGroup + 1) + " 行起放不下", 0);
        }

        BufferedImage src;
        try {
            src = ImageIO.read(imageFile);
            if (src == null) return Result.fail("无法读取图片", 0);
        } catch (Exception e) {
            return Result.fail("读图失败: " + e.getMessage(), 0);
        }

        int tw = cols * MAP_SIZE;
        int th = rows * MAP_SIZE;
        // 整图量化（含抖动）再切瓦片，避免瓦片接缝色带断裂
        byte[] full = MapArtQuantizer.quantize(src, tw, th);

        List<byte[]> tiles = new ArrayList<>(total);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                tiles.add(MapArtQuantizer.extractTile(full, tw, col * MAP_SIZE, row * MAP_SIZE, MAP_SIZE));
            }
        }

        try {
            return CompletableFuture.supplyAsync(
                () -> writeMapsAndHotbars(tiles, startGroup, scale, cols, rows),
                mc
            ).get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            return Result.fail("写入失败: " + e.getMessage(), 0);
        }
    }

    private static Result writeMapsAndHotbars(List<byte[]> tiles, int startGroup, byte scale,
                                             int cols, int rows) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return Result.fail("世界已关闭", 0);
        }

        MinecraftServer server = mc.getSingleplayerServer();
        boolean integrated = server != null;
        List<ItemStack> maps = new ArrayList<>(tiles.size());

        for (int i = 0; i < tiles.size(); i++) {
            byte[] colors = tiles.get(i);
            String name = tiles.size() == 1 ? "地图画" : "地图画 " + (i + 1);
            ItemStack stack = integrated
                ? createServerMap(server.overworld(), colors, name, scale)
                : createClientMap(mc.level, colors, name, scale);
            if (stack.isEmpty()) {
                return Result.fail("创建地图失败 #" + (i + 1), maps.size());
            }
            maps.add(stack);
        }

        HotbarManager hotbars = mc.getHotbarManager();
        int index = startGroup * 9;
        for (ItemStack map : maps) {
            int group = index / 9;
            int slot = index % 9;
            Hotbar bar = hotbars.get(group);
            ensureSize(bar);
            bar.set(slot, map.copy());
            index++;
        }
        hotbars.save();

        String where = integrated ? "单人世界地图数据" : "客户端缓存（多人可用）";
        int cover = MAP_SIZE << scale;
        return new Result(true,
            "已生成 " + maps.size() + " 张（等级 " + scale + " · 覆盖边长 " + cover
                + "）→ 已保存快捷栏第 " + (startGroup + 1) + " 行起 · " + where,
            maps.size(),
            Collections.unmodifiableList(maps),
            cols,
            rows);
    }

    private static void ensureSize(Hotbar bar) {
        while (bar.size() < 9) {
            bar.add(ItemStack.EMPTY);
        }
    }

    private static ItemStack createServerMap(ServerLevel level, byte[] colors, String name, byte scale) {
        int id = level.getFreeMapId();
        MapItemSavedData data = MapItemSavedData.createFresh(0, 0, scale, true, false, level.dimension());
        data = data.locked();
        level.setMapData(MapItem.makeKey(id), data);
        for (int i = 0; i < colors.length; i++) {
            data.setColor(i % MAP_SIZE, i / MAP_SIZE, colors[i]);
        }
        data.setDirty();

        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        MapArtStore.attachToStack(stack, id, colors, scale);
        stack.setHoverName(Component.literal(name));
        return stack;
    }

    private static ItemStack createClientMap(ClientLevel level, byte[] colors, String name, byte scale) {
        int id = MapArtStore.nextId();
        MapArtStore.injectIntoClient(id, colors, level.dimension(), scale);

        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        MapArtStore.attachToStack(stack, id, colors, scale);
        stack.setHoverName(Component.literal(name));
        return stack;
    }
}
