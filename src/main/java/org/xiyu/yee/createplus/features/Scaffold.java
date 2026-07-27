package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.utils.BlockEdits;

/**
 * 自动在脚下铺方块，方便悬空搭建。
 *
 * 旧实现只在"刚按下跳跃键的那一帧"补一格，飞行或走出边缘时都不管用，
 * 而且是无条件放置，第一次踩空就掉下去了。
 */
public class Scaffold extends CreativePlusFeature {

    public enum Mode {
        /** 只要脚下是空的就补。 */
        ALWAYS,
        /** 只在潜行时补，适合精细作业。 */
        SNEAK,
        /** 只在按住跳跃时补，用来向上搭塔。 */
        JUMP
    }

    private final EnumSetting<Mode> mode = enumSetting("mode", Mode.ALWAYS);
    private final BooleanSetting tower = boolSetting("tower", false);
    private final BooleanSetting whileFlying = boolSetting("while_flying", false);

    public Scaffold() {
        super("scaffold", "自动在脚下放置方块，悬空也能走", Category.BUILDING);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !player.isCreative()) return;
        if (!(player.getMainHandItem().getItem() instanceof BlockItem)) return;
        if (!whileFlying.get() && player.getAbilities().flying) return;

        boolean active = switch (mode.get()) {
            case ALWAYS -> true;
            case SNEAK -> player.isShiftKeyDown();
            case JUMP -> player.input.jumping;
        };
        if (!active) return;

        BlockPos below = player.blockPosition().below();
        if (!mc.level.getBlockState(below).isAir()) return;

        BlockEdits.placeWithHeldItem(below, InteractionHand.MAIN_HAND);

        // 搭塔：按住跳跃时把玩家往上顶一格，配合脚下补方块就能垂直上升
        if (tower.get() && player.input.jumping && player.onGround()) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.42D, player.getDeltaMovement().z);
        }
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7触发方式: §f" + mode.getDisplayValue()
            + (tower.get() ? "\n§7搭塔: 按住跳跃垂直上升" : "")
            + "\n§7需要主手拿着方块";
    }
}
