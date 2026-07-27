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
import org.xiyu.yee.createplus.mapart.MapArtGenerator;
import org.xiyu.yee.createplus.mapart.MapArtPlacer;
import org.xiyu.yee.createplus.mapart.MapArtStore;
import org.xiyu.yee.createplus.ui.Theme;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 选图 → 等级/网格 → 生成到已保存快捷栏；可清空、可自动放置（墙+展示框+地图）。
 */
public class MapArtScreen extends Screen {
    private int cols = 1;
    private int rows = 1;
    private int startGroup = 0;
    private int mapScale = 0;
    private WallBlock wallBlock = WallBlock.QUARTZ;
    private File selected;
    private String status = "";
    private String imageInfo = "";
    private EditBox pathBox;
    private boolean busy;
    private String lastBrowseDir = "";
    private List<ItemStack> lastMaps = List.of();
    private int lastCols = 1;
    private int lastRows = 1;

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

        pathBox = new EditBox(this.font, cx - 160, 44, 250, 20, Component.literal("path"));
        pathBox.setMaxLength(1024);
        pathBox.setValue(selected == null ? "" : selected.getAbsolutePath());
        pathBox.setHint(Component.translatable("createplus.map_art.path_hint"));
        pathBox.setResponder(this::onPathTyped);
        addRenderableWidget(pathBox);

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.pick"), b -> pickFile())
            .bounds(cx + 96, 44, 64, 20).build());

        addRenderableWidget(CycleButton.builder((Integer v) -> Component.literal(v + " × " + rows))
            .withValues(1, 2, 3, 4, 5, 6, 7, 8, 9)
            .withInitialValue(cols)
            .create(cx - 160, 70, 100, 20, Component.translatable("createplus.map_art.cols"),
                (btn, v) -> {
                    cols = v;
                    init();
                }));

        addRenderableWidget(CycleButton.builder((Integer v) -> Component.literal(cols + " × " + v))
            .withValues(1, 2, 3, 4, 5, 6, 7, 8, 9)
            .withInitialValue(rows)
            .create(cx - 50, 70, 100, 20, Component.translatable("createplus.map_art.rows"),
                (btn, v) -> {
                    rows = v;
                    init();
                }));

        addRenderableWidget(CycleButton.builder((Integer v) -> Component.literal(String.valueOf(v + 1)))
            .withValues(0, 1, 2, 3, 4, 5, 6, 7, 8)
            .withInitialValue(startGroup)
            .create(cx + 60, 70, 100, 20, Component.translatable("createplus.map_art.start_row"),
                (btn, v) -> startGroup = v));

        addRenderableWidget(CycleButton.builder((Integer v) -> Component.literal(
                "Lv." + v + " · " + (128 << v) + "格"))
            .withValues(0, 1, 2, 3, 4)
            .withInitialValue(mapScale)
            .create(cx - 160, 96, 150, 20, Component.translatable("createplus.map_art.scale"),
                (btn, v) -> mapScale = v));

        addRenderableWidget(CycleButton.builder((WallBlock w) -> Component.literal(w.label))
            .withValues(WallBlock.values())
            .withInitialValue(wallBlock)
            .create(cx + 0, 96, 160, 20, Component.translatable("createplus.map_art.wall"),
                (btn, v) -> wallBlock = v));

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.single"), b -> {
            cols = 1;
            rows = 1;
            init();
            status = "已设为单张 1×1，选图后生成即可";
        }).bounds(cx - 160, 122, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.generate"), b -> generate())
            .bounds(cx - 50, 122, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.clear"), b -> clearHotbars())
            .bounds(cx + 60, 122, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("createplus.map_art.place"), b -> armPlace())
            .bounds(cx - 60, 148, 120, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
            .bounds(cx - 60, this.height - 28, 120, 20).build());
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
                stack.UTF8("*.png"),
                stack.UTF8("*.jpg"),
                stack.UTF8("*.jpeg"),
                stack.UTF8("*.bmp"),
                stack.UTF8("*.gif"),
                stack.UTF8("*.webp")
            );
            path = TinyFileDialogs.tinyfd_openFileDialog(
                Component.translatable("createplus.map_art.pick_title").getString(),
                defaultPath,
                patterns,
                "Images",
                false
            );
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
            MapArtGenerator.ImageProbe probe = MapArtGenerator.probe(file);
            selected = file;
            cols = probe.suggestCols();
            rows = probe.suggestRows();
            imageInfo = probe.summary();
            status = "已加载 " + file.getName() + " · 可改几乘几 / 等级后生成";
            File parent = file.getParentFile();
            if (parent != null) lastBrowseDir = parent.getAbsolutePath() + File.separator;
            String path = file.getAbsolutePath();
            init();
            if (pathBox != null) pathBox.setValue(path);
        } catch (Exception e) {
            status = "读图失败: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            imageInfo = "";
        }
    }

    private void generate() {
        if (busy) return;
        File file = selected;
        if (file == null || !file.isFile()) {
            String typed = pathBox != null ? pathBox.getValue().trim() : "";
            if (!typed.isEmpty()) file = new File(typed);
        }
        if (file == null || !file.isFile()) {
            status = "请先选图，或把图片路径贴到输入框";
            return;
        }
        busy = true;
        status = "生成中… " + cols + "×" + rows + " · 等级 " + mapScale;
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

    private void armPlace() {
        List<ItemStack> maps = lastMaps;
        if (maps == null || maps.isEmpty()) {
            status = "请先生成地图画，再点自动放置";
            return;
        }
        MapArtPlacer.arm(maps, lastCols, lastRows, wallBlock.state);
        status = "已武装：关闭界面后右键点墙面开始放置";
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, Theme.TEXT);
        graphics.drawCenteredString(this.font,
            Component.translatable("createplus.map_art.hint").getString(),
            this.width / 2, 28, Theme.TEXT_MUTED);

        int outW = cols * MapArtGenerator.MAP_SIZE;
        int outH = rows * MapArtGenerator.MAP_SIZE;
        int cover = MapArtGenerator.MAP_SIZE << mapScale;
        graphics.drawCenteredString(this.font,
            cols + "×" + rows + " 张 · 像素 " + outW + "×" + outH
                + " · 等级 " + mapScale + "（覆盖边长 " + cover + " 格）· 占 "
                + (cols * rows) + " 格快捷栏",
            this.width / 2, 176, Theme.TEXT_MUTED);

        if (imageInfo != null && !imageInfo.isEmpty()) {
            graphics.drawCenteredString(this.font, imageInfo, this.width / 2, 190, Theme.TEXT_MUTED);
        }

        if (status != null && !status.isEmpty()) {
            graphics.drawCenteredString(this.font, status, this.width / 2, 206, Theme.ACCENT);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
