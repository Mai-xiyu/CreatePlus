package org.xiyu.yee.createplus.features;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 水上行走：在水面/岩浆表面像固体一样跑（Jesus 风格，需创造）。
 */
public class LiquidWalk extends CreativePlusFeature {

    private final BooleanSetting lava = boolSetting("lava", true);
    private final BooleanSetting water = boolSetting("water", true);

    public LiquidWalk() {
        super("liquid_walk", "在水面与岩浆上行走（需创造）", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7液面行走只在创造模式下生效");
        }
    }

    @Override
    public void onTick() {
        if (!CreativeGate.allow()) return;
        LocalPlayer player = player();
        if (player == null || player.isShiftKeyDown() || player.getAbilities().flying) return;

        BlockPos pos = player.blockPosition();
        FluidState fluid = player.level().getFluidState(pos);
        FluidState below = player.level().getFluidState(pos.below());

        boolean onLiquid = isTarget(fluid) || isTarget(below)
            || player.level().getBlockState(pos).getBlock() instanceof LiquidBlock
            || player.level().getBlockState(pos.below()).getBlock() instanceof LiquidBlock;
        if (!onLiquid) return;

        // 把垂直速度托住，像踩在实心面上
        Vec3 motion = player.getDeltaMovement();
        if (motion.y < 0.0D) {
            player.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
        player.setOnGround(true);
        player.fallDistance = 0.0F;

        // 略微抬到液面之上，避免闷头游泳动画
        if (player.getY() < pos.getY() + 0.99D && isTarget(below)) {
            player.setPos(player.getX(), pos.getY() + 1.0D, player.getZ());
        }
    }

    private boolean isTarget(FluidState state) {
        if (state.isEmpty()) return false;
        if (water.get() && state.is(net.minecraft.world.level.material.Fluids.WATER)) return true;
        if (lava.get() && state.is(net.minecraft.world.level.material.Fluids.LAVA)) return true;
        return false;
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7水: §f" + (water.get() ? "开" : "关")
            + " §7岩浆: §f" + (lava.get() ? "开" : "关")
            + "\n§7潜行可沉下去"
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
