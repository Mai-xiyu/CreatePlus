package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.api.FreecamAPI;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.IntSetting;

/**
 * 把视角吸附到固定角度上。
 *
 * 摆楼梯、栅栏、告示牌这些带朝向的方块时，朝向取决于放置瞬间的偏航角，
 * 手抖一下整排就歪了。吸附之后视角只会停在 45° 的整数倍上，
 * 一整面墙的朝向必然一致。
 *
 * 挂在 {@code RenderTickEvent.START} 上而不是客户端 tick：鼠标转向发生在
 * {@code Minecraft.runTick} 里 tick 循环之后，在 tick 末尾吸附会立刻被
 * 鼠标覆盖掉，画面上看不到任何效果。
 */
public class SnapAim extends CreativePlusFeature {

    private final IntSetting yawStep = intSetting("yaw_step", 45, 5, 90, 5, "°");
    private final BooleanSetting snapPitch = boolSetting("snap_pitch", true);
    private final IntSetting pitchStep = intSetting("pitch_step", 45, 5, 90, 5, "°");
    private final BooleanSetting freeWhileSneaking = boolSetting("free_while_sneaking", true);

    public SnapAim() {
        super("snap_aim", "把视角吸附到固定角度，摆放带朝向的方块时不会歪", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        notifyPlayer("§b[CreatePlus] §7视角吸附已启用：每 §f" + yawStep.value()
            + "° §7一格" + (freeWhileSneaking.get() ? "，按住潜行可自由转视角" : ""));
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;
        // 灵魂出窍时镜头就是玩家实体，吸附会让人没法自由观察建筑
        if (FreecamAPI.isFreecam()) return;
        if (freeWhileSneaking.get() && player.isShiftKeyDown()) return;

        // 上一刻的角度要一起改，否则渲染会在吸附前后的角度之间插值，看着一直在抖
        float yaw = snap(player.getYRot(), yawStep.value());
        player.setYRot(yaw);
        player.yRotO = yaw;

        if (snapPitch.get()) {
            float pitch = Mth.clamp(snap(player.getXRot(), pitchStep.value()), -90.0F, 90.0F);
            player.setXRot(pitch);
            player.xRotO = pitch;
        }
    }

    private static float snap(float angle, int step) {
        return Math.round(angle / step) * (float) step;
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7水平: 每 §f" + yawStep.value() + "° §7一格"
            + "\n§7俯仰: §f" + (snapPitch.get() ? "每 " + pitchStep.value() + "° 一格" : "不吸附")
            + (freeWhileSneaking.get() ? "\n§7按住潜行可临时自由转视角" : "");
    }
}
