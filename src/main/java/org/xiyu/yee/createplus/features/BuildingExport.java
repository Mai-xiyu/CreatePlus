package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.utils.BlockEdits;
import org.xiyu.yee.createplus.utils.Selection;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 把选区里的建筑写成 .mcfunction，方便搬到别的存档。
 *
 * 命令入口在 {@code /createplus export <名称>}，选区来自选区工具，
 * 不再自己维护一套木铲选点逻辑。
 */
public class BuildingExport extends CreativePlusFeature {
    private final BooleanSetting includeAir = boolSetting("include_air", false);
    private final BooleanSetting relativeCoords = boolSetting("relative_coords", true);

    public BuildingExport() {
        super("building_export", "把选区导出为 .mcfunction 文件", Category.BUILDING);
    }

    /**
     * 导出当前选区。
     *
     * @return 写出的方块数量，失败时返回 -1
     */
    public int export(Path file) throws IOException {
        Minecraft mc = Minecraft.getInstance();
        Selection selection = Selection.shared();
        if (mc.level == null || !selection.isComplete()) return -1;

        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();
        List<String> lines = new ArrayList<>();

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.isAir() && !includeAir.get()) continue;

                    if (relativeCoords.get()) {
                        BlockPos rel = pos.subtract(min);
                        lines.add(String.format("setblock ~%d ~%d ~%d %s",
                            rel.getX(), rel.getY(), rel.getZ(), BlockEdits.toCommandString(state)));
                    } else {
                        lines.add(String.format("setblock %d %d %d %s",
                            pos.getX(), pos.getY(), pos.getZ(), BlockEdits.toCommandString(state)));
                    }
                }
            }
        }

        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("# CreatePlus building export\n");
            writer.write(String.format("# size %dx%dx%d\n",
                selection.width(), selection.height(), selection.depth()));
            for (String line : lines) {
                writer.write(line);
                writer.write('\n');
            }
        }
        return lines.size();
    }

    @Override
    public String getDetailedDescription() {
        Selection selection = Selection.shared();
        return getTranslatedDescription()
            + "\n§7/createplus export <名称> §8· §7/createplus import <名称>"
            + "\n§7当前选区: §f" + (selection.isComplete() ? selection.describe() : "未选择")
            + "\n§7文件位于 .minecraft/createplus/buildings/";
    }
}
