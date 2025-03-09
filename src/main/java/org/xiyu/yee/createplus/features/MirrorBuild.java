package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;

public class MirrorBuild extends CreativePlusFeature {
    private BlockPos mirrorPoint = null;
    private Direction.Axis mirrorAxis = Direction.Axis.X; // 默认X轴镜像

    public MirrorBuild() {
        super("mirror_build", "同时在对称位置放置方块");
    }

    @Override
    public void onEnable() {
        mirrorPoint = null;
        Minecraft.getInstance().player.sendSystemMessage(
            Component.literal("§b请使用木剑: 右键选择镜像点，Shift+右键切换镜像轴(当前: " + getMirrorAxisName() + ")")
        );
    }

    @Override
    public void onDisable() {
        mirrorPoint = null;
    }

    private String getMirrorAxisName() {
        return switch (mirrorAxis) {
            case X -> "X轴(东西)";
            case Z -> "Z轴(南北)";
            default -> "未知";
        };
    }

    public void toggleMirrorAxis() {
        mirrorAxis = (mirrorAxis == Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
        Minecraft.getInstance().player.sendSystemMessage(
            Component.literal("§b已切换镜像轴: " + getMirrorAxisName())
        );
    }

    @Override
    public void handleClick(boolean isRightClick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isCreative()) return;

        // 检查玩家是否手持木剑
        if (!mc.player.getMainHandItem().is(net.minecraft.world.item.Items.WOODEN_SWORD)) {
            return;
        }

        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;

        if (isRightClick) {
            if (mc.player.isShiftKeyDown()) {
                // Shift+右键：切换镜像轴
                toggleMirrorAxis();
            } else {
                // 普通右键：设置镜像点
                if (mirrorPoint == null) {
                    mirrorPoint = blockHit.getBlockPos();
                    mc.player.sendSystemMessage(
                        Component.literal("§a已设置镜像点: " + mirrorPoint.toShortString())
                    );
                }
            }
        } else {
            // 左键：重置镜像点
            mirrorPoint = null;
            mc.player.sendSystemMessage(Component.literal("§c已重置镜像点"));
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isCreative()) return;

        // 监听方块放置
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
            
            // 如果正在放置方块且有镜像点，则在镜像位置也放置
            if (mc.options.keyUse.isDown() && mirrorPoint != null) {
                handleBlockPlace((BlockHitResult) mc.hitResult);
            }
        }
    }

    public void handleBlockPlace(BlockHitResult hitResult) {
        if (mirrorPoint == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        BlockPos placePos = hitResult.getBlockPos();
        BlockPos mirroredPos = getMirroredPosition(placePos);

        // 发送放置方块数据包到镜像位置
        mc.player.connection.send(new ServerboundUseItemOnPacket(
            InteractionHand.MAIN_HAND,
            new BlockHitResult(mc.player.position(), hitResult.getDirection(), mirroredPos, false),
            0
        ));
    }

    private BlockPos getMirroredPosition(BlockPos pos) {
        if (mirrorAxis == Direction.Axis.X) {
            // X轴镜像：镜像点的X坐标 + (镜像点的X坐标 - 原始点的X坐标)
            int deltaX = mirrorPoint.getX() - pos.getX();
            return new BlockPos(mirrorPoint.getX() + deltaX, pos.getY(), pos.getZ());
        } else {
            // Z轴镜像：镜像点的Z坐标 + (镜像点的Z坐标 - 原始点的Z坐标)
            int deltaZ = mirrorPoint.getZ() - pos.getZ();
            return new BlockPos(pos.getX(), pos.getY(), mirrorPoint.getZ() + deltaZ);
        }
    }
} 