package org.xiyu.yee.createplus.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 参数化体素生成。
 *
 * 每个方法返回的都是相对于原点的偏移量，锚点由调用方决定 —— 预览要跟着准星
 * 每帧移动，如果生成的是绝对坐标就得每帧重算几万个体素。
 *
 * 半径统一按 {@code r + 0.5} 归一化：直接用 {@code x²+y²+z² <= r²} 的话，
 * 赤道上正好落在半径处的那一圈会被判掉，球看起来是瘪的。
 */
public final class Geometry {

    /** 采样类曲线（螺旋、样条）每步前进的距离，小于半格才不会在拐弯处漏点。 */
    private static final double CURVE_STEP = 0.4D;

    /**
     * 生成量上限（跟全局可调上限走）。
     *
     * 半径 64 的实心球有一百多万个体素，光是装进列表就够卡一下。
     * 超了直接返回空列表，让调用方报错 —— 截断一半的形状比什么都不画更让人困惑。
     */
    public static int maxVoxels() {
        return BlockEdits.maxVolume();
    }

    private Geometry() {
    }

    // ==================================================================
    // 封闭体
    // ==================================================================

    public static List<BlockPos> sphere(int radius, boolean hollow, int thickness) {
        return ellipsoid(radius, radius, radius, hollow, thickness);
    }

    public static List<BlockPos> ellipsoid(int rx, int ry, int rz, boolean hollow, int thickness) {
        List<BlockPos> out = new ArrayList<>();
        double ax = rx + 0.5D, ay = ry + 0.5D, az = rz + 0.5D;
        double bx = rx - thickness + 0.5D, by = ry - thickness + 0.5D, bz = rz - thickness + 0.5D;
        boolean canHollow = hollow && bx > 0.0D && by > 0.0D && bz > 0.0D;

        for (int x = -rx; x <= rx; x++) {
            for (int y = -ry; y <= ry; y++) {
                for (int z = -rz; z <= rz; z++) {
                    if (norm(x, y, z, ax, ay, az) > 1.0D) continue;
                    if (canHollow && norm(x, y, z, bx, by, bz) <= 1.0D) continue;
                    if (out.size() >= maxVoxels()) return List.of();
                    out.add(new BlockPos(x, y, z));
                }
            }
        }
        return out;
    }

    /** 底面在 y=0 的半球。 */
    public static List<BlockPos> dome(int radius, boolean hollow, int thickness) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos pos : sphere(radius, hollow, thickness)) {
            if (pos.getY() >= 0) out.add(pos);
        }
        return out;
    }

    /**
     * 底面在 y=0 的圆柱，沿指定轴延伸。
     *
     * 空心时只留侧壁不留顶底 —— 塔楼和烟囱要的是筒，带盖的话还得自己再挖一次。
     */
    public static List<BlockPos> cylinder(int radius, int height, Direction.Axis axis,
                                          boolean hollow, int thickness) {
        List<BlockPos> out = new ArrayList<>();
        double outer = radius + 0.5D;
        double inner = radius - thickness + 0.5D;
        boolean canHollow = hollow && inner > 0.0D;

        for (int along = 0; along < height; along++) {
            for (int u = -radius; u <= radius; u++) {
                for (int v = -radius; v <= radius; v++) {
                    if (norm2(u, v, outer, outer) > 1.0D) continue;
                    if (canHollow && norm2(u, v, inner, inner) <= 1.0D) continue;
                    if (out.size() >= maxVoxels()) return List.of();
                    out.add(fromAxis(axis, along, u, v));
                }
            }
        }
        return out;
    }

    /**
     * 底面在 y=0、尖端朝上的圆锥。空心时只留斜面。
     *
     * 接近尖端时整层已经比壁厚还细，这时候再掏空会在塔尖上戳出一个洞，
     * 所以窄到一定程度就整层填实。
     */
    public static List<BlockPos> cone(int radius, int height, boolean hollow, int thickness) {
        List<BlockPos> out = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            double shrink = 1.0D - (double) y / height;
            double outer = radius * shrink + 0.5D;
            double inner = outer - thickness;
            boolean hollowLayer = hollow && radius * shrink > thickness;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (norm2(x, z, outer, outer) > 1.0D) continue;
                    if (hollowLayer && norm2(x, z, inner, inner) <= 1.0D) continue;
                    if (out.size() >= maxVoxels()) return List.of();
                    out.add(new BlockPos(x, y, z));
                }
            }
        }
        return out;
    }

    /** 底面在 y=0 的方底棱锥。空心时只留四个斜面，尖端同样填实。 */
    public static List<BlockPos> pyramid(int halfBase, int height, boolean hollow, int thickness) {
        List<BlockPos> out = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            int outer = (int) Math.round(halfBase * (1.0D - (double) y / height));
            int inner = outer - thickness;
            boolean hollowLayer = hollow && outer > thickness;

            for (int x = -outer; x <= outer; x++) {
                for (int z = -outer; z <= outer; z++) {
                    if (hollowLayer && Math.abs(x) <= inner && Math.abs(z) <= inner) continue;
                    if (out.size() >= maxVoxels()) return List.of();
                    out.add(new BlockPos(x, y, z));
                }
            }
        }
        return out;
    }

    /**
     * 圆环（甜甜圈）。
     *
     * @param major 环心到管心的距离
     * @param minor 管子自身的半径
     */
    public static List<BlockPos> torus(int major, int minor, Direction.Axis axis) {
        List<BlockPos> out = new ArrayList<>();
        int span = major + minor;
        double tube = minor + 0.5D;

        for (int u = -span; u <= span; u++) {
            for (int v = -span; v <= span; v++) {
                double ring = Math.sqrt((double) u * u + (double) v * v) - major;
                for (int along = -minor; along <= minor; along++) {
                    if (ring * ring + (double) along * along > tube * tube) continue;
                    if (out.size() >= maxVoxels()) return List.of();
                    out.add(fromAxis(axis, along, u, v));
                }
            }
        }
        return out;
    }

    /**
     * 立在 y=0 上的椭圆拱门。
     *
     * 拱圈是椭圆环带，两条腿是环带和地面相交自然形成的，不用单独拼。
     *
     * @param span  拱门总跨度
     * @param depth 沿垂直方向的厚度（墙有多厚）
     */
    public static List<BlockPos> arch(int span, int height, int depth, int thickness) {
        List<BlockPos> out = new ArrayList<>();
        double a = span / 2.0D + 0.5D;
        double b = height + 0.5D;
        double ia = a - thickness;
        double ib = b - thickness;
        int half = span / 2;

        for (int x = -half; x <= half; x++) {
            for (int y = 0; y <= height; y++) {
                if (norm2(x, y, a, b) > 1.0D) continue;
                if (ia > 0.0D && ib > 0.0D && norm2(x, y, ia, ib) <= 1.0D) continue;
                for (int z = 0; z < depth; z++) {
                    if (out.size() >= maxVoxels()) return List.of();
                    out.add(new BlockPos(x, y, z));
                }
            }
        }
        return out;
    }

    /**
     * 螺旋线，底端在 y=0。
     *
     * 沿曲线逐点盖球。thickness=1 时是一条细藤蔓/扶手，再大就是旋转坡道。
     * 传给 {@link #tube} 时减半格，和样条曲线的粗细语义对齐：整数 1 正好是
     * 一格直径，而不是半径 1（直径 3）的粗管子。
     *
     * @param turns 圈数，可以是小数
     */
    public static List<BlockPos> helix(int radius, int height, double turns, int thickness) {
        List<Vec3> path = new ArrayList<>();
        double totalAngle = turns * Math.PI * 2.0D;
        // 弧长决定采样数：半径越大、圈数越多，需要的点越密
        double arc = Math.hypot(radius * totalAngle, height);
        int steps = Math.max(2, (int) Math.ceil(arc / CURVE_STEP));

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double angle = t * totalAngle;
            path.add(new Vec3(
                Math.cos(angle) * radius,
                t * height,
                Math.sin(angle) * radius));
        }
        return tube(path, Math.max(0.5D, thickness - 0.5D));
    }

    // ==================================================================
    // 曲线
    // ==================================================================

    /** 沿一串采样点盖球，得到一根有粗细的管子。 */
    public static List<BlockPos> tube(List<Vec3> path, double radius) {
        Set<BlockPos> out = new LinkedHashSet<>();
        int r = (int) Math.ceil(radius);
        double rr = (radius + 0.35D) * (radius + 0.35D);

        for (Vec3 point : path) {
            int cx = (int) Math.floor(point.x);
            int cy = (int) Math.floor(point.y);
            int cz = (int) Math.floor(point.z);
            for (int x = cx - r; x <= cx + r; x++) {
                for (int y = cy - r; y <= cy + r; y++) {
                    for (int z = cz - r; z <= cz + r; z++) {
                        double dx = x + 0.5D - point.x;
                        double dy = y + 0.5D - point.y;
                        double dz = z + 0.5D - point.z;
                        if (dx * dx + dy * dy + dz * dz <= rr) {
                            if (out.size() >= maxVoxels()) return List.of();
                            out.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
        }
        return new ArrayList<>(out);
    }

    /**
     * 穿过所有控制点的 Catmull-Rom 样条。
     *
     * 用 Catmull-Rom 而不是贝塞尔：它保证曲线经过每一个控制点，
     * 玩家点在哪路就从哪过，所见即所得。
     *
     * @param tension 0 = 最平滑，越大转折越锐
     * @param closed  首尾相接成环
     */
    public static List<Vec3> spline(List<Vec3> points, double tension, boolean closed) {
        List<Vec3> out = new ArrayList<>();
        int n = points.size();
        if (n < 2) return new ArrayList<>(points);

        double alpha = (1.0D - Math.max(0.0D, Math.min(1.0D, tension))) * 0.5D;
        int segments = closed ? n : n - 1;

        for (int i = 0; i < segments; i++) {
            Vec3 p0 = points.get(wrap(i - 1, n, closed));
            Vec3 p1 = points.get(wrap(i, n, closed));
            Vec3 p2 = points.get(wrap(i + 1, n, closed));
            Vec3 p3 = points.get(wrap(i + 2, n, closed));

            int steps = Math.max(2, (int) Math.ceil(p1.distanceTo(p2) / CURVE_STEP));
            for (int s = 0; s < steps; s++) {
                out.add(catmullRom(p0, p1, p2, p3, (double) s / steps, alpha));
            }
        }
        out.add(closed ? points.get(0) : points.get(n - 1));
        return out;
    }

    private static int wrap(int index, int size, boolean closed) {
        if (closed) return Math.floorMod(index, size);
        return Math.max(0, Math.min(size - 1, index));
    }

    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t, double alpha) {
        double t2 = t * t;
        double t3 = t2 * t;

        double m0 = -alpha * t3 + 2.0D * alpha * t2 - alpha * t;
        double m1 = (2.0D - alpha) * t3 + (alpha - 3.0D) * t2 + 1.0D;
        double m2 = (alpha - 2.0D) * t3 + (3.0D - 2.0D * alpha) * t2 + alpha * t;
        double m3 = alpha * t3 - alpha * t2;

        return new Vec3(
            p0.x * m0 + p1.x * m1 + p2.x * m2 + p3.x * m3,
            p0.y * m0 + p1.y * m1 + p2.y * m2 + p3.y * m3,
            p0.z * m0 + p1.z * m1 + p2.z * m2 + p3.z * m3);
    }

    // ==================================================================

    private static double norm(int x, int y, int z, double ax, double ay, double az) {
        double fx = x / ax, fy = y / ay, fz = z / az;
        return fx * fx + fy * fy + fz * fz;
    }

    private static double norm2(int u, int v, double au, double av) {
        double fu = u / au, fv = v / av;
        return fu * fu + fv * fv;
    }

    /** 把"沿轴距离 + 截面坐标"还原成实际偏移。 */
    private static BlockPos fromAxis(Direction.Axis axis, int along, int u, int v) {
        return switch (axis) {
            case X -> new BlockPos(along, u, v);
            case Y -> new BlockPos(u, along, v);
            case Z -> new BlockPos(u, v, along);
        };
    }
}
