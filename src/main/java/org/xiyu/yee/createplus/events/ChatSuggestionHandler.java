package org.xiyu.yee.createplus.events;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xiyu.yee.createplus.Createplus;

@Mod.EventBusSubscriber(modid = Createplus.MODID)
public class ChatSuggestionHandler {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // 注册基础命令
        registerCommand(dispatcher, "hudcolor", "fps", "time", "pos", "biome", "localtime");
        registerCommand(dispatcher, "weather", "晴天", "雨天", "雷暴");
        registerCommand(dispatcher, "time", "日出", "中午", "日落", "前夜", "午夜");
        registerCommand(dispatcher, "give", "@s", "@p", "@a");
        
        // 注册子命令
        registerSubCommand(dispatcher, "hudcolor fps", "text", "value");
        registerSubCommand(dispatcher, "hudcolor time", "text", "value");
        registerSubCommand(dispatcher, "hudcolor pos", "text", "value");
        registerSubCommand(dispatcher, "hudcolor biome", "text", "value");
        registerSubCommand(dispatcher, "hudcolor localtime", "text", "value");
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, String command, String... subCommands) {
        // 注册带斜杠的命令
        var slashBuilder = Commands.literal(command);
        // 注册带点号的命令
        var dotBuilder = Commands.literal("." + command);
        
        for (String sub : subCommands) {
            var subCommand = Commands.literal(sub)
                .executes(context -> handleCommand(command, sub));
            slashBuilder.then(subCommand);
            dotBuilder.then(subCommand);
        }
        
        dispatcher.register(slashBuilder);
        dispatcher.register(dotBuilder);
    }

    private static void registerSubCommand(CommandDispatcher<CommandSourceStack> dispatcher, String parentCommand, String... subCommands) {
        String[] parts = parentCommand.split(" ");
        
        // 为带斜杠和点号的父命令分别注册子命令
        for (String prefix : new String[]{"", "."}) {
            String fullCommand = prefix + parentCommand;
            LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(prefix + parts[0]);
            
            for (String sub : subCommands) {
                builder.then(Commands.literal(parts[1])
                    .then(Commands.literal(sub)
                        .executes(context -> handleSubCommand(parentCommand, sub))));
            }
            
            dispatcher.register(builder);
        }
    }

    private static int handleCommand(String command, String subCommand) {
        // 处理命令逻辑
        return 1;
    }

    private static int handleSubCommand(String parentCommand, String subCommand) {
        // 处理子命令逻辑
        return 1;
    }
} 