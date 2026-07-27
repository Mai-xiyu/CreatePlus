package org.xiyu.yee.createplus.api;

/** 创造瞬间破坏。 */
public final class FastBreakAPI {
    private static boolean active;

    private FastBreakAPI() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}
