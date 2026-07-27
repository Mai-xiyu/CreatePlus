package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.ItemStack;
import org.xiyu.yee.createplus.settings.BooleanSetting;

/**
 * 创造模式下保持手中物品满堆叠，这样连续放置时不会因为数量减少而中断。
 */
public class InfinitePlacement extends CreativePlusFeature {
    /** 快捷栏里所有格子都补满，而不只是手上那一格。 */
    private final BooleanSetting wholeHotbar = boolSetting("whole_hotbar", false);

    public InfinitePlacement() {
        super("infinite_placement", "创造模式下自动补满手中物品", Category.BUILDING);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !player.isCreative() || mc.getConnection() == null) return;

        if (wholeHotbar.get()) {
            for (int slot = 0; slot < 9; slot++) {
                refill(mc, player, slot);
            }
        } else {
            refill(mc, player, player.getInventory().selected);
        }
    }

    private void refill(Minecraft mc, LocalPlayer player, int hotbarSlot) {
        ItemStack stack = player.getInventory().getItem(hotbarSlot);
        if (stack.isEmpty()) return;

        // 尊重物品自己的最大堆叠数：以前写死 64，会把只能叠 16 个的物品同步成非法状态
        int max = stack.getMaxStackSize();
        if (max <= 1 || stack.getCount() >= max) return;

        stack.setCount(max);
        // 创造模式物品栏数据包用的是容器槽位编号，快捷栏从 36 开始
        mc.getConnection().send(new ServerboundSetCreativeModeSlotPacket(36 + hotbarSlot, stack));
    }
}
