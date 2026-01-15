package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class BuildingExport extends CreativePlusFeature {
    private BlockPos firstPos = null;
    private BlockPos secondPos = null;
    private Map<BlockPos, BlockState> selectedBlocks = new HashMap<>();

    public BuildingExport() {
        super("building_export", "使用木铲选择区域并导出建筑");
    }

    @Override
    public void onEnable() {
        firstPos = null;
        secondPos = null;
        selectedBlocks.clear();
        Minecraft.getInstance().player.sendSystemMessage(
            Component.literal("§b建筑导出已启用，使用木铲右键选择两个点")
        );
    }

    @Override
    public void onDisable() {
        firstPos = null;
        secondPos = null;
        selectedBlocks.clear();
    }

    public void handleClick(boolean isRightClick) {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isCreative()) return;

        // 检查是否手持木铲
        if (!mc.player.getMainHandItem().is(Items.WOODEN_SHOVEL)) {
            return;
        }

        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
        BlockPos pos = blockHit.getBlockPos();

        if (isRightClick) {
            if (firstPos == null) {
                firstPos = pos;
                mc.player.sendSystemMessage(
                    Component.literal("§a已设置第一个点: " + pos.toShortString())
                );
            } else {
                secondPos = pos;
                mc.player.sendSystemMessage(
                    Component.literal("§a已设置第二个点: " + pos.toShortString())
                );
                updateSelection();
            }
        } else {
            // 左键重置选择
            firstPos = null;
            secondPos = null;
            selectedBlocks.clear();
            mc.player.sendSystemMessage(Component.literal("§c已重置选择"));
        }
    }

    private void updateSelection() {
        selectedBlocks.clear();
        if (firstPos == null || secondPos == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

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
                        selectedBlocks.put(pos, state);
                    }
                }
            }
        }

        mc.player.sendSystemMessage(
            Component.literal("§a已选择 " + selectedBlocks.size() + " 个方块，可以使用 /exportbuilding <名称> 导出")
        );
    }

    public boolean hasSelection() {
        return firstPos != null && secondPos != null && !selectedBlocks.isEmpty();
    }

    public BlockPos getFirstPos() {
        return firstPos;
    }

    public Map<BlockPos, BlockState> getSelectedBlocks() {
        return selectedBlocks;
    }

} 