package org.xiyu.yee.createplus.features;

import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.DoubleSetting;
import org.xiyu.yee.createplus.utils.KeyBindings;

/**
 * 按住缩放键放大视角。
 *
 * 通过 FOV 事件临时改变视野，不会像旧实现那样把玩家在视频设置里的 FOV 覆盖掉
 * （那个 bug 在缩放中崩溃或切服时会让 FOV 永久变成 30）。
 */
public class Zoom extends CreativePlusFeature {
    private final DoubleSetting level = doubleSetting("level", 4.0D, 1.5D, 20.0D, 0.5D);
    private final BooleanSetting smooth = boolSetting("smooth", true);
    private final DoubleSetting smoothSpeed = doubleSetting("smooth_speed", 0.25D, 0.05D, 1.0D, 0.05D);
    private final BooleanSetting scrollToAdjust = boolSetting("scroll_to_adjust", true);

    private boolean zooming = false;
    private double currentZoom = 1.0D;

    public Zoom() {
        super("zoom", "按住缩放键放大视角，滚轮调节倍率", Category.RENDER);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        zooming = false;
        currentZoom = 1.0D;
    }

    @Override
    public void onTick() {
        zooming = KeyBindings.TOGGLE_ZOOM.isDown();
    }

    @Override
    public boolean handleScroll(double delta) {
        if (!zooming || !scrollToAdjust.get()) return false;
        level.cycle(delta > 0 ? 1 : -1);
        notifyActionBar(String.format("§b缩放: §f%.1fx", level.value()));
        return true;
    }

    @SubscribeEvent
    public void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) return;

        double target = zooming ? level.value() : 1.0D;
        if (smooth.get()) {
            currentZoom += (target - currentZoom) * smoothSpeed.value();
            if (Math.abs(target - currentZoom) < 0.01D) currentZoom = target;
        } else {
            currentZoom = target;
        }

        if (currentZoom > 1.001D) {
            event.setFOV(event.getFOV() / currentZoom);
        }
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + String.format("\n§7当前倍率: §f%.1fx", level.value())
            + "\n§7在按键设置中可以修改缩放键";
    }
}
