package org.xiyu.yee.createplus.api;

/**
 * 灵魂出窍的共享状态。
 *
 * 单独放在这里是因为多个 mixin（移动数据包拦截、旁观判定、飞行判定）
 * 都需要读它，而 mixin 不方便直接持有功能实例。
 */
public final class FreecamAPI {
    private static boolean freecam;

    private FreecamAPI() {
    }

    public static boolean isFreecam() {
        return freecam;
    }

    public static void setFreecamEnabled(boolean enabled) {
        freecam = enabled;
    }

    public static void reset() {
        freecam = false;
    }
}
