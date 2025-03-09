package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;

public class AreaCopy extends CreativePlusFeature {
    private BlockPos firstPos = null;
    private BlockPos secondPos = null;
    private Map<BlockPos, BlockState> copiedBlocks = new HashMap<>();
    private BlockPos pasteReference = null;
    private Queue<BlockPos> placementQueue = new LinkedList<>();
    private boolean isPlacing = false;
    private static final int PLACE_DELAY = 2; // ticks between block placements
    private int tickCounter = 0;

    public AreaCopy() {
        super("area_copy", "复制和粘贴选定区域的建筑");
    }

    @Override
    public void onEnable() {
        reset();
        Minecraft.getInstance().player.sendSystemMessage(
            Component.literal("§b请使用木铲选择区域: 右键选择点，左键重置")
        );
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        firstPos = null;
        secondPos = null;
        copiedBlocks.clear();
        pasteReference = null;
        placementQueue.clear();
        isPlacing = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isCreative()) return;

        // 处理自动放置
        if (isPlacing && !placementQueue.isEmpty()) {
            tickCounter++;
            if (tickCounter >= PLACE_DELAY) {
                tickCounter = 0;
                placeNextBlock(mc);
            }
        }
    }

    @Override
    public void handleClick(boolean isRightClick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isCreative()) return;

        // 检查玩家是否手持木铲
        if (!mc.player.getMainHandItem().is(net.minecraft.world.item.Items.WOODEN_SHOVEL)) {
            return;
        }

        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;

        if (isRightClick) {
            if (firstPos == null) {
                firstPos = blockHit.getBlockPos();
                mc.player.sendSystemMessage(Component.literal("§a已设置第一个点"));
            } else if (secondPos == null) {
                secondPos = blockHit.getBlockPos();
                copyArea(mc);
            } else {
                pasteReference = blockHit.getBlockPos();
                pasteArea(mc);
            }
        } else {
            reset();
            mc.player.sendSystemMessage(Component.literal("§c已重置选区"));
        }
    }

    private void copyArea(Minecraft mc) {
        if (firstPos == null || secondPos == null || mc.level == null) return;

        copiedBlocks.clear();
        int minX = Math.min(firstPos.getX(), secondPos.getX());
        int minY = Math.min(firstPos.getY(), secondPos.getY());
        int minZ = Math.min(firstPos.getZ(), secondPos.getZ());
        int maxX = Math.max(firstPos.getX(), secondPos.getX());
        int maxY = Math.max(firstPos.getY(), secondPos.getY());
        int maxZ = Math.max(firstPos.getZ(), secondPos.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (!state.isAir()) {
                        copiedBlocks.put(pos.subtract(firstPos), state);
                    }
                }
            }
        }

        mc.player.sendSystemMessage(Component.literal("§a已复制 " + copiedBlocks.size() + " 个方块"));
    }

    private void pasteArea(Minecraft mc) {
        if (copiedBlocks.isEmpty() || pasteReference == null || mc.player == null) return;

        // 检查权限等级
        boolean hasCommandPermission = mc.player.hasPermissions(2);

        if (hasCommandPermission) {
            // 使用命令进行粘贴
            pasteUsingCommands(mc);
        } else {
            // 使用模拟放置进行粘贴
            preparePlacementQueue();
            isPlacing = true;
        }
    }

    private void pasteUsingCommands(Minecraft mc) {
        for (Map.Entry<BlockPos, BlockState> entry : copiedBlocks.entrySet()) {
            BlockPos targetPos = pasteReference.offset(entry.getKey());
            BlockState state = entry.getValue();
            
            // 获取方块的注册名
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(state.getBlock())
                .toString();
            
            // 构建方块状态
            StringBuilder blockStateString = new StringBuilder();
            
            // 收集所有方块状态属性
            state.getProperties().forEach(property -> {
                if (blockStateString.length() > 0) {
                    blockStateString.append(",");
                } else {
                    blockStateString.append("[");
                }
                
                String propertyName = property.getName();
                String propertyValue = state.getValue(property).toString().toLowerCase();
                blockStateString.append(propertyName).append("=").append(propertyValue);
            });
            
            if (blockStateString.length() > 0) {
                blockStateString.append("]");
            }
            
            // 构建setblock命令 (1.20格式)
            String command = String.format("setblock %d %d %d %s%s",
                targetPos.getX(), targetPos.getY(), targetPos.getZ(),
                blockId,
                blockStateString.toString()
            );
            
            // 发送命令
            mc.player.connection.sendCommand(command);
        }
        mc.player.sendSystemMessage(Component.literal("§a已通过命令粘贴 " + copiedBlocks.size() + " 个方块"));
    }

    private void preparePlacementQueue() {
        placementQueue.clear();
        // 按照从下到上的顺序添加方块位置
        copiedBlocks.entrySet().stream()
            .sorted((e1, e2) -> e1.getKey().getY() - e2.getKey().getY())
            .forEach(entry -> placementQueue.offer(pasteReference.offset(entry.getKey())));
    }

    private void placeNextBlock(Minecraft mc) {
        if (placementQueue.isEmpty() || mc.player == null) {
            isPlacing = false;
            return;
        }

        BlockPos pos = placementQueue.poll();
        BlockState state = copiedBlocks.get(pos.subtract(pasteReference));
        if (state == null) return;

        // 移动到目标位置
        double x = pos.getX() + 0.5;
        double y = pos.getY() - 0.5;
        double z = pos.getZ() + 0.5;
        
        // 发送移动数据包
        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, true));
        
        // 发送放置方块数据包
        mc.player.connection.send(new ServerboundUseItemOnPacket(
            InteractionHand.MAIN_HAND,
            new BlockHitResult(mc.player.position(), mc.player.getDirection(), pos, false),
            0
        ));

        if (placementQueue.isEmpty()) {
            isPlacing = false;
            mc.player.sendSystemMessage(Component.literal("§a已完成方块放置"));
        }
    }
} 