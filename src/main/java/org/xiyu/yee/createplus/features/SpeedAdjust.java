package org.xiyu.yee.createplus.features;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.xiyu.yee.createplus.settings.DoubleSetting;

/**
 * 各类移动速度倍率。
 *
 * 行走和飞行走玩家能力/属性，游泳、鞘翅、骑乘走速度向量。
 * 向量类的会做上限截断 —— 旧实现是每 tick 直接乘一遍倍率，
 * 几秒之内速度就会指数级爆炸然后把玩家甩出世界。
 */
public class SpeedAdjust extends CreativePlusFeature {
    private static final double VANILLA_WALK = 0.1D;
    private static final float VANILLA_FLY = 0.05F;

    /** 各模式的基准水平速度，乘上倍率后作为速度上限。 */
    private static final double SWIM_BASE = 0.30D;
    private static final double ELYTRA_BASE = 1.20D;
    private static final double RIDE_BASE = 0.40D;

    private final DoubleSetting walk = doubleSetting("walk", 1.0D, 0.1D, 10.0D, 0.1D);
    private final DoubleSetting fly = doubleSetting("fly", 1.0D, 0.1D, 10.0D, 0.1D);
    private final DoubleSetting swim = doubleSetting("swim", 1.0D, 0.1D, 5.0D, 0.1D);
    private final DoubleSetting elytra = doubleSetting("elytra", 1.0D, 0.1D, 5.0D, 0.1D);
    private final DoubleSetting ride = doubleSetting("ride", 1.0D, 0.1D, 5.0D, 0.1D);

    public SpeedAdjust() {
        super("speed_adjust", "自定义行走、飞行、游泳、鞘翅与骑乘速度", Category.MOVEMENT);
        // 数值一改就立刻生效，不用重开功能
        walk.onChange(value -> applyAbilities());
        fly.onChange(value -> applyAbilities());
    }

    @Override
    public void onEnable() {
        applyAbilities();
    }

    @Override
    public void onWorldJoin() {
        applyAbilities();
    }

    @Override
    public void onDisable() {
        LocalPlayer player = player();
        if (player == null) return;
        player.getAbilities().setWalkingSpeed((float) VANILLA_WALK);
        player.getAbilities().setFlyingSpeed(VANILLA_FLY);
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) movement.setBaseValue(VANILLA_WALK);
    }

    private void applyAbilities() {
        LocalPlayer player = player();
        if (player == null || !isEnabled()) return;

        player.getAbilities().setWalkingSpeed((float) (VANILLA_WALK * walk.value()));
        player.getAbilities().setFlyingSpeed((float) (VANILLA_FLY * fly.value()));

        // 注意：这里刻意不调用 onUpdateAbilities()。
        // 那个方法会往服务端发一个能力数据包，每 tick 发一次等于刷屏，
        // 而且服务端的移动速度也不由这个包决定，发了纯属浪费。
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.setBaseValue(VANILLA_WALK * walk.value());
        }
    }

    @Override
    public void onTick() {
        LocalPlayer player = player();
        if (player == null) return;

        applyAbilities();

        if (player.isInWater() || player.isSwimming()) {
            player.setDeltaMovement(
                boostHorizontal(player.getDeltaMovement(), swim.value(), SWIM_BASE * swim.value()));
        }

        if (player.isFallFlying()) {
            player.setDeltaMovement(
                boostHorizontal(player.getDeltaMovement(), elytra.value(), ELYTRA_BASE * elytra.value()));
        }

        if (player.getVehicle() instanceof LivingEntity mount) {
            mount.setDeltaMovement(
                boostHorizontal(mount.getDeltaMovement(), ride.value(), RIDE_BASE * ride.value()));
        }
    }

    /**
     * 放大水平速度，但把结果限制在 maxHorizontal 以内。
     * 这个上限就是防止"每 tick 乘一次"导致速度失控的关键。
     */
    private static Vec3 boostHorizontal(Vec3 motion, double multiplier, double maxHorizontal) {
        if (multiplier <= 1.0D) return motion;

        double x = motion.x * multiplier;
        double z = motion.z * multiplier;
        double horizontal = Math.sqrt(x * x + z * z);
        if (horizontal > maxHorizontal && horizontal > 1.0E-6D) {
            double scale = maxHorizontal / horizontal;
            x *= scale;
            z *= scale;
        }
        return new Vec3(x, motion.y, z);
    }

    public double getWalkMultiplier() {
        return walk.value();
    }

    public double getFlyMultiplier() {
        return fly.value();
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + String.format("\n§7行走 §f%.1fx §7飞行 §f%.1fx", walk.value(), fly.value())
            + String.format("\n§7游泳 §f%.1fx §7鞘翅 §f%.1fx §7骑乘 §f%.1fx",
                swim.value(), elytra.value(), ride.value());
    }
}
