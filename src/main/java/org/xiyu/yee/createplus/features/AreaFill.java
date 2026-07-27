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
 * 这是"选区工具"之后最常用的一步：Litematica / WorldEdit 的 //set 与 //walls。
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

    public AreaFill() {
        super("area_fill", "用手中的方块填充选区，支持空心/墙壁/棱框与撤销", Category.BUILDING);
        perTick.onChange(EditQueue::setPerTick);
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

    /** 按键触发的填充。 */
    public void fillFromKeybind() {
        fill(mode.get());
    }

    public void fill(Mode fillMode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (EditQueue.isBusy()) {
            notifyActionBar("§c上一次操作还没做完，剩余 " + EditQueue.remaining() + " 个方块");
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
        if (volume > BlockEdits.MAX_VOLUME) {
            notifyActionBar("§c选区太大了（" + volume + " > " + BlockEdits.MAX_VOLUME + "）");
            return;
        }

        // 调色板要逐格算材质，用不了单条 fill
        PaintPalette palette = PaintPalette.active();
        // 有权限而且是最简单的整体填充时，一条 fill 命令就够，比逐格发包快几个数量级
        boolean singleCommand = fillMode == Mode.FILL && palette == null && BlockEdits.canUseCommands();

        EditHistory.Step step = EditHistory.begin(label(fillMode));
        EditQueue.start(label(fillMode));

        int planned = 0;
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!includes(fillMode, pos, min, max, mc)) continue;

                    step.record(pos, mc.level.getBlockState(pos));
                    planned++;
                    if (!singleCommand) {
                        EditQueue.enqueue(pos,
                            palette == null || fillMode == Mode.CLEAR
                                ? target
                                : palette.pick(pos, target));
                    }
                }
            }
        }

        if (planned == 0) {
            notifyActionBar("§7没有需要修改的方块");
            return;
        }

        EditHistory.commit(step);

        if (singleCommand) {
            BlockEdits.fillViaCommand(min, max, target, null);
            notifyActionBar("§a" + label(fillMode) + " 完成 §f" + planned + " §7个方块");
        }
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
