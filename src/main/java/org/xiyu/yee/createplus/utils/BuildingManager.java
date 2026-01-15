package org.xiyu.yee.createplus.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class BuildingManager {
    private static final BuildingManager INSTANCE = new BuildingManager();
    private BlockPos firstPos;
    private BlockPos secondPos;
    private Map<BlockPos, BlockState> selectedBlocks = new HashMap<>();
    private List<String> pendingCommands = new ArrayList<>();
    private boolean isPreviewActive = false;
    private AABB previewBox;
    private CommandSourceStack currentSource;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private int blockCount = 0;
    private double volume = 0;

    public static BuildingManager getInstance() {
        return INSTANCE;
    }

    private BuildingManager() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void setFirstPos(BlockPos pos) {
        firstPos = pos;
    }

    public void setSecondPos(BlockPos pos) {
        secondPos = pos;
        if (firstPos != null) {
            updateSelection();
        }
    }

    public BlockPos getFirstPos() {
        return firstPos;
    }

    public boolean hasSelection() {
        return firstPos != null && secondPos != null;
    }

    public Map<BlockPos, BlockState> getSelectedBlocks() {
        return selectedBlocks;
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
    }

    public void startPreview(Path filePath, CommandSourceStack source) {
        try {
            pendingCommands.clear();
            pendingCommands.addAll(Files.readAllLines(filePath));
            currentSource = source;
            isPreviewActive = true;
            
            // 计算预览区域
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            blockCount = 0;
            
            for (String command : pendingCommands) {
                if (command.startsWith("setblock")) {
                    blockCount++;
                    String[] parts = command.split(" ");
                    int x = Integer.parseInt(parts[1].substring(1));
                    int y = Integer.parseInt(parts[2].substring(1));
                    int z = Integer.parseInt(parts[3].substring(1));
                    
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                }
            }
            
            // 计算体积
            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            int depth = maxZ - minZ + 1;
            volume = width * height * depth;
            
            net.minecraft.world.phys.Vec3 pos = source.getPosition();
            BlockPos playerPos = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
            previewBox = new AABB(
                playerPos.getX() + minX, playerPos.getY() + minY, playerPos.getZ() + minZ,
                playerPos.getX() + maxX + 1, playerPos.getY() + maxY + 1, playerPos.getZ() + maxZ + 1
            );

            // 显示建筑信息
            source.sendSuccess(() -> 
                Component.literal(String.format(
                    "§a建筑信息:\n" +
                    "§7- 大小: §f%dx%dx%d\n" +
                    "§7- 方块数: §f%d\n" +
                    "§7- 体积: §f%.1f m³",
                    width, height, depth,
                    blockCount,
                    volume
                )), false);
            
            source.sendSuccess(() -> 
                Component.literal("§a正在预览建筑，输入 /confirmimport 确认导入（记得切换旁观模式），输入 /cancelimport 取消"), false);
            
        } catch (IOException e) {
            source.sendFailure(Component.literal("§c无法读取建筑文件: " + e.getMessage()));
        }
    }

    public void confirmImport() {
        if (!isPreviewActive || currentSource == null) return;
        
        // 关闭预览状态
        isPreviewActive = false;
        
        // 发送开始导入消息
        currentSource.sendSuccess(() -> 
            Component.literal("§a开始导入建筑，共 " + pendingCommands.size() + " 个方块"), false);

        // 创建新的执行器
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        executor = Executors.newSingleThreadScheduledExecutor();

        // 每tick执行一条命令
        final Iterator<String> commandIterator = pendingCommands.iterator();
        final int[] blockCount = {0};
        
        executor.scheduleAtFixedRate(() -> {
            try {
                if (commandIterator.hasNext()) {
                    String command = commandIterator.next();
                    // 检查并修正命令格式
                    if (command.startsWith("/")) {
                        command = command.substring(1);
                    }
                    if (!command.startsWith("s") && command.contains("etblock")) {
                        command = "s" + command;
                    }
                    
                    // 发送命令到服务器
                    Minecraft.getInstance().player.connection.sendCommand(command);
                    
                    blockCount[0]++;
                    
                    // 更新进度显示
                    float progress = (blockCount[0] * 100.0f) / pendingCommands.size();
                    
                    // 使用title显示进度
                    Minecraft.getInstance().player.connection.sendCommand(
                        String.format("title @s actionbar {\"text\":\"导入进度: %.1f%% (%d/%d)\",\"color\":\"green\"}",
                            progress, blockCount[0], pendingCommands.size())
                    );
                    
                } else {
                    // 完成导入
                    executor.shutdown();
                    // 清除进度显示
                    Minecraft.getInstance().player.connection.sendCommand(
                        "title @s actionbar {\"text\":\"建筑导入完成！\",\"color\":\"green\"}"
                    );
                    currentSource.sendSuccess(() -> 
                        Component.literal("§a建筑导入完成！"), false);
                    pendingCommands.clear();
                    previewBox = null;
                }
            } catch (Exception e) {
                currentSource.sendFailure(
                    Component.literal("§c导入过程中出错: " + e.getMessage())
                );
                // 清除进度显示
                Minecraft.getInstance().player.connection.sendCommand(
                    "title @s actionbar {\"text\":\"导入失败！\",\"color\":\"red\"}"
                );
                executor.shutdown();
                pendingCommands.clear();
                previewBox = null;
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    public void cancelImport() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        isPreviewActive = false;
        pendingCommands.clear();
        previewBox = null;
        
        // 清除进度显示并显示取消消息
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.sendCommand(
                "title @s actionbar {\"text\":\"已取消导入\",\"color\":\"red\"}"
            );
        }
        
        if (currentSource != null) {
            currentSource.sendSuccess(() -> 
                Component.literal("§c已取消导入"), false);
        }
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        
        if (isPreviewActive && previewBox != null) {
            // 获取渲染缓冲
            var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            
            // 渲染主边框
            RenderUtils.drawBox(event.getPoseStack(), previewBox, 0.0F, 1.0F, 0.0F, 0.4F);
            
            // 渲染边界线
            RenderUtils.drawEdgeLines(event.getPoseStack(), previewBox, 1.0F, 1.0F, 1.0F, 1.0F);
            
            // 渲染角点标记
            RenderUtils.drawCornerMarkers(event.getPoseStack(), previewBox, 1.0F, 0.0F, 0.0F, 1.0F);
            
            // 渲染信息文本
            if (Minecraft.getInstance().player != null) {
                double width = previewBox.maxX - previewBox.minX;
                double height = previewBox.maxY - previewBox.minY;
                double depth = previewBox.maxZ - previewBox.minZ;
                
                net.minecraft.world.phys.Vec3 center = previewBox.getCenter();
                
                // 渲染尺寸信息
                RenderUtils.renderText(
                    event.getPoseStack(),
                    String.format("§e%.1f×%.1f×%.1f", width, height, depth),
                    center.x, center.y + height/2 + 0.5, center.z,
                    0xFFFFFF
                );
                
                // 渲染方块数量
                RenderUtils.renderText(
                    event.getPoseStack(),
                    String.format("§a%d个方块", blockCount),
                    center.x, center.y + height/2 + 0.2, center.z,
                    0xFFFFFF
                );
            }
            
            // 结束渲染
            bufferSource.endBatch();
        }
    }

    public boolean isPreviewActive() {
        return isPreviewActive;
    }
} 