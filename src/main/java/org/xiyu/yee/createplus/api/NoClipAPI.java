package org.xiyu.yee.createplus.api;

/**
 * 穿墙状态。mixin 热路径只读这里，避免每 tick 扫功能表。
 *
 * {@link #ignoreRubberband} 为 true 时会丢掉服务端拉回包 —— 远端多人服上
 * 没有服务端 mixin，只能靠这个让客户端继续待在墙里。
 */
public final class NoClipAPI {
    private static boolean active;
    private static boolean ignoreRubberband;

    private NoClipAPI() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
        if (!value) ignoreRubberband = false;
    }

    public static boolean ignoreRubberband() {
        return ignoreRubberband;
    }

    public static void setIgnoreRubberband(boolean value) {
        ignoreRubberband = value;
    }
}
