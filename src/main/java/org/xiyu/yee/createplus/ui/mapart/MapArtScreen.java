package org.xiyu.yee.createplus.ui.mapart;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.xiyu.yee.createplus.mapart.BlockPixelArt;
import org.xiyu.yee.createplus.mapart.BlockPixelPlacer;
import org.xiyu.yee.createplus.mapart.MapArtGenerator;
import org.xiyu.yee.createplus.mapart.MapArtPlacer;
import org.xiyu.yee.createplus.mapart.MapArtStore;
import org.xiyu.yee.createplus.ui.Theme;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 两种方案并存：
 * <ul>
 *   <li>地图画：filled_map → 已保存快捷栏 / 展示框（高清；非房主联机仅自己可见）</li>
 *   <li>方块像素画：实心方块墙（原版全局可见，备用）</li>
 * </ul>
 */
public class MapArtScreen extends Screen {
    private enum Mode {
        MAP("地图画"),
        BLOCK("方块像素画(全局)");

        final String label;

        Mode(String label) {
            this.label = label;
        }
    }

    private Mode mode = Mode.MAP;

    // --- 地图画 ---
    private int cols = 1;
    private int rows = 1;
    private int startGroup = 0;
    private int mapScale = 0;
    private WallBlock wallBlock = WallBlock.QUARTZ;
    private List<ItemStack> lastMaps = List.of();
    private int lastCols = 1;
    private int lastRows = 1;

    // --- 方块像素画 ---
    private int widthBlocks = 32;
    private int heightBlocks = 32;
    private BlockPixelArt.Palette palette = BlockPixelArt.Palette.MIXED;
    private boolean dither = true;
    private boolean lockAspect = true;
    private double imageAspect = 1.0;
    private BufferedImage cachedImage;
    private EditBox blockWBox;
    private EditBox blockHBox;
    private boolean syncingSizeBoxes;

    private File selected;
    private String status = "";
    private String imageInfo = "";
    private EditBox pathBox;
    private boolean busy;
    private String lastBrowseDir = "";

    private enum WallBlock {
        QUARTZ(Blocks.QUARTZ_BLOCK.defaultBlockState(), "石英"),
        STONE(Blocks.STONE.defaultBlockState(), "石头"),
        WHITE_CONCRETE(Blocks.WHITE_CONCRETE.defaultBlockState(), "白混凝土"),
        OAK_PLANKS(Blocks.OAK_PLANKS.defaultBlockState(), "橡木板");

        final BlockState state;
        final String label;

        WallBlock(BlockState state, String label) {
            this.state = state;
            this.label = label;
        }
    }

    public MapArtScreen() {
        super(Component.translatable("createplus.map_art.title"));
    }

    @Override
    protected void init() {
        clearWidgets();
        int cx = this.width / 2;

        addRenderableWidget(CycleButton.builder((Mode m) -> Component.literal(m.label))
            .withValues(Mode.values())
            .withInitialValue(mode)
            .create(cx - 100, 40, 200, 20, Component.translatable("createplus.map_art.mode"),
                (btn, v) -> {
                    mode = v;
                    if (mode == Mode.BLOCK && cachedImage != null) {
                        applyBlockProbe(cachedImage);
                    } else if (mode == Mode.MAP && selected != null && selected.isFile()) {
                        try {
                            MapArtGenerator.ImageProbe probe = MapArtGenerator.probe(selected);
                            cols = probe.suggestCols();
                            rows = probe.suggestRows();
                            imageInfo = probe.summary();
                        } catch (Exception ignored) {
                        }
                    }
                    init();
                }));

        pathBox = new EditBox(this.font, cx - 160, 66, 250, 20, Component.literal("path"));
        pathBox.setMaxLength(1024);
        pathBox.setValue(selected == null ? "" : selected.getAbsolutePath());
        pathBox.setHint(Component.translatable("createplus.map_art.path_hint"));
        pathBox.setResponder(this::onPathTyped);
        addRenderableWidget(pathBox);

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.pick"), b -> pickFile())
            .bounds(cx + 96, 66, 64, 20).build());

        if (mode == Mode.MAP) {
            initMapWidgets(cx);
        } else {
            initBlockWidgets(cx);
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
            .bounds(cx - 60, this.height - 28, 120, 20).build());
    }

    private void initMapWidgets(int cx) {
        addRenderableWidget(CycleButton.builder((Integer v) -> Component.literal(v + " × " + rows))
            .withValues(1, 2, 3, 4, 5, 6, 7, 8, 9)
            .withInitialValue(cols)
            .create(cx - 160, 92, 100, 20, Component.translatable("createplus.map_art.cols"),
                (btn, v) -> {
                    cols = v;
                    init();
                }));

        addRenderableWidget(CycleButton.builder((Integer v) -> Component.literal(cols + " × " + v))
            .withValues(1, 2, 3, 4, 5, 6, 7, 8, 9)
            .withInitialValue(rows)
            .create(cx - 50, 92, 100, 20, Component.translatable("createplus.map_art.rows"),
                (btn, v) -> {
                    rows = v;
                    init();
                }));

        addRenderableWidget(CycleButton.builder((Integer v) -> Component.literal(String.valueOf(v + 1)))
            .withValues(0, 1, 2, 3, 4, 5, 6, 7, 8)
            .withInitialValue(startGroup)
            .create(cx + 60, 92, 100, 20, Component.translatable("createplus.map_art.start_row"),
                (btn, v) -> startGroup = v));

        addRenderableWidget(CycleButton.builder((Integer v) -> Component.literal(
                "Lv." + v + " · " + (128 << v) + "格"))
            .withValues(0, 1, 2, 3, 4)
            .withInitialValue(mapScale)
            .create(cx - 160, 118, 150, 20, Component.translatable("createplus.map_art.scale"),
                (btn, v) -> mapScale = v));

        addRenderableWidget(CycleButton.builder((WallBlock w) -> Component.literal(w.label))
            .withValues(WallBlock.values())
            .withInitialValue(wallBlock)
            .create(cx + 0, 118, 160, 20, Component.translatable("createplus.map_art.wall"),
                (btn, v) -> wallBlock = v));

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.single"), b -> {
            cols = 1;
            rows = 1;
            init();
            status = "已设为单张 1×1";
        }).bounds(cx - 160, 144, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.generate"), b -> generateMaps())
            .bounds(cx - 50, 144, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.clear"), b -> clearHotbars())
            .bounds(cx + 60, 144, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.place"), b -> armMapPlace())
            .bounds(cx - 60, 170, 120, 20).build());
    }

    private void initBlockWidgets(int cx) {
        blockWBox = new EditBox(this.font, cx - 160, 92, 70, 20, Component.literal("w"));
        blockWBox.setMaxLength(4);
        blockWBox.setValue(String.valueOf(widthBlocks));
        blockWBox.setFilter(s -> s.isEmpty() || s.matches("\\d{1,4}"));
        blockWBox.setResponder(s -> onBlockSizeTyped(true, s));
        addRenderableWidget(blockWBox);

        blockHBox = new EditBox(this.font, cx - 80, 92, 70, 20, Component.literal("h"));
        blockHBox.setMaxLength(4);
        blockHBox.setValue(String.valueOf(heightBlocks));
        blockHBox.setFilter(s -> s.isEmpty() || s.matches("\\d{1,4}"));
        blockHBox.setResponder(s -> onBlockSizeTyped(false, s));
        addRenderableWidget(blockHBox);

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.suggest"), b -> {
            if (cachedImage != null) {
                applyBlockProbe(cachedImage);
                refreshBlockSizeBoxes();
                status = "已按图片自动建议 " + widthBlocks + "×" + heightBlocks;
            } else {
                status = "请先选图";
            }
        }).bounds(cx + 0, 92, 80, 20).build());

        addRenderableWidget(CycleButton.builder((BlockPixelArt.Palette p) -> Component.literal(p.label))
            .withValues(BlockPixelArt.Palette.values())
            .withInitialValue(palette)
            .create(cx + 90, 92, 70, 20, Component.translatable("createplus.map_art.palette"),
                (btn, v) -> palette = v));

        addRenderableWidget(CycleButton.onOffBuilder(dither)
            .create(cx - 160, 118, 100, 20, Component.translatable("createplus.map_art.dither"),
                (btn, v) -> dither = v));

        addRenderableWidget(CycleButton.onOffBuilder(lockAspect)
            .create(cx - 50, 118, 100, 20, Component.translatable("createplus.map_art.lock_aspect"),
                (btn, v) -> lockAspect = v));

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.place_blocks"), b -> armBlockPlace())
            .bounds(cx + 60, 118, 100, 20).build());
    }

    private void onBlockSizeTyped(boolean editingWidth, String text) {
        if (syncingSizeBoxes || text == null || text.isBlank()) return;
        int v;
        try {
            v = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return;
        }
        v = Math.max(1, Math.min(BlockPixelArt.MAX_SIDE, v));
        if (editingWidth) {
            widthBlocks = v;
            if (lockAspect && imageAspect > 0) {
                heightBlocks = Math.max(1, (int) Math.round(v / imageAspect));
                int[] sized = BlockPixelArt.clampToBudget(widthBlocks, heightBlocks);
                widthBlocks = sized[0];
                heightBlocks = sized[1];
                syncingSizeBoxes = true;
                if (blockHBox != null) blockHBox.setValue(String.valueOf(heightBlocks));
                if (blockWBox != null && widthBlocks != v) blockWBox.setValue(String.valueOf(widthBlocks));
                syncingSizeBoxes = false;
            } else if (!BlockPixelArt.fits(widthBlocks, heightBlocks)) {
                // 关锁：只压正在改的宽，绝不按比例改高
                widthBlocks = Math.max(1, Math.min(widthBlocks,
                    BlockPixelArt.MAX_AREA / Math.max(1, heightBlocks)));
                if (blockWBox != null && widthBlocks != v) {
                    syncingSizeBoxes = true;
                    blockWBox.setValue(String.valueOf(widthBlocks));
                    syncingSizeBoxes = false;
                }
            }
        } else {
            heightBlocks = v;
            if (lockAspect && imageAspect > 0) {
                widthBlocks = Math.max(1, (int) Math.round(v * imageAspect));
                int[] sized = BlockPixelArt.clampToBudget(widthBlocks, heightBlocks);
                widthBlocks = sized[0];
                heightBlocks = sized[1];
                syncingSizeBoxes = true;
                if (blockWBox != null) blockWBox.setValue(String.valueOf(widthBlocks));
                if (blockHBox != null && heightBlocks != v) blockHBox.setValue(String.valueOf(heightBlocks));
                syncingSizeBoxes = false;
            } else if (!BlockPixelArt.fits(widthBlocks, heightBlocks)) {
                // 关锁：只压正在改的高，绝不按比例改宽
                heightBlocks = Math.max(1, Math.min(heightBlocks,
                    BlockPixelArt.MAX_AREA / Math.max(1, widthBlocks)));
                if (blockHBox != null && heightBlocks != v) {
                    syncingSizeBoxes = true;
                    blockHBox.setValue(String.valueOf(heightBlocks));
                    syncingSizeBoxes = false;
                }
            }
        }
    }

    private void refreshBlockSizeBoxes() {
        syncingSizeBoxes = true;
        if (blockWBox != null) blockWBox.setValue(String.valueOf(widthBlocks));
        if (blockHBox != null) blockHBox.setValue(String.valueOf(heightBlocks));
        syncingSizeBoxes = false;
    }

    private void applyBlockProbe(BufferedImage img) {
        BlockPixelArt.Probe probe = BlockPixelArt.probe(img);
        widthBlocks = probe.suggestW();
        heightBlocks = probe.suggestH();
        imageAspect = img.getWidth() / (double) Math.max(1, img.getHeight());
        imageInfo = probe.summary();
    }

    private void onPathTyped(String text) {
        if (busy || text == null || text.isBlank()) return;
        File f = new File(text.trim());
        if (f.isFile() && (selected == null || !f.equals(selected))) {
            applyImage(f);
        }
    }

    private void pickFile() {
        if (busy) return;
        String defaultPath = selected != null
            ? selected.getAbsolutePath()
            : (lastBrowseDir.isEmpty() ? Path.of("").toAbsolutePath().toString() : lastBrowseDir);
        String path;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer patterns = stack.pointers(
                stack.UTF8("*.png"), stack.UTF8("*.jpg"), stack.UTF8("*.jpeg"),
                stack.UTF8("*.bmp"), stack.UTF8("*.gif"), stack.UTF8("*.webp"));
            path = TinyFileDialogs.tinyfd_openFileDialog(
                Component.translatable("createplus.map_art.pick_title").getString(),
                defaultPath, patterns, "Images", false);
        } catch (Exception e) {
            status = "打开文件选择器失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return;
        }
        if (path == null || path.isBlank()) {
            status = "已取消选图";
            return;
        }
        applyImage(new File(path));
    }

    private void applyImage(File file) {
        if (!file.isFile()) {
            status = "文件不存在";
            return;
        }
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) throw new IllegalArgumentException("无法读取图片");
            selected = file;
            cachedImage = img;
            if (mode == Mode.MAP) {
                MapArtGenerator.ImageProbe probe = MapArtGenerator.probe(file);
                cols = probe.suggestCols();
                rows = probe.suggestRows();
                imageInfo = probe.summary();
            } else {
                applyBlockProbe(img);
            }
            status = "已加载 " + file.getName() + (mode == Mode.BLOCK
                ? " · 已自动建议 " + widthBlocks + "×" + heightBlocks : "");
            File parent = file.getParentFile();
            if (parent != null) lastBrowseDir = parent.getAbsolutePath() + File.separator;
            String path = file.getAbsolutePath();
            init();
            if (pathBox != null) pathBox.setValue(path);
            if (mode == Mode.BLOCK) refreshBlockSizeBoxes();
        } catch (Exception e) {
            status = "读图失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            imageInfo = "";
            cachedImage = null;
        }
    }

    private File resolveFile() {
        File file = selected;
        if (file == null || !file.isFile()) {
            String typed = pathBox != null ? pathBox.getValue().trim() : "";
            if (!typed.isEmpty()) file = new File(typed);
        }
        return file != null && file.isFile() ? file : null;
    }

    private void generateMaps() {
        if (busy) return;
        File file = resolveFile();
        if (file == null) {
            status = "请先选图";
            return;
        }
        busy = true;
        status = "生成地图画… " + cols + "×" + rows;
        File f = file;
        int c = cols, r = rows, g = startGroup, s = mapScale;
        Thread worker = new Thread(() -> {
            MapArtGenerator.Result result = MapArtGenerator.generate(f, c, r, g, s);
            this.minecraft.execute(() -> {
                busy = false;
                status = result.message();
                if (result.ok()) {
                    lastMaps = new ArrayList<>(result.maps());
                    lastCols = result.cols();
                    lastRows = result.rows();
                }
            });
        }, "createplus-mapart-gen");
        worker.setDaemon(true);
        worker.start();
    }

    private void clearHotbars() {
        int n = MapArtStore.clearSavedHotbarMapArt();
        lastMaps = List.of();
        status = n == 0 ? "已保存快捷栏里没有地图画" : "已清空 " + n + " 张地图画";
    }

    private void armMapPlace() {
        if (lastMaps == null || lastMaps.isEmpty()) {
            status = "请先生成地图画，再点自动放置";
            return;
        }
        MapArtPlacer.arm(lastMaps, lastCols, lastRows, wallBlock.state);
        status = "已武装地图画：关闭后右键墙面";
        onClose();
    }

    private void armBlockPlace() {
        if (busy) return;
        if (cachedImage == null) {
            File file = resolveFile();
            if (file == null) {
                status = "请先选图";
                return;
            }
            try {
                cachedImage = ImageIO.read(file);
                selected = file;
            } catch (Exception e) {
                status = "读图失败: " + e.getMessage();
                return;
            }
        }
        if (cachedImage == null) {
            status = "请先选图";
            return;
        }
        busy = true;
        status = "量化方块画… " + widthBlocks + "×" + heightBlocks;
        BufferedImage img = cachedImage;
        int w = widthBlocks, h = heightBlocks;
        BlockPixelArt.Palette pal = palette;
        boolean dith = dither;
        Thread worker = new Thread(() -> {
            BlockPixelArt.Grid grid = BlockPixelArt.quantize(img, w, h, pal, dith);
            this.minecraft.execute(() -> {
                busy = false;
                BlockPixelPlacer.arm(grid);
                status = "已武装方块画：关闭后右键墙面（全局可见）";
                onClose();
            });
        }, "createplus-block-pixel");
        worker.setDaemon(true);
        worker.start();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, Theme.TEXT);
        graphics.drawCenteredString(this.font,
            Component.translatable(mode == Mode.MAP
                ? "createplus.map_art.hint_map"
                : "createplus.map_art.hint_block").getString(),
            this.width / 2, 26, Theme.TEXT_MUTED);

        if (mode == Mode.MAP) {
            int outW = cols * MapArtGenerator.MAP_SIZE;
            int outH = rows * MapArtGenerator.MAP_SIZE;
            graphics.drawCenteredString(this.font,
                cols + "×" + rows + " 张 · 像素 " + outW + "×" + outH
                    + " · 等级 " + mapScale + " · 占 " + (cols * rows) + " 格快捷栏",
                this.width / 2, 198, Theme.TEXT_MUTED);
        } else {
            graphics.drawCenteredString(this.font,
                "宽×高 " + widthBlocks + "×" + heightBlocks
                    + "（" + (widthBlocks * heightBlocks) + " 格）· 上限单边 "
                    + BlockPixelArt.MAX_SIDE + " / 总面积 " + BlockPixelArt.MAX_AREA
                    + (lockAspect ? " · 锁比例" : "")
                    + " · 侧面竖放 / 地板天花板平铺",
                this.width / 2, 148, Theme.TEXT_MUTED);
        }

        if (imageInfo != null && !imageInfo.isEmpty()) {
            graphics.drawCenteredString(this.font, imageInfo, this.width / 2,
                mode == Mode.MAP ? 212 : 162, Theme.TEXT_MUTED);
        }
        if (status != null && !status.isEmpty()) {
            graphics.drawCenteredString(this.font, status, this.width / 2,
                mode == Mode.MAP ? 226 : 176, Theme.ACCENT);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
