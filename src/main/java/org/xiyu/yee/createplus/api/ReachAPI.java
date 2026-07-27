package org.xiyu.yee.createplus.api;

/**
 * 超距交互状态。GameMode / 选取 mixin 读这里。
 */
public final class ReachAPI {
    private static boolean active;
    private static double blockReach = 4.5D;
    private static double entityReach = 3.0D;

    private ReachAPI() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static double blockReach() {
        return blockReach;
    }

    public static double entityReach() {
        return entityReach;
    }

    public static void setReach(double block, double entity) {
        blockReach = Math.max(0.0D, block);
        entityReach = Math.max(0.0D, entity);
    }
}
