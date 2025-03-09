package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;

import java.util.ArrayList;
import java.util.List;

public class AreaPlace extends CreativePlusFeature {
    private static final int PLACE_DELAY = 1;
    private int tickCounter = 0;
    private BlockPos lastPos = null;

    public AreaPlace() {
        super("area_place", "使用副手物品进行范围放置");
    }

    @Override
    public void onEnable() {
        Minecraft.getInstance().player.sendSystemMessage(
            Component.literal("§b范围放置已启用，副手持方块即可生效")
        );
    }

    @Override
    public void onDisable() {
        lastPos = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isCreative()) return;

        // 检查副手是否持有方块
        ItemStack offhandItem = mc.player.getOffhandItem();
        if (!(offhandItem.getItem() instanceof BlockItem)) return;

        tickCounter++;
        if (tickCounter < PLACE_DELAY) return;
        tickCounter = 0;

        // 获取玩家的触及距离
        double reach = mc.player.getBlockReach();
        
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
            BlockPos centerPos = blockHit.getBlockPos().relative(blockHit.getDirection());
            
            // 如果这个位置已经处理过，跳过
            if (centerPos.equals(lastPos)) return;
            lastPos = centerPos;

            // 获取范围内的所有位置
            List<BlockPos> placementPositions = new ArrayList<>();
            int radius = 2;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = centerPos.offset(x, y, z);
                        
                        // 检查位置是否在玩家触及范围内
                        if (mc.player.position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > reach * reach) {
                            continue;
                        }

                        // 检查位置是否可以放置方块
                        if (mc.level.getBlockState(pos).isAir()) {
                            placementPositions.add(pos);
                        }
                    }
                }
            }

            // 发送放置数据包
            for (BlockPos pos : placementPositions) {
                mc.player.connection.send(new ServerboundUseItemOnPacket(
                    InteractionHand.OFF_HAND,
                    new BlockHitResult(mc.player.position(), blockHit.getDirection(), pos, false),
                    0
                ));
            }
        }
    }
} 