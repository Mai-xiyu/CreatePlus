package org.xiyu.yee.createplus.api;

import net.minecraft.world.phys.Vec3;

public class FreecamAPI {
    private static boolean isFreecam = false;
    private static Vec3 freecamPos = null;
    private static float freecamYRot = 0;
    private static float freecamXRot = 0;

    public static void setFreecamEnabled(boolean enabled) {
        isFreecam = enabled;
    }

    public static void setFreecamPosition(Vec3 pos) {
        freecamPos = pos;
    }

    public static void setFreecamRotation(float yRot, float xRot) {
        freecamYRot = yRot;
        freecamXRot = xRot;
    }

    public static boolean isFreecam() {
        return isFreecam;
    }

    public static Vec3 getFreecamPos() {
        return freecamPos;
    }

    public static float getFreecamYRot() {
        return freecamYRot;
    }

    public static float getFreecamXRot() {
        return freecamXRot;
    }
} 