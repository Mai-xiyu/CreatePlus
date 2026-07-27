package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 自动搭路：向前走时在脚下自动续方块，修桥/铺路不用低头点。
 */
public class AutoBridge extends CreativePlusFeature {

    private final BooleanSetting onlyWhenMoving = boolSetting("only_when_moving", true);
    private final BooleanSetting requireSneak = boolSetting("require_sneak", false);

    public AutoBridge() {
        super("auto_bridge", "走路时自动在脚下续方块（需创造）", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7自动搭路只在创造模式下生效");
        } else {
            notifyPlayer("§b[CreatePlus] §7自动搭路已开启 · 手持方块向前走即可");
        }
    }

    @Override
    public void onTick() {
        if (!CreativeGate.allow()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || mc.level == null) return;
        if (mc.screen != null) return;
        if (requireSneak.get() && !mc.player.isShiftKeyDown()) return;

        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof BlockItem)) return;

        if (onlyWhenMoving.get()) {
            if (mc.player.input.forwardImpulse == 0 && mc.player.input.leftImpulse == 0) return;
        }

        BlockPos below = mc.player.blockPosition().below();
        if (!mc.level.getBlockState(below).canBeReplaced()) return;

        // 从下方朝上点一下，模拟贴地放置
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(below), Direction.UP, below, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7仅移动时: §f" + (onlyWhenMoving.get() ? "开" : "关")
            + " §7需潜行: §f" + (requireSneak.get() ? "开" : "关")
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
