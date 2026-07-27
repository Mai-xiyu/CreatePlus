package org.xiyu.yee.createplus.api;

/** 第三人称镜头穿墙。 */
public final class CameraClipAPI {
    private static boolean active;

    private CameraClipAPI() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}
