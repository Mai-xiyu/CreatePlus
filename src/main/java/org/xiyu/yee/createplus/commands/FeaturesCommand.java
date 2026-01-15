package org.xiyu.yee.createplus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.utils.HelpManager;

import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Createplus.MODID)
public class FeaturesCommand {

    // 创建一个命令补全提供器
    private static final SuggestionProvider<CommandSourceStack> FEATURE_SUGGESTIONS = (context, builder) -> {
        // 获取所有功能名称
        var featureNames = Createplus.FEATURE_MANAGER.getFeatures()
            .stream()
            .map(CreativePlusFeature::getName)
            .collect(Collectors.toList());
        
        // 返回匹配当前输入的建议
        return SharedSuggestionProvider.suggest(featureNames, builder);
    };

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("features")
            .then(Commands.argument("feature", StringArgumentType.greedyString())
                .suggests(FEATURE_SUGGESTIONS) // 添加补全建议
                .executes(context -> {
                    String featureName = StringArgumentType.getString(context, "feature");
                    return toggleFeature(context.getSource(), featureName);
                }))
            .then(Commands.literal("help")
                .executes(context -> {
                    showHelp(context.getSource(), null);
                    return 1;
                })
                .then(Commands.argument("feature", StringArgumentType.greedyString())
                    .suggests(FEATURE_SUGGESTIONS)
                    .executes(context -> {
                        String featureName = StringArgumentType.getString(context, "feature");
                        showHelp(context.getSource(), featureName);
                        return 1;
                    })))
            .executes(context -> listFeatures(context.getSource()))); // 不带参数时列出所有功能
    }

    private static int toggleFeature(CommandSourceStack source, String featureName) {
        var features = Createplus.FEATURE_MANAGER.getFeatures();
        
        for (CreativePlusFeature feature : features) {
            if (feature.getName().equals(featureName)) {
                feature.toggle();
                String status = feature.isEnabled() ? "启用" : "禁用";
                source.sendSuccess(() -> 
                    Component.literal("§b已" + status + "功能: §f" + feature.getName()), false);
                return 1;
            }
        }

        source.sendFailure(Component.literal("§c未找到功能: §f" + featureName));
        return 0;
    }

    private static int listFeatures(CommandSourceStack source) {
        var features = Createplus.FEATURE_MANAGER.getFeatures();
        
        source.sendSuccess(() -> Component.literal("§b可用功能列表:"), false);
        for (CreativePlusFeature feature : features) {
            String status = feature.isEnabled() ? "§a[已启用]" : "§7[已禁用]";
            source.sendSuccess(() -> 
                Component.literal(status + " §f" + feature.getName() + " §7- " + feature.getDescription()), false);
        }
        
        source.sendSuccess(() -> Component.literal("\n§e使用 §6/features help <功能名> §e查看详细帮助"), false);
        return 1;
    }

    private static void showHelp(CommandSourceStack source, String featureName) {
        if (featureName == null) {
            HelpManager.showGeneralHelp(source);
        } else {
            HelpManager.showFeatureHelp(source, featureName);
        }
    }
} 