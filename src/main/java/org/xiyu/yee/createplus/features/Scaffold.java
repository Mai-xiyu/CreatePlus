package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;

public class Scaffold extends CreativePlusFeature {
    private boolean wasJumping = false;
    private static final int PLACE_DELAY = 1;
    private int tickCounter = 0;

    public Scaffold() {
        super("scaffold", "跳跃时自动在脚下放置方块");
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isCreative()) return;

        tickCounter++;
        if (tickCounter < PLACE_DELAY) return;
        tickCounter = 0;

        // 检查是否在跳跃
        boolean isJumping = mc.player.input.jumping;
        
        // 如果玩家正在跳跃
        if (isJumping && !wasJumping) {
            // 获取玩家手中的物品
            ItemStack heldItem = mc.player.getMainHandItem();
            if (!(heldItem.getItem() instanceof BlockItem)) return;

            // 获取玩家脚下的位置
            BlockPos playerPos = mc.player.blockPosition();
            BlockPos placePos = playerPos.below();
            
            // 如果脚下是空气，放置方块
            if (mc.level != null && mc.level.getBlockState(placePos).isAir()) {
                // 创建方块放置结果
                BlockHitResult hitResult = new BlockHitResult(
                    Vec3.atBottomCenterOf(placePos.above()), // 点击位置
                    Direction.DOWN, // 从上方放置
                    placePos, // 目标方块位置
                    false // inside
                );

                // 发送放置数据包
                mc.player.connection.send(new ServerboundUseItemOnPacket(
                    InteractionHand.MAIN_HAND,
                    hitResult,
                    0
                ));

                // 播放放置音效
                mc.player.playSound(
                    net.minecraft.sounds.SoundEvents.STONE_PLACE,
                    1.0F,
                    1.0F
                );
            }
        }
        
        wasJumping = isJumping;
    }

    @Override
    public void onEnable() {
        wasJumping = false;
        tickCounter = 0;
        Minecraft.getInstance().player.sendSystemMessage(
            Component.literal("§b脚手架已启用")
        );
    }

    @Override
    public void onDisable() {
        wasJumping = false;
        tickCounter = 0;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                Component.literal("§7脚手架已禁用")
            );
        }
    }
} 