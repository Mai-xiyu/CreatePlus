package org.xiyu.yee.createplus.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 * 一个长方体选区。
 *
 * 全局共享一份（{@link #shared()}），这样"选区工具"选好之后，
 * 复制、导出、填充等功能都能直接用，不用各选一遍。
 */
public class Selection {
    private static final Selection SHARED = new Selection();

    private BlockPos first;
    private BlockPos second;

    public static Selection shared() {
        return SHARED;
    }

    public BlockPos getFirst() {
        return first;
    }

    public BlockPos getSecond() {
        return second;
    }

    public void setFirst(BlockPos pos) {
        this.first = pos;
    }

    public void setSecond(BlockPos pos) {
        this.second = pos;
    }

    public void clear() {
        first = null;
        second = null;
    }

    public boolean isComplete() {
        return first != null && second != null;
    }

    public boolean isEmpty() {
        return first == null && second == null;
    }

    public BlockPos getMin() {
        return isComplete() ? BlockEdits.min(first, second) : first;
    }

    public BlockPos getMax() {
        return isComplete() ? BlockEdits.max(first, second) : first;
    }

    public long volume() {
        return isComplete() ? BlockEdits.volumeOf(first, second) : (first != null ? 1L : 0L);
    }

    public int width() {
        return isComplete() ? Math.abs(first.getX() - second.getX()) + 1 : 0;
    }

    public int height() {
        return isComplete() ? Math.abs(first.getY() - second.getY()) + 1 : 0;
    }

    public int depth() {
        return isComplete() ? Math.abs(first.getZ() - second.getZ()) + 1 : 0;
    }

    /** 覆盖整个选区的包围盒，坐标已经扩展到方块外沿。 */
    public AABB getBox() {
        if (!isComplete()) {
            if (first == null) return null;
            return new AABB(first);
        }
        BlockPos min = getMin();
        BlockPos max = getMax();
        return new AABB(min.getX(), min.getY(), min.getZ(),
            max.getX() + 1.0D, max.getY() + 1.0D, max.getZ() + 1.0D);
    }

    public String describe() {
        if (!isComplete()) return "-";
        return width() + "x" + height() + "x" + depth() + " (" + volume() + ")";
    }
}
