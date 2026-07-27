package org.xiyu.yee.createplus.features;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.DoubleSetting;

/**
 * 创造飞行手感优化：松手立刻停住，不再滑出去一大截。
 */
public class ImprovedFlight extends CreativePlusFeature {
    private static final float VANILLA_FLY_SPEED = 0.05F;
    /** 原版创造飞行的垂直速度上限，用来给垂直加速算一个合理的天花板。 */
    private static final double BASE_VERTICAL_SPEED = 0.5D;

    private final BooleanSetting instantStop = boolSetting("instant_stop", true);
    /** 松手后每 tick 保留的速度比例，1.0 等同原版惯性。 */
    private final DoubleSetting drag = doubleSetting("drag", 0.0D, 0.0D, 0.95D, 0.05D);
    private final DoubleSetting verticalBoost = doubleSetting("vertical_boost", 1.0D, 1.0D, 3.0D, 0.1D);

    public ImprovedFlight() {
        super("improved_flight", "改进创造飞行手感：取消惯性、可调垂直加速", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        applyBaseSpeed();
    }

    @Override
    public void onWorldJoin() {
        applyBaseSpeed();
    }

    private void applyBaseSpeed() {
        LocalPlayer player = player();
        if (player == null) return;
        // 飞行倍率交给"速度调整"统一管理，这里只负责保证不低于原版
        if (player.getAbilities().getFlyingSpeed() < VANILLA_FLY_SPEED) {
            player.getAbilities().setFlyingSpeed(VANILLA_FLY_SPEED);
            player.onUpdateAbilities();
        }
    }

    @Override
    public void onDisable() {
        LocalPlayer player = player();
        if (player != null) {
            player.getAbilities().setFlyingSpeed(VANILLA_FLY_SPEED);
            player.onUpdateAbilities();
        }
    }

    @Override
    public void onTick() {
        LocalPlayer player = player();
        if (player == null || !player.getAbilities().flying) return;

        boolean hasHorizontalInput = player.input.forwardImpulse != 0.0F || player.input.leftImpulse != 0.0F;
        boolean hasVerticalInput = player.input.jumping || player.input.shiftKeyDown;

        if (hasVerticalInput && verticalBoost.value() > 1.0D) {
            Vec3 motion = player.getDeltaMovement();
            // 必须夹住上限：不加限制就是每 tick 乘一遍，几秒后直接冲出世界高度
            double max = BASE_VERTICAL_SPEED * verticalBoost.value();
            double y = Math.max(-max, Math.min(max, motion.y * verticalBoost.value()));
            player.setDeltaMovement(motion.x, y, motion.z);
        }

        if (!hasHorizontalInput && instantStop.get()) {
            Vec3 motion = player.getDeltaMovement();
            double keep = drag.value();
            // 只清水平惯性，垂直方向留给玩家和重力，避免飞行时被卡在半空
            player.setDeltaMovement(motion.x * keep, motion.y, motion.z * keep);
        }
    }
}
