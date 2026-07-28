package org.xiyu.yee.createplus.mapart;

import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.utils.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客户端地图画持久化。
 * <p>
 * 远端多人时 {@link ClientLevel#setMapData} 是空实现，必须用 {@link ClientLevel#overrideMapData}。
 * {@link ClientLevel#getFreeMapId()} 恒返回 0，所以使用高位自增 ID，避免和服务器地图撞车。
 */
public final class MapArtStore {
    /** 与原版低位 map id 隔开；服务端极少会发到这个区间。 */
    public static final int ID_BASE = 1_900_000_000;
    public static final String TAG_FLAG = "createplus_mapart";
    public static final String TAG_COLORS = "createplus_colors";
    public static final String TAG_SCALE = "createplus_scale";

    private static final AtomicInteger NEXT = new AtomicInteger(0);
    private static final Map<Integer, byte[]> CACHE = new LinkedHashMap<>();
    private static final java.util.Set<Integer> APPLIED = new java.util.HashSet<>();

    private MapArtStore() {
    }

    public static int parseMapKey(String mapKey) {
        if (mapKey == null || !mapKey.startsWith("map_")) return -1;
        try {
            return Integer.parseInt(mapKey.substring(4));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void markApplied(int mapId) {
        APPLIED.add(mapId);
    }

    public static boolean isApplied(int mapId) {
        return APPLIED.contains(mapId);
    }

    public static void clearApplied() {
        APPLIED.clear();
    }

    public static boolean isOurId(int mapId) {
        return mapId >= ID_BASE;
    }

    public static synchronized int nextId() {
        loadIndex();
        int id = ID_BASE + NEXT.getAndIncrement();
        saveIndex();
        return id;
    }

    public static void putColors(int mapId, byte[] colors) {
        if (colors == null || colors.length != 128 * 128) return;
        CACHE.put(mapId, colors.clone());
        saveMapFile(mapId, colors);
    }

    public static byte[] getColors(int mapId) {
        byte[] cached = CACHE.get(mapId);
        if (cached != null) return cached;
        byte[] disk = loadMapFile(mapId);
        if (disk != null) CACHE.put(mapId, disk);
        return disk;
    }

    /** 把像素灌进 ClientLevel，供 MapRenderer / 手持地图读取。 */
    public static void injectIntoClient(int mapId, byte[] colors, ResourceKey<Level> dimension) {
        injectIntoClient(mapId, colors, dimension, (byte) 0);
    }

    public static void injectIntoClient(int mapId, byte[] colors, ResourceKey<Level> dimension, byte scale) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel client = mc.level;
        if (client == null || colors == null || colors.length != 128 * 128) return;

        byte s = (byte) Math.max(0, Math.min(4, scale));
        MapItemSavedData data = MapItemSavedData.createForClient(s, true, dimension);
        for (int i = 0; i < colors.length; i++) {
            data.setColor(i % 128, i / 128, colors[i]);
        }
        client.overrideMapData(MapItem.makeKey(mapId), data);
        putColors(mapId, colors);
        markApplied(mapId);
    }

    public static void injectFromStack(ItemStack stack, ClientLevel level) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.FILLED_MAP)) return;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(TAG_FLAG)) return;
        Integer id = MapItem.getMapId(stack);
        if (id == null) return;

        byte[] colors = null;
        if (tag.contains(TAG_COLORS, Tag.TAG_BYTE_ARRAY)) {
            colors = tag.getByteArray(TAG_COLORS);
        }
        if (colors == null || colors.length != 128 * 128) {
            colors = getColors(id);
        }
        if (colors == null) return;

        if (level.getMapData(MapItem.makeKey(id)) == null) {
            byte scale = scaleOf(stack);
            injectIntoClient(id, colors, level.dimension(), scale);
        }
    }

    /** 进世界后：把本地缓存的地图画重新 override 进 ClientLevel。 */
    public static void reinjectAll() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel client = mc.level;
        if (client == null) return;

        loadAllFromDisk();
        for (Map.Entry<Integer, byte[]> e : CACHE.entrySet()) {
            injectIntoClient(e.getKey(), e.getValue(), client.dimension());
        }

        try {
            HotbarManager mgr = mc.getHotbarManager();
            for (int g = 0; g < HotbarManager.NUM_HOTBAR_GROUPS; g++) {
                Hotbar bar = mgr.get(g);
                for (ItemStack stack : bar) {
                    injectFromStack(stack, client);
                }
            }
        } catch (Exception e) {
            Createplus.LOGGER.warn("CreatePlus mapart: hotbar reinject failed", e);
        }
        Createplus.LOGGER.info("CreatePlus mapart: reinjected {} client maps", CACHE.size());
    }

    public static void attachToStack(ItemStack stack, int mapId, byte[] colors) {
        attachToStack(stack, mapId, colors, (byte) 0);
    }

    public static void attachToStack(ItemStack stack, int mapId, byte[] colors, byte scale) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("map", mapId);
        tag.putBoolean(TAG_FLAG, true);
        tag.putByte(TAG_SCALE, (byte) Math.max(0, Math.min(4, scale)));
        // 不再把颜色数组塞进物品 NBT（体积大且无法让原版他人看见）
    }

    public static boolean isMapArtStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.FILLED_MAP)) return false;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean(TAG_FLAG)) return true;
        Integer id = MapItem.getMapId(stack);
        return id != null && isOurId(id);
    }

    /** 清空已保存快捷栏里的 CreatePlus 地图画，返回清除数量。 */
    public static int clearSavedHotbarMapArt() {
        Minecraft mc = Minecraft.getInstance();
        HotbarManager mgr = mc.getHotbarManager();
        int cleared = 0;
        for (int g = 0; g < HotbarManager.NUM_HOTBAR_GROUPS; g++) {
            Hotbar bar = mgr.get(g);
            for (int s = 0; s < 9; s++) {
                while (bar.size() <= s) bar.add(ItemStack.EMPTY);
                ItemStack stack = bar.get(s);
                if (isMapArtStack(stack)) {
                    bar.set(s, ItemStack.EMPTY);
                    cleared++;
                }
            }
        }
        mgr.save();
        return cleared;
    }

    public static byte scaleOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_SCALE)) {
            return (byte) Math.max(0, Math.min(4, tag.getByte(TAG_SCALE)));
        }
        return 0;
    }

    private static Path dir() throws Exception {
        Path p = FileUtils.getModDirectory().resolve("mapart");
        Files.createDirectories(p);
        return p;
    }

    private static Path indexFile() throws Exception {
        return dir().resolve("index.nbt");
    }

    private static Path mapFile(int id) throws Exception {
        return dir().resolve("map_" + id + ".nbt");
    }

    private static void loadIndex() {
        try {
            Path f = indexFile();
            if (!Files.exists(f)) return;
            CompoundTag tag = NbtIo.readCompressed(f.toFile());
            if (tag != null && tag.contains("next")) {
                NEXT.set(Math.max(NEXT.get(), tag.getInt("next")));
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveIndex() {
        try {
            CompoundTag tag = new CompoundTag();
            tag.putInt("next", NEXT.get());
            NbtIo.writeCompressed(tag, indexFile().toFile());
        } catch (Exception e) {
            Createplus.LOGGER.warn("CreatePlus mapart: failed to save index", e);
        }
    }

    private static void saveMapFile(int id, byte[] colors) {
        try {
            CompoundTag tag = new CompoundTag();
            tag.putInt("id", id);
            tag.putByteArray("colors", colors);
            NbtIo.writeCompressed(tag, mapFile(id).toFile());
        } catch (Exception e) {
            Createplus.LOGGER.warn("CreatePlus mapart: failed to save map {}", id, e);
        }
    }

    private static byte[] loadMapFile(int id) {
        try {
            Path f = mapFile(id);
            if (!Files.exists(f)) return null;
            CompoundTag tag = NbtIo.readCompressed(f.toFile());
            if (tag == null || !tag.contains("colors")) return null;
            byte[] colors = tag.getByteArray("colors");
            return colors.length == 128 * 128 ? colors : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void loadAllFromDisk() {
        try {
            loadIndex();
            try (var stream = Files.list(dir())) {
                stream.filter(p -> p.getFileName().toString().startsWith("map_"))
                    .forEach(p -> {
                        try {
                            CompoundTag tag = NbtIo.readCompressed(p.toFile());
                            if (tag == null) return;
                            int id = tag.getInt("id");
                            byte[] colors = tag.getByteArray("colors");
                            if (colors.length == 128 * 128) CACHE.put(id, colors);
                        } catch (Exception ignored) {
                        }
                    });
            }
        } catch (Exception e) {
            Createplus.LOGGER.warn("CreatePlus mapart: disk load failed", e);
        }
    }
}
