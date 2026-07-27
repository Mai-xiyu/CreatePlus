package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xiyu.yee.createplus.api.FreecamAPI;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.DoubleSetting;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.RenderUtils;

/**
 * 灵魂出窍：镜头脱离身体自由飞行，服务端看到的玩家位置保持不动。
 *
 * 做法是让本地玩家临时被当作旁观者（LocalPlayerMixin 让 isSpectator 返回 true，
 * 于是原版每刻自动置上 noPhysics）并拦截移动数据包（ConnectionMixin），
 * 退出时把玩家放回原位。全程不修改 gameMode，避免影响原版逻辑。
 */
public class Freecam extends CreativePlusFeature implements WorldRenderFeature {
    private final DoubleSetting flySpeed = doubleSetting("fly_speed", 1.0D, 0.1D, 10.0D, 0.1D);
    private final BooleanSetting showBody = boolSetting("show_body", true);
    private final BooleanSetting scrollToAdjust = boolSetting("scroll_to_adjust", true);

    private Vec3 anchorPosition;
    private float anchorYRot;
    private float anchorXRot;
    private boolean previousMayFly;
    private boolean previousFlying;

    public Freecam() {
        super("freecam", "镜头脱离身体自由观察，按 F6 开关", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            // 退出世界时清理状态，否则下次进游戏会带着一个坏掉的 freecam 标记
            if (FreecamAPI.isFreecam()) FreecamAPI.reset();
            return;
        }

        while (KeyBindings.TOGGLE_FREECAM.consumeClick()) {
            toggleFreecam();
        }

        if (FreecamAPI.isFreecam()) {
            // 每刻都写一遍：服务端随时可能发能力数据包把这些值改回去
            mc.player.getAbilities().setFlyingSpeed((float) (0.05D * flySpeed.value()));
            mc.player.getAbilities().mayfly = true;
            mc.player.getAbilities().flying = true;
        }
    }

    @Override
    public boolean handleScroll(double delta) {
        if (!FreecamAPI.isFreecam() || !scrollToAdjust.get()) return false;
        flySpeed.cycle(delta > 0 ? 1 : -1);
        notifyActionBar(String.format("§b灵魂出窍速度: §f%.1fx", flySpeed.value()));
        return true;
    }

    public void toggleFreecam() {
        if (FreecamAPI.isFreecam()) {
            stop();
        } else {
            start();
        }
    }

    private void start() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        anchorPosition = player.position();
        anchorYRot = player.getYRot();
        anchorXRot = player.getXRot();
        previousMayFly = player.getAbilities().mayfly;
        previousFlying = player.getAbilities().flying;

        FreecamAPI.setFreecamEnabled(true);

        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.setDeltaMovement(Vec3.ZERO);

        notifyActionBar("§b灵魂出窍已开启 §7(再按一次返回身体)");
    }

    private void stop() {
        LocalPlayer player = Minecraft.getInstance().player;

        // 先落下标记再挪玩家：isSpectator 还是 true 的话，
        // 原版会把 noPhysics 保持开启，玩家可能停在方块里。
        FreecamAPI.setFreecamEnabled(false);

        if (player != null) {
            if (anchorPosition != null) {
                player.setPos(anchorPosition.x, anchorPosition.y, anchorPosition.z);
                player.setYRot(anchorYRot);
                player.setXRot(anchorXRot);
            }
            player.setDeltaMovement(Vec3.ZERO);
            player.getAbilities().mayfly = previousMayFly;
            player.getAbilities().flying = previousFlying;
            player.getAbilities().setFlyingSpeed(0.05F);
        }

        notifyActionBar("§7已返回身体");
    }

    @Override
    public void onDisable() {
        if (FreecamAPI.isFreecam()) {
            stop();
        }
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (!FreecamAPI.isFreecam() || !showBody.get() || anchorPosition == null) return;

        // 在身体留下的位置画一个轮廓，方便知道自己"实际"站在哪
        AABB body = new AABB(
            anchorPosition.x - 0.3D, anchorPosition.y, anchorPosition.z - 0.3D,
            anchorPosition.x + 0.3D, anchorPosition.y + 1.8D, anchorPosition.z + 0.3D);

        RenderUtils.drawWorldFilledBox(poseStack, camera, body, 0.0F, 0.85F, 1.0F, 0.2F);
        RenderUtils.drawWorldBox(poseStack, camera, body, 0.0F, 0.85F, 1.0F, 0.9F);
        RenderUtils.drawWorldLabel(poseStack, camera, "§b身体",
            anchorPosition.x, anchorPosition.y + 2.1D, anchorPosition.z, 0xFFFFFFFF);
    }

    @Override
    public String getDetailedDescription() {
        StringBuilder text = new StringBuilder(getTranslatedDescription());
        text.append("\n§7按 F6 开关，WASD + 空格/潜行移动");
        text.append(String.format("\n§7飞行速度: §f%.1fx §7(滚轮调节)", flySpeed.value()));
        if (FreecamAPI.isFreecam()) {
            text.append("\n§a当前: 已脱离身体");
        }
        return text.toString();
    }
}
