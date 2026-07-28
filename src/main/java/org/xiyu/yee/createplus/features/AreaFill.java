package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.utils.BlockEdits;
import org.xiyu.yee.createplus.utils.EditHistory;
import org.xiyu.yee.createplus.utils.EditQueue;
import org.xiyu.yee.createplus.utils.Selection;

/**
 * 用手中的方块填充选区，附带撤销。
 *
 * 有 OP 时优先走原版 {@code /fill}（清空 / 实心 / 空心 / 补空气 / 墙 / 棱），
 * 超过 32768 自动切成多批；无权限或调色板模式再退回逐格 EditQueue。
 */
public class AreaFill extends CreativePlusFeature {

    public enum Mode {
        /** 整个选区都填满。 */
        FILL,
        /** 只填外壳，内部掏空。 */
        HOLLOW,
        /** 只填四面墙，不含顶和底。 */
        WALLS,
        /** 只填 12 条棱。 */
        FRAME,
        /** 只填原本是空气的位置。 */
        REPLACE_AIR,
        /** 清空为空气。 */
        CLEAR
    }

    private final EnumSetting<Mode> mode = enumSetting("mode", Mode.FILL);
    private final IntSetting perTick = intSetting("per_tick", 64, 4, 512);
    /** 单次操作方块数上限，默认一亿；改完会同步到全局批量编辑。 */
    private final IntSetting maxVolume = intSetting("max_volume",
        BlockEdits.DEFAULT_MAX_VOLUME, BlockEdits.FILL_COMMAND_MAX, BlockEdits.ABSOLUTE_MAX_VOLUME,
        1_000_000, "");

    public AreaFill() {
        super("area_fill", "用手中的方块填充选区，支持空心/墙壁/棱框与撤销", Category.BUILDING);
        perTick.onChange(EditQueue::setPerTick);
        maxVolume.onChange(BlockEdits::setMaxVolume);
        BlockEdits.setMaxVolume(maxVolume.value());
    }

    @Override
    public void onEnable() {
        EditQueue.setPerTick(perTick.value());
        notifyPlayer("§b[CreatePlus] §7区域填充已启用。先用选区工具选好范围，"
            + "再手持方块按填充键（默认未绑定，可在控制设置里指定）");
    }

    public Mode getMode() {
        return mode.get();
    }

    public void setMode(Mode value) {
        mode.set(value);
    }

    /** 同步设置项与全局上限（命令 / GUI 共用）。 */
    public void setMaxVolumeLimit(int value) {
        maxVolume.set(value);
    }

    public int getMaxVolumeLimit() {
        return maxVolume.value();
    }

    /** 按键触发的填充。 */
    public void fillFromKeybind() {
        fill(mode.get());
    }

    public void fill(Mode fillMode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (EditQueue.isBusy()) {
            notifyActionBar("§c上一次操作还没做完，剩余 " + EditQueue.remaining() + " 个任务");
            return;
        }

        Selection selection = Selection.shared();
        if (!selection.isComplete()) {
            notifyActionBar("§c请先用选区工具选择一个区域");
            return;
        }

        BlockState target = resolveTarget(fillMode);
        if (target == null) {
            notifyActionBar("§c主手需要手持一个方块");
            return;
        }

        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();
        long volume = selection.volume();
        if (volume > BlockEdits.maxVolume()) {
            notifyActionBar("§c选区太大了（" + volume + " > " + BlockEdits.maxVolume() + "）");
            return;
        }

        PaintPalette palette = PaintPalette.active();
        boolean canCommand = palette == null && BlockEdits.canUseCommands();

        if (canCommand && tryFillViaCommands(fillMode, min, max, target, volume, mc)) {
            return;
        }

        fillViaQueue(fillMode, min, max, target, palette, mc);
    }

    /**
     * OP + 无调色板：用 /fill 分批执行。成功排队返回 true。
     */
    private boolean tryFillViaCommands(Mode fillMode, BlockPos min, BlockPos max,
                                       BlockState target, long volume, Minecraft mc) {
        return switch (fillMode) {
            case FILL, CLEAR -> {
                enqueueFillChunks(min, max, target, null, label(fillMode), volume, mc);
                yield true;
            }
            case REPLACE_AIR -> {
                enqueueFillChunks(min, max, target, "replace minecraft:air",
                    label(fillMode), volume, mc);
                yield true;
            }
            case HOLLOW -> {
                enqueueHollow(min, max, target, volume, mc);
                yield true;
            }
            case WALLS -> {
                enqueueWalls(min, max, target, volume, mc);
                yield true;
            }
            case FRAME -> {
                enqueueFrame(min, max, target, volume, mc);
                yield true;
            }
        };
    }

    private void enqueueFillChunks(BlockPos min, BlockPos max, BlockState target,
                                   String fillMode, String label, long volume, Minecraft mc) {
        maybeRecordHistory(label, min, max, volume, mc, (pos, a, b) -> true);

        EditQueue.start(label);
        int[] batches = {0};
        BlockEdits.forEachFillChunk(min, max, BlockEdits.FILL_COMMAND_MAX, (from, to) -> {
            EditQueue.enqueueFill(from, to, target, fillMode);
            batches[0]++;
        });

        if (batches[0] > 1) {
            notifyActionBar("§a" + label + " §7分 §f" + batches[0] + " §7批 fill（共 " + volume + " 格）");
        } else {
            notifyActionBar("§a" + label + " §7已提交 fill（" + volume + " 格）");
        }
    }

    /** 大空心：整区实心 fill + 内部空气 fill；小区直接 hollow。 */
    private void enqueueHollow(BlockPos min, BlockPos max, BlockState target,
                               long volume, Minecraft mc) {
        maybeRecordHistory(label(Mode.HOLLOW), min, max, volume, mc,
            (pos, a, b) -> true);

        EditQueue.start(label(Mode.HOLLOW));
        if (volume <= BlockEdits.FILL_COMMAND_MAX) {
            EditQueue.enqueueFill(min, max, target, "hollow");
            notifyActionBar("§a空心填充 §7已提交 fill hollow（" + volume + " 格）");
            return;
        }

        BlockEdits.forEachFillChunk(min, max, BlockEdits.FILL_COMMAND_MAX,
            (from, to) -> EditQueue.enqueueFill(from, to, target, null));

        int dx = max.getX() - min.getX();
        int dy = max.getY() - min.getY();
        int dz = max.getZ() - min.getZ();
        if (dx >= 2 && dy >= 2 && dz >= 2) {
            BlockPos innerMin = min.offset(1, 1, 1);
            BlockPos innerMax = max.offset(-1, -1, -1);
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockEdits.forEachFillChunk(innerMin, innerMax, BlockEdits.FILL_COMMAND_MAX,
                (from, to) -> EditQueue.enqueueFill(from, to, air, null));
        }
        notifyActionBar("§a空心填充 §7分批 fill（共 " + volume + " 格）");
    }

    private void enqueueWalls(BlockPos min, BlockPos max, BlockState target,
                              long volume, Minecraft mc) {
        maybeRecordHistory(label(Mode.WALLS), min, max, volume, mc,
            (pos, a, b) -> includes(Mode.WALLS, pos, a, b, mc));

        EditQueue.start(label(Mode.WALLS));
        // 南北墙（完整 X/Y）
        BlockEdits.forEachFillChunk(
            new BlockPos(min.getX(), min.getY(), min.getZ()),
            new BlockPos(max.getX(), max.getY(), min.getZ()),
            BlockEdits.FILL_COMMAND_MAX,
            (from, to) -> EditQueue.enqueueFill(from, to, target, null));
        if (max.getZ() != min.getZ()) {
            BlockEdits.forEachFillChunk(
                new BlockPos(min.getX(), min.getY(), max.getZ()),
                new BlockPos(max.getX(), max.getY(), max.getZ()),
                BlockEdits.FILL_COMMAND_MAX,
                (from, to) -> EditQueue.enqueueFill(from, to, target, null));
        }
        // 东西墙（避开已填的南北棱，z 收一格）
        if (max.getZ() - min.getZ() >= 2) {
            BlockEdits.forEachFillChunk(
                new BlockPos(min.getX(), min.getY(), min.getZ() + 1),
                new BlockPos(min.getX(), max.getY(), max.getZ() - 1),
                BlockEdits.FILL_COMMAND_MAX,
                (from, to) -> EditQueue.enqueueFill(from, to, target, null));
            if (max.getX() != min.getX()) {
                BlockEdits.forEachFillChunk(
                    new BlockPos(max.getX(), min.getY(), min.getZ() + 1),
                    new BlockPos(max.getX(), max.getY(), max.getZ() - 1),
                    BlockEdits.FILL_COMMAND_MAX,
                    (from, to) -> EditQueue.enqueueFill(from, to, target, null));
            }
        }
        notifyActionBar("§a墙壁 §7已提交分批 fill");
    }

    private void enqueueFrame(BlockPos min, BlockPos max, BlockState target,
                              long volume, Minecraft mc) {
        maybeRecordHistory(label(Mode.FRAME), min, max, volume, mc,
            (pos, a, b) -> includes(Mode.FRAME, pos, a, b, mc));

        EditQueue.start(label(Mode.FRAME));
        int x0 = min.getX(), x1 = max.getX();
        int y0 = min.getY(), y1 = max.getY();
        int z0 = min.getZ(), z1 = max.getZ();

        // 底面 4 棱 + 顶面 4 棱 + 竖棱 4 条
        enqueueEdge(x0, y0, z0, x1, y0, z0, target);
        enqueueEdge(x0, y0, z1, x1, y0, z1, target);
        enqueueEdge(x0, y0, z0, x0, y0, z1, target);
        enqueueEdge(x1, y0, z0, x1, y0, z1, target);

        enqueueEdge(x0, y1, z0, x1, y1, z0, target);
        enqueueEdge(x0, y1, z1, x1, y1, z1, target);
        enqueueEdge(x0, y1, z0, x0, y1, z1, target);
        enqueueEdge(x1, y1, z0, x1, y1, z1, target);

        if (y1 != y0) {
            enqueueEdge(x0, y0, z0, x0, y1, z0, target);
            enqueueEdge(x1, y0, z0, x1, y1, z0, target);
            enqueueEdge(x0, y0, z1, x0, y1, z1, target);
            enqueueEdge(x1, y0, z1, x1, y1, z1, target);
        }
        notifyActionBar("§a棱框 §7已提交 fill");
    }

    private static void enqueueEdge(int x0, int y0, int z0, int x1, int y1, int z1, BlockState target) {
        BlockEdits.forEachFillChunk(
            new BlockPos(x0, y0, z0), new BlockPos(x1, y1, z1),
            BlockEdits.FILL_COMMAND_MAX,
            (from, to) -> EditQueue.enqueueFill(from, to, target, null));
    }

    @FunctionalInterface
    private interface PosFilter {
        boolean test(BlockPos pos, BlockPos min, BlockPos max);
    }

    private void maybeRecordHistory(String label, BlockPos min, BlockPos max,
                                    long volume, Minecraft mc, PosFilter filter) {
        if (volume > BlockEdits.HISTORY_VOLUME) {
            notifyPlayer("§7选区超过 " + BlockEdits.HISTORY_VOLUME
                + " 格，本次不记录撤销快照（仍可用手动还原）");
            return;
        }
        EditHistory.Step step = EditHistory.begin(label);
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!filter.test(pos, min, max)) continue;
                    step.record(pos, mc.level.getBlockState(pos));
                }
            }
        }
        EditHistory.commit(step);
    }

    private void fillViaQueue(Mode fillMode, BlockPos min, BlockPos max,
                              BlockState target, PaintPalette palette, Minecraft mc) {
        EditHistory.Step step = null;
        long plannedEstimate = BlockEdits.volumeOf(min, max);
        boolean record = plannedEstimate <= BlockEdits.HISTORY_VOLUME;
        if (record) {
            step = EditHistory.begin(label(fillMode));
        } else {
            notifyPlayer("§7选区较大，本次不记录撤销快照");
        }

        EditQueue.start(label(fillMode));
        int planned = 0;
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!includes(fillMode, pos, min, max, mc)) continue;

                    if (step != null) {
                        step.record(pos, mc.level.getBlockState(pos));
                    }
                    planned++;
                    EditQueue.enqueue(pos,
                        palette == null || fillMode == Mode.CLEAR
                            ? target
                            : palette.pick(pos, target));
                }
            }
        }

        if (planned == 0) {
            EditQueue.start("");
            notifyActionBar("§7没有需要修改的方块");
            return;
        }

        if (step != null) {
            EditHistory.commit(step);
        }
        notifyActionBar("§a" + label(fillMode) + " §7排队 §f" + planned + " §7格");
    }

    /** 撤销最近一次区域操作。 */
    public void undo() {
        if (!BlockEdits.canUseCommands()) {
            notifyPlayer("§c撤销需要命令权限（OP 2 级）：客户端无法凭空恢复任意方块状态");
            return;
        }
        if (EditQueue.isBusy()) {
            notifyActionBar("§c还有操作正在进行");
            return;
        }

        EditHistory.Step step = EditHistory.pop();
        if (step == null) {
            notifyActionBar("§7没有可撤销的操作");
            return;
        }

        EditQueue.start("撤销" + step.getLabel());
        step.getBefore().forEach(EditQueue::enqueue);
    }

    private BlockState resolveTarget(Mode fillMode) {
        if (fillMode == Mode.CLEAR) return Blocks.AIR.defaultBlockState();

        ItemStack held = Minecraft.getInstance().player.getMainHandItem();
        if (held.getItem() instanceof BlockItem item) {
            return item.getBlock().defaultBlockState();
        }
        // 调色板自带材质，手上拿什么都无所谓
        return PaintPalette.active() != null ? Blocks.STONE.defaultBlockState() : null;
    }

    private static boolean includes(Mode fillMode, BlockPos pos, BlockPos min, BlockPos max, Minecraft mc) {
        boolean edgeX = pos.getX() == min.getX() || pos.getX() == max.getX();
        boolean edgeY = pos.getY() == min.getY() || pos.getY() == max.getY();
        boolean edgeZ = pos.getZ() == min.getZ() || pos.getZ() == max.getZ();

        return switch (fillMode) {
            case FILL, CLEAR -> true;
            case REPLACE_AIR -> mc.level.getBlockState(pos).isAir();
            case HOLLOW -> edgeX || edgeY || edgeZ;
            case WALLS -> edgeX || edgeZ;
            // 棱：三个方向里至少有两个处于边界
            case FRAME -> (edgeX ? 1 : 0) + (edgeY ? 1 : 0) + (edgeZ ? 1 : 0) >= 2;
        };
    }

    private static String label(Mode fillMode) {
        return switch (fillMode) {
            case FILL -> "填充";
            case HOLLOW -> "空心填充";
            case WALLS -> "墙壁";
            case FRAME -> "棱框";
            case REPLACE_AIR -> "补空气";
            case CLEAR -> "清空";
        };
    }

    @Override
    public String getDetailedDescription() {
        StringBuilder text = new StringBuilder(getTranslatedDescription());
        Selection selection = Selection.shared();
        text.append("\n§7模式: §f").append(mode.getDisplayValue());
        text.append("\n§7选区: §f").append(selection.isComplete() ? selection.describe() : "未选择");
        text.append("\n§7上限: §f").append(BlockEdits.maxVolume())
            .append(" §7（fill 单批 ").append(BlockEdits.FILL_COMMAND_MAX).append("）");
        text.append("\n§7可撤销步数: §f").append(EditHistory.depth());
        if (PaintPalette.active() != null) {
            text.append("\n§a调色板生效中，将按噪声混合材质");
        }
        if (!BlockEdits.canUseCommands()) {
            text.append("\n§e无 OP 权限：只能放置手中的方块，且无法撤销");
        }
        return text.toString();
    }
}
