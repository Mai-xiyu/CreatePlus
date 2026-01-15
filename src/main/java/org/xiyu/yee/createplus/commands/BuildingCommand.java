package org.xiyu.yee.createplus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.utils.BuildingManager;
import org.xiyu.yee.createplus.utils.FileUtils;
import org.xiyu.yee.createplus.features.BuildingExport;
import org.xiyu.yee.createplus.features.CreativePlusFeature;

import java.io.*;
import java.nio.file.*;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Createplus.MODID)
public class BuildingCommand {
    
    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // 导出建筑命令
        dispatcher.register(Commands.literal("exportbuilding")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    return exportBuilding(context.getSource(), name);
                }))
        );

        // 导入建筑命令
        dispatcher.register(Commands.literal("importbuilding")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    return importBuilding(context.getSource(), name);
                }))
        );

        // 确认导入命令
        dispatcher.register(Commands.literal("confirmimport")
            .requires(source -> source.hasPermission(2))
            .executes(context -> confirmImport(context.getSource()))
        );

        // 取消导入命令
        dispatcher.register(Commands.literal("cancelimport")
            .executes(context -> cancelImport(context.getSource()))
        );
    }

    private static int exportBuilding(CommandSourceStack source, String name) {
        BuildingExport exportFeature = null;
        
        // 查找BuildingExport功能
        for (CreativePlusFeature feature : Createplus.FEATURE_MANAGER.getFeatures()) {
            if (feature instanceof BuildingExport) {
                exportFeature = (BuildingExport) feature;
                break;
            }
        }

        if (exportFeature == null || !exportFeature.isEnabled()) {
            source.sendFailure(Component.literal("§c请先启用建筑导出功能！使用 /features 建筑导出"));
            return 0;
        }

        if (!exportFeature.hasSelection()) {
            source.sendFailure(Component.literal("§c请先使用木铲选择区域！"));
            return 0;
        }

        try {
            Path buildingsDir = FileUtils.getBuildingsDirectory();
            Path filePath = buildingsDir.resolve(name + ".mcfunction");

            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                Map<BlockPos, BlockState> blocks = exportFeature.getSelectedBlocks();
                BlockPos reference = exportFeature.getFirstPos();

                for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
                    BlockPos relativePos = entry.getKey().subtract(reference);
                    BlockState state = entry.getValue();
                    
                    // 获取方块的注册名和状态
                    String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    StringBuilder blockStateString = new StringBuilder();
                    
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

                    // 写入setblock命令
                    writer.write(String.format("setblock ~%d ~%d ~%d %s%s\n",
                        relativePos.getX(), relativePos.getY(), relativePos.getZ(),
                        blockId, blockStateString.toString()));
                }
            }

            source.sendSuccess(() -> 
                Component.literal("§a已将建筑导出到: createplus/buildings/" + name + ".mcfunction"), false);
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal("§c导出失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int importBuilding(CommandSourceStack source, String name) {
        try {
            Path filePath = FileUtils.getBuildingsDirectory().resolve(name + ".mcfunction");
            if (!Files.exists(filePath)) {
                source.sendFailure(Component.literal("§c找不到建筑文件: createplus/buildings/" + name + ".mcfunction"));
                return 0;
            }

            // 检查权限
            if (!source.hasPermission(2)) {
                source.sendFailure(Component.literal("§c需要2级权限才能导入建筑"));
                return 0;
            }

            // 启动预览渲染
            BuildingManager.getInstance().startPreview(filePath, source);
            source.sendSuccess(() -> 
                Component.literal("§a正在预览建筑，输入 /confirmimport 确认导入，输入 /cancelimport 取消"), false);
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal("§c导入失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int confirmImport(CommandSourceStack source) {
        BuildingManager manager = BuildingManager.getInstance();
        if (!manager.isPreviewActive()) {
            source.sendFailure(Component.literal("§c没有正在预览的建筑！请先使用 /importbuilding <名称> 选择建筑"));
            return 0;
        }

        manager.confirmImport();
        return 1;
    }

    private static int cancelImport(CommandSourceStack source) {
        BuildingManager manager = BuildingManager.getInstance();
        if (!manager.isPreviewActive()) {
            source.sendFailure(Component.literal("§c没有正在预览的建筑！"));
            return 0;
        }
        manager.cancelImport();
        return 1;
    }
} 