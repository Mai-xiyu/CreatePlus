package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import org.xiyu.yee.createplus.utils.KeyBindings;

public class Zoom extends CreativePlusFeature {
    private boolean isZooming = false;
    private float zoomLevel = 4.0f;
    private int originalFov;
    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 10.0f;
    private static final float ZOOM_STEP = 0.5f;
    private static final float SMOOTH_FACTOR = 0.15f;
    private float currentZoom = 1.0f;

    public Zoom() {
        super("zoom", "允许玩家缩放视角");
    }

    @Override
    public void onEnable() {
        // 不需要特殊初始化
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        
        Minecraft mc = Minecraft.getInstance();

        // 处理缩放开关
        if (KeyBindings.TOGGLE_ZOOM.isDown()) {
            if (!isZooming) {
                // 开始缩放
                isZooming = true;
                originalFov = mc.options.fov().get();
            }
            
            // 处理鼠标滚轮
            double scroll = mc.mouseHandler.getXVelocity();
            if (scroll != 0) {
                float delta = (float) (Math.signum(scroll) * ZOOM_STEP);
                zoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomLevel + delta));
            }
            
            // 平滑过渡
            currentZoom += (zoomLevel - currentZoom) * SMOOTH_FACTOR;
            // 确保FOV值在有效范围内 (30-110)
            int newFov = (int) Math.max(30, Math.min(110, originalFov / currentZoom));
            mc.options.fov().set(newFov);
            
        } else if (isZooming) {
            // 结束缩放
            isZooming = false;
            currentZoom = 1.0f;
            mc.options.fov().set(originalFov);
        }
    }

    @Override
    public void onDisable() {
        if (isZooming) {
            Minecraft mc = Minecraft.getInstance();
            isZooming = false;
            currentZoom = 1.0f;
            mc.options.fov().set(originalFov);
        }
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder(super.getDescription());
        if (isEnabled()) {
            desc.append("\n§7按住 C 进行缩放");
            if (isZooming) {
                desc.append("\n§7当前缩放: §e").append(String.format("%.1fx", zoomLevel));
                desc.append("\n§7使用鼠标滚轮调节缩放");
            }
        }
        return desc.toString();
    }
} 