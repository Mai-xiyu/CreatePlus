package org.xiyu.yee.createplus.utils;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** 范围工具用的几种笔刷形状。 */
public final class Shapes {

    public enum Shape {
        /** 立方体。 */
        CUBE,
        /** 球体。 */
        SPHERE,
        /** 水平圆盘。 */
        DISC,
        /** 十字。 */
        CROSS
    }

    private Shapes() {
    }

    /** 收集形状覆盖的所有坐标，按到中心的距离排序（先近后远，放置时观感更自然）。 */
    public static List<BlockPos> collect(Shape shape, BlockPos center, int radius) {
        List<BlockPos> result = new ArrayList<>();
        int r = Math.max(0, radius);
        int rSq = r * r;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (!inside(shape, dx, dy, dz, r, rSq)) continue;
                    result.add(center.offset(dx, dy, dz));
                }
            }
        }

        result.sort((a, b) -> Double.compare(a.distSqr(center), b.distSqr(center)));
        return result;
    }

    private static boolean inside(Shape shape, int dx, int dy, int dz, int r, int rSq) {
        return switch (shape) {
            case CUBE -> true;
            case SPHERE -> dx * dx + dy * dy + dz * dz <= rSq;
            case DISC -> dy == 0 && dx * dx + dz * dz <= rSq;
            case CROSS -> (dx == 0 && dy == 0) || (dx == 0 && dz == 0) || (dy == 0 && dz == 0);
        };
    }
}
