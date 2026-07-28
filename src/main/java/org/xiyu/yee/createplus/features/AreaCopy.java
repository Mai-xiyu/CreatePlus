package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.utils.BlockEdits;
import org.xiyu.yee.createplus.utils.EditHistory;
import org.xiyu.yee.createplus.utils.EditQueue;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.Selection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 复制选区并粘贴到别处，支持旋转与镜像。
 *
 * 用 §f/createplus copy§7 与 §f/createplus paste§7 操作，选区由选区工具提供。
 */
public class AreaCopy extends CreativePlusFeature {

    public enum Anchor {
        /** 粘贴到准星指向的方块。 */
        LOOK,
        /** 粘贴到脚下。 */
        FEET
    }

    private final EnumSetting<Anchor> anchor = enumSetting("anchor", Anchor.LOOK);
    private final EnumSetting<Rotation> rotation = enumSetting("rotation", Rotation.NONE);
    private final EnumSetting<Mirror> mirror = enumSetting("mirror", Mirror.NONE);
    private final BooleanSetting includeAir = boolSetting("include_air", false);

    /** 相对于选区最小角的方块快照。 */
    private final Map<BlockPos, BlockState> clipboard = new LinkedHashMap<>();
    private int sizeX;
    private int sizeY;
    private int sizeZ;

    public AreaCopy() {
        super("area_copy", "复制选区内的建筑并粘贴到别处，可旋转镜像", Category.BUILDING);
    }

    @Override
    public void onDisable() {
        clipboard.clear();
    }

    public int clipboardSize() {
        return clipboard.size();
    }

    /** Ctrl + 滚轮切换粘贴旋转角度。 */
    @Override
    public boolean handleScroll(double delta) {
        if (clipboard.isEmpty() || !KeyBindings.modifierDown()) return false;
        rotation.cycle(delta > 0 ? 1 : -1);
        notifyActionBar("§b粘贴旋转: §f" + rotation.getDisplayValue());
        return true;
    }

    public void copy() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Selection selection = Selection.shared();
        if (!selection.isComplete()) {
            notifyActionBar("§c请先用选区工具选择一个区域");
            return;
        }
        if (selection.volume() > BlockEdits.maxVolume()) {
            notifyActionBar("§c选区太大了（" + selection.volume() + "）");
            return;
        }

        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();
        clipboard.clear();
        sizeX = selection.width();
        sizeY = selection.height();
        sizeZ = selection.depth();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.isAir() && !includeAir.get()) continue;
                    clipboard.put(pos.subtract(min), state);
                }
            }
        }

        notifyPlayer(String.format("§a已复制 §f%d §a个方块 §7(%dx%dx%d)",
            clipboard.size(), sizeX, sizeY, sizeZ));
    }

    public void paste() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (clipboard.isEmpty()) {
            notifyActionBar("§c剪贴板是空的，先执行 /createplus copy");
            return;
        }
        if (EditQueue.isBusy()) {
            notifyActionBar("§c上一次操作还没做完");
            return;
        }

        BlockPos origin = resolveOrigin(mc);
        if (origin == null) {
            notifyActionBar("§c请先看向一个方块");
            return;
        }

        Rotation rot = rotation.get();
        Mirror mir = mirror.get();

        EditHistory.Step step = EditHistory.begin("粘贴");
        EditQueue.start("粘贴");

        for (Map.Entry<BlockPos, BlockState> entry : clipboard.entrySet()) {
            BlockPos offset = transform(entry.getKey(), rot, mir);
            BlockPos target = origin.offset(offset);
            BlockState state = entry.getValue().mirror(mir).rotate(rot);

            step.record(target, mc.level.getBlockState(target));
            EditQueue.enqueue(target, state);
        }

        EditHistory.commit(step);
        notifyActionBar("§7开始粘贴 §f" + clipboard.size() + " §7个方块");
    }

    /**
     * 沿某个方向把选区整体重复若干次，相当于 WorldEdit 的 //stack。
     *
     * 走廊、柱廊、围墙这类等距重复的结构，用复制粘贴要对一次准星贴一次，
     * 这里一条命令就能贴完，间距永远等于选区尺寸所以不会错位。
     *
     * @param direction 重复方向，传 null 表示用玩家当前的朝向
     */
    public void stack(int count, Direction direction) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Selection selection = Selection.shared();
        if (!selection.isComplete()) {
            notifyActionBar("§c请先用选区工具选择一个区域");
            return;
        }
        if (EditQueue.isBusy()) {
            notifyActionBar("§c上一次操作还没做完");
            return;
        }

        long volume = selection.volume() * count;
        if (volume > BlockEdits.maxVolume()) {
            notifyActionBar("§c阵列总量太大了（" + volume + "）");
            return;
        }

        Direction axis = direction != null ? direction : Direction.getNearest(
            mc.player.getLookAngle().x, mc.player.getLookAngle().y, mc.player.getLookAngle().z);
        int span = switch (axis.getAxis()) {
            case X -> selection.width();
            case Y -> selection.height();
            case Z -> selection.depth();
        };

        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();
        EditHistory.Step step = EditHistory.begin("阵列复制");
        EditQueue.start("阵列复制");

        int queued = 0;
        for (int copy = 1; copy <= count; copy++) {
            int shift = span * copy;
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int y = min.getY(); y <= max.getY(); y++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        BlockState state = mc.level.getBlockState(new BlockPos(x, y, z));
                        if (state.isAir() && !includeAir.get()) continue;

                        BlockPos target = new BlockPos(
                            x + axis.getStepX() * shift,
                            y + axis.getStepY() * shift,
                            z + axis.getStepZ() * shift);
                        step.record(target, mc.level.getBlockState(target));
                        EditQueue.enqueue(target, state);
                        queued++;
                    }
                }
            }
        }

        EditHistory.commit(step);
        notifyPlayer(String.format("§a阵列 §f%d §a份 · §f%d §a个方块 §7(方向 %s)",
            count, queued, axis.getName()));
    }

    /**
     * 把相对坐标按旋转/镜像变换。
     *
     * 旋转是绕 Y 轴的，尺寸也要跟着换，否则 90° 之后建筑会跑到选区外面。
     */
    private BlockPos transform(BlockPos offset, Rotation rot, Mirror mir) {
        int x = offset.getX();
        int z = offset.getZ();

        if (mir == Mirror.FRONT_BACK) {
            x = sizeX - 1 - x;
        } else if (mir == Mirror.LEFT_RIGHT) {
            z = sizeZ - 1 - z;
        }

        return switch (rot) {
            case NONE -> new BlockPos(x, offset.getY(), z);
            case CLOCKWISE_90 -> new BlockPos(sizeZ - 1 - z, offset.getY(), x);
            case CLOCKWISE_180 -> new BlockPos(sizeX - 1 - x, offset.getY(), sizeZ - 1 - z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, offset.getY(), sizeX - 1 - x);
        };
    }

    private BlockPos resolveOrigin(Minecraft mc) {
        if (anchor.get() == Anchor.FEET) {
            return mc.player.blockPosition();
        }
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult) mc.hitResult;
            return hit.getBlockPos().relative(hit.getDirection());
        }
        return null;
    }

    @Override
    public String getDetailedDescription() {
        StringBuilder text = new StringBuilder(getTranslatedDescription());
        text.append("\n§7/createplus copy §8· §7/createplus paste §8· §7/createplus stack <份数>");
        if (clipboard.isEmpty()) {
            text.append("\n§7剪贴板: 空");
        } else {
            text.append(String.format("\n§7剪贴板: §f%d §7个方块 (%dx%dx%d)",
                clipboard.size(), sizeX, sizeY, sizeZ));
        }
        text.append("\n§7旋转: §f").append(rotation.getDisplayValue())
            .append(" §7镜像: §f").append(mirror.getDisplayValue());
        text.append("\n§7Ctrl + 滚轮切换旋转角度");
        return text.toString();
    }
}
