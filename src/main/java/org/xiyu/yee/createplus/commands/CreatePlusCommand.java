package org.xiyu.yee.createplus.commands;

import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.client.CreatePlusClient;
import org.xiyu.yee.createplus.features.AreaCopy;
import org.xiyu.yee.createplus.features.AreaFill;
import org.xiyu.yee.createplus.features.BuildingExport;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.features.CurveBuilder;
import org.xiyu.yee.createplus.features.PaintPalette;
import org.xiyu.yee.createplus.features.ShapeStudio;
import org.xiyu.yee.createplus.features.TimeWeatherControl;
import org.xiyu.yee.createplus.settings.Setting;
import org.xiyu.yee.createplus.ui.FeatureScreen;
import org.xiyu.yee.createplus.utils.BlockEdits;
import org.xiyu.yee.createplus.utils.BuildingImport;
import org.xiyu.yee.createplus.utils.EditQueue;
import org.xiyu.yee.createplus.utils.FileUtils;
import org.xiyu.yee.createplus.utils.HelpManager;
import org.xiyu.yee.createplus.utils.Selection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 所有客户端命令。
 *
 * 统一挂在 {@code /createplus} 下，只额外保留 {@code /features} 作为老用户的别名。
 * 旧版本用聊天里的点号命令（{@code .give}、{@code .time}）实现这些功能，
 * 既没有补全也会和服务器命令抢输入。
 */
public final class CreatePlusCommand {

    private static final SuggestionProvider<CommandSourceStack> FEATURES = (context, builder) ->
        SharedSuggestionProvider.suggest(
            Createplus.FEATURE_MANAGER.getFeatures().stream().map(CreativePlusFeature::getName).toList(),
            builder);

    private static final SuggestionProvider<CommandSourceStack> SETTINGS = (context, builder) -> {
        CreativePlusFeature feature = Createplus.FEATURE_MANAGER.getFeature(
            StringArgumentType.getString(context, "feature"));
        List<String> ids = new ArrayList<>();
        if (feature != null) {
            for (Setting<?> setting : feature.getSettings()) {
                ids.add(setting.getId());
            }
        }
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> BUILDINGS = (context, builder) -> {
        List<String> names = new ArrayList<>();
        try (var stream = Files.list(FileUtils.getBuildingsDirectory())) {
            stream.filter(path -> path.toString().endsWith(".mcfunction"))
                .forEach(path -> {
                    String file = path.getFileName().toString();
                    names.add(file.substring(0, file.length() - ".mcfunction".length()));
                });
        } catch (IOException ignored) {
            // 目录还不存在就没有可补全的内容
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> FILL_MODES = (context, builder) ->
        SharedSuggestionProvider.suggest(names(AreaFill.Mode.values()), builder);

    private static final SuggestionProvider<CommandSourceStack> SHAPES = (context, builder) ->
        SharedSuggestionProvider.suggest(names(ShapeStudio.Shape.values()), builder);

    private static final SuggestionProvider<CommandSourceStack> DIRECTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(names(Direction.values()), builder);

    private static final SuggestionProvider<CommandSourceStack> WEATHER = (context, builder) ->
        SharedSuggestionProvider.suggest(names(TimeWeatherControl.Weather.values()), builder);

    private static final SuggestionProvider<CommandSourceStack> TIME_PRESETS = (context, builder) ->
        SharedSuggestionProvider.suggest(
            List.of("sunrise", "day", "noon", "sunset", "night", "midnight"), builder);

    private CreatePlusCommand() {
    }

    private static List<String> names(Enum<?>[] values) {
        List<String> result = new ArrayList<>();
        for (Enum<?> value : values) {
            result.add(value.name().toLowerCase(Locale.ROOT));
        }
        return result;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root("createplus"));
        // 别名整体重定向到主命令，这样 /cplus help、/features toggle 之类全都能用。
        // 只在别名上重新挂一遍子命令的话，漏掉的那些会变成"未知功能"。
        alias(dispatcher, "cplus", node);
        alias(dispatcher, "features", node);
    }

    private static void alias(CommandDispatcher<CommandSourceStack> dispatcher, String name,
                              LiteralCommandNode<CommandSourceStack> target) {
        dispatcher.register(Commands.literal(name)
            .executes(context -> list(context.getSource()))
            .redirect(target));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
            .executes(context -> list(context.getSource()))

            .then(Commands.literal("list").executes(context -> list(context.getSource())))

            .then(Commands.literal("gui").executes(context -> {
                Minecraft.getInstance().tell(() ->
                    Minecraft.getInstance().setScreen(CreatePlusClient.clickGui()));
                return 1;
            }))

            .then(Commands.literal("hud").executes(context -> {
                FeatureScreen.get().toggleVisibility();
                return 1;
            }))

            .then(Commands.literal("toggle")
                .then(Commands.argument("feature", StringArgumentType.word())
                    .suggests(FEATURES)
                    .executes(context -> toggle(context.getSource(),
                        StringArgumentType.getString(context, "feature")))))

            .then(Commands.literal("set")
                .then(Commands.argument("feature", StringArgumentType.word())
                    .suggests(FEATURES)
                    .then(Commands.argument("setting", StringArgumentType.word())
                        .suggests(SETTINGS)
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .executes(context -> set(context.getSource(),
                                StringArgumentType.getString(context, "feature"),
                                StringArgumentType.getString(context, "setting"),
                                StringArgumentType.getString(context, "value")))))))

            .then(Commands.literal("reset")
                .then(Commands.argument("feature", StringArgumentType.word())
                    .suggests(FEATURES)
                    .executes(context -> reset(context.getSource(),
                        StringArgumentType.getString(context, "feature")))))

            .then(Commands.literal("help")
                .executes(context -> {
                    HelpManager.showGeneralHelp(context.getSource());
                    return 1;
                })
                .then(Commands.argument("feature", StringArgumentType.word())
                    .suggests(FEATURES)
                    .executes(context -> {
                        HelpManager.showFeatureHelp(context.getSource(),
                            StringArgumentType.getString(context, "feature"));
                        return 1;
                    })))

            // === 选区与编辑 ===

            .then(Commands.literal("sel")
                .executes(context -> selectionInfo(context.getSource()))
                .then(Commands.literal("clear").executes(context -> {
                    Selection.shared().clear();
                    context.getSource().sendSuccess(() -> literal("§7选区已清空"), false);
                    return 1;
                })))

            .then(Commands.literal("copy").executes(context ->
                withFeature(context.getSource(), AreaCopy.class, feature -> feature.copy())))

            .then(Commands.literal("paste").executes(context ->
                withFeature(context.getSource(), AreaCopy.class, feature -> feature.paste())))

            .then(Commands.literal("stack")
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                    .executes(context -> withFeature(context.getSource(), AreaCopy.class,
                        feature -> feature.stack(IntegerArgumentType.getInteger(context, "count"), null)))
                    .then(Commands.argument("direction", StringArgumentType.word())
                        .suggests(DIRECTIONS)
                        .executes(context -> {
                            String raw = StringArgumentType.getString(context, "direction");
                            Direction direction = Direction.byName(raw.toLowerCase(Locale.ROOT));
                            if (direction == null) {
                                context.getSource().sendFailure(literal("§c未知方向: " + raw));
                                return 0;
                            }
                            return withFeature(context.getSource(), AreaCopy.class,
                                feature -> feature.stack(
                                    IntegerArgumentType.getInteger(context, "count"), direction));
                        }))))

            .then(Commands.literal("fill")
                .executes(context -> withFeature(context.getSource(), AreaFill.class,
                    feature -> feature.fill(feature.getMode())))
                .then(Commands.argument("mode", StringArgumentType.word())
                    .suggests(FILL_MODES)
                    .executes(context -> {
                        String raw = StringArgumentType.getString(context, "mode");
                        AreaFill.Mode mode;
                        try {
                            mode = AreaFill.Mode.valueOf(raw.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            context.getSource().sendFailure(literal("§c未知的填充模式: " + raw));
                            return 0;
                        }
                        return withFeature(context.getSource(), AreaFill.class,
                            feature -> feature.fill(mode));
                    })))

            .then(Commands.literal("maxvolume")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> literal(
                        "§7当前方块操作上限: §f" + BlockEdits.maxVolume()
                            + " §7（可改范围 " + BlockEdits.FILL_COMMAND_MAX
                            + " ~ " + BlockEdits.ABSOLUTE_MAX_VOLUME + "）"), false);
                    return 1;
                })
                .then(Commands.argument("value", IntegerArgumentType.integer(
                        BlockEdits.FILL_COMMAND_MAX, BlockEdits.ABSOLUTE_MAX_VOLUME))
                    .executes(context -> {
                        int value = IntegerArgumentType.getInteger(context, "value");
                        return withFeature(context.getSource(), AreaFill.class, feature -> {
                            feature.setMaxVolumeLimit(value);
                            context.getSource().sendSuccess(() -> literal(
                                "§a方块操作上限已设为 §f" + BlockEdits.maxVolume()), false);
                        });
                    })))

            // === 生成式建造 ===

            .then(Commands.literal("shape")
                .executes(context -> shapeInfo(context.getSource()))
                .then(Commands.literal("pin").executes(context ->
                    withFeature(context.getSource(), ShapeStudio.class, ShapeStudio::togglePin)))
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests(SHAPES)
                    .executes(context -> shape(context.getSource(),
                        StringArgumentType.getString(context, "type"), null, null))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                        .executes(context -> shape(context.getSource(),
                            StringArgumentType.getString(context, "type"),
                            IntegerArgumentType.getInteger(context, "radius"), null))
                        .then(Commands.argument("height", IntegerArgumentType.integer(1, 128))
                            .executes(context -> shape(context.getSource(),
                                StringArgumentType.getString(context, "type"),
                                IntegerArgumentType.getInteger(context, "radius"),
                                IntegerArgumentType.getInteger(context, "height")))))))

            .then(Commands.literal("build").executes(context -> build(context.getSource())))

            .then(Commands.literal("curve")
                .then(Commands.literal("clear").executes(context ->
                    withFeature(context.getSource(), CurveBuilder.class, CurveBuilder::clearPoints))))

            .then(Commands.literal("palette")
                .executes(context -> paletteInfo(context.getSource()))
                .then(Commands.literal("grab").executes(context -> paletteGrab(context.getSource())))
                .then(Commands.literal("list").executes(context -> paletteInfo(context.getSource())))
                .then(Commands.literal("clear").executes(context ->
                    withFeature(context.getSource(), PaintPalette.class, palette -> {
                        palette.clear();
                        context.getSource().sendSuccess(() -> literal("§7调色板已清空"), false);
                    }))))

            .then(Commands.literal("undo").executes(context ->
                withFeature(context.getSource(), AreaFill.class, feature -> feature.undo())))

            .then(Commands.literal("cancel").executes(context -> {
                EditQueue.cancel();
                BuildingImport.cancel();
                context.getSource().sendSuccess(() -> literal("§7已取消进行中的操作"), false);
                return 1;
            }))

            // === 建筑文件 ===

            .then(Commands.literal("export")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> export(context.getSource(),
                        StringArgumentType.getString(context, "name")))))

            .then(Commands.literal("import")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(BUILDINGS)
                    .executes(context -> importBuilding(context.getSource(),
                        StringArgumentType.getString(context, "name")))))

            .then(Commands.literal("confirm").executes(context -> {
                if (!BuildingImport.hasPreview()) {
                    context.getSource().sendFailure(literal("§c没有待确认的建筑"));
                    return 0;
                }
                BuildingImport.confirm();
                context.getSource().sendSuccess(() -> literal("§a开始导入…"), false);
                return 1;
            }))

            // === 时间与天气 ===

            .then(Commands.literal("time")
                .then(Commands.argument("value", StringArgumentType.word())
                    .suggests(TIME_PRESETS)
                    .executes(context -> setTime(context.getSource(),
                        StringArgumentType.getString(context, "value")))))

            .then(Commands.literal("weather")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests(WEATHER)
                    .executes(context -> setWeather(context.getSource(),
                        StringArgumentType.getString(context, "type")))))

            // === 创造给予 ===

            .then(Commands.literal("give")
                .then(Commands.argument("item", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                        BuiltInRegistries.ITEM.keySet(), builder))
                    .executes(context -> give(context.getSource(),
                        StringArgumentType.getString(context, "item"), 1))
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                        .executes(context -> give(context.getSource(),
                            StringArgumentType.getString(context, "item"),
                            IntegerArgumentType.getInteger(context, "count"))))))

            // 兜底：/createplus <功能 id> 直接开关，省去 toggle。
            // 必须放在所有字面量之后，Brigadier 会优先匹配字面量。
            .then(Commands.argument("feature", StringArgumentType.word())
                .suggests(FEATURES)
                .executes(context -> toggle(context.getSource(),
                    StringArgumentType.getString(context, "feature"))));
    }

    // ==================================================================

    private interface FeatureAction<T extends CreativePlusFeature> {
        void run(T feature);
    }

    private static <T extends CreativePlusFeature> int withFeature(
        CommandSourceStack source, Class<T> type, FeatureAction<T> action) {
        T feature = Createplus.FEATURE_MANAGER.get(type);
        if (feature == null) {
            source.sendFailure(literal("§c功能不可用"));
            return 0;
        }
        if (!feature.isEnabled()) {
            source.sendFailure(literal("§c请先启用功能: §f" + feature.getTranslatedName()
                + " §7(/createplus toggle " + feature.getName() + ")"));
            return 0;
        }
        action.run(feature);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        source.sendSuccess(() -> literal("§b=== CreatePlus 功能 ==="), false);

        for (CreativePlusFeature.Category category : CreativePlusFeature.Category.values()) {
            List<CreativePlusFeature> features = Createplus.FEATURE_MANAGER.getByCategory(category);
            if (features.isEmpty()) continue;

            source.sendSuccess(() -> literal("§8— §7" + category.getDisplayName()), false);
            for (CreativePlusFeature feature : features) {
                MutableComponent line = Component.literal("  ")
                    .append(Component.literal(feature.isEnabled() ? "●" : "○")
                        .withStyle(feature.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY))
                    .append(Component.literal(" " + feature.getTranslatedName())
                        .withStyle(Style.EMPTY
                            .withColor(feature.isEnabled() ? ChatFormatting.WHITE : ChatFormatting.GRAY)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/createplus toggle " + feature.getName()))))
                    .append(Component.literal(" §8" + feature.getName()));
                source.sendSuccess(() -> line, false);
            }
        }

        source.sendSuccess(() -> literal("§7点击功能名即可开关，§f/createplus help§7 查看帮助"), false);
        return 1;
    }

    private static int toggle(CommandSourceStack source, String name) {
        CreativePlusFeature feature = Createplus.FEATURE_MANAGER.getFeature(name);
        if (feature == null) {
            source.sendFailure(literal("§c未找到功能: §f" + name));
            return 0;
        }
        Createplus.FEATURE_MANAGER.toggle(feature);
        source.sendSuccess(() -> literal((feature.isEnabled() ? "§a已开启 " : "§7已关闭 ")
            + "§f" + feature.getTranslatedName()), false);
        return 1;
    }

    private static int set(CommandSourceStack source, String featureName, String settingId, String value) {
        CreativePlusFeature feature = Createplus.FEATURE_MANAGER.getFeature(featureName);
        if (feature == null) {
            source.sendFailure(literal("§c未找到功能: §f" + featureName));
            return 0;
        }
        Setting<?> setting = feature.getSetting(settingId);
        if (setting == null) {
            source.sendFailure(literal("§c功能 §f" + featureName + " §c没有设置项 §f" + settingId));
            return 0;
        }

        try {
            // 所有 Setting 都能从 JSON 基元恢复，字符串同样可以被解析成数字/布尔
            setting.fromJson(new JsonPrimitive(value));
        } catch (RuntimeException e) {
            source.sendFailure(literal("§c无法把 §f" + value + " §c解析为该设置项的值"));
            return 0;
        }

        Createplus.FEATURE_MANAGER.saveConfig();
        source.sendSuccess(() -> literal("§a" + feature.getTranslatedName() + " · "
            + setting.getDisplayName() + " §7→ §f" + setting.getDisplayValue()), false);
        return 1;
    }

    private static int reset(CommandSourceStack source, String featureName) {
        CreativePlusFeature feature = Createplus.FEATURE_MANAGER.getFeature(featureName);
        if (feature == null) {
            source.sendFailure(literal("§c未找到功能: §f" + featureName));
            return 0;
        }
        for (Setting<?> setting : feature.getSettings()) {
            setting.reset();
        }
        Createplus.FEATURE_MANAGER.saveConfig();
        source.sendSuccess(() -> literal("§a已恢复默认设置: §f" + feature.getTranslatedName()), false);
        return 1;
    }

    private static int shape(CommandSourceStack source, String raw, Integer radius, Integer height) {
        ShapeStudio.Shape type;
        try {
            type = ShapeStudio.Shape.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendFailure(literal("§c未知形状: §f" + raw
                + " §7可用: " + String.join(", ", names(ShapeStudio.Shape.values()))));
            return 0;
        }
        return withFeature(source, ShapeStudio.class,
            studio -> studio.configure(type, radius, height));
    }

    private static int shapeInfo(CommandSourceStack source) {
        return withFeature(source, ShapeStudio.class, studio -> {
            for (String line : studio.getDetailedDescription().split("\n")) {
                source.sendSuccess(() -> literal("§7" + line), false);
            }
        });
    }

    /**
     * 落地当前预览。
     *
     * 曲线优先：手上正拿着控制杖点了一路控制点，这时候的 build 显然是要修那条路，
     * 而几何体工坊很可能只是开着没关。
     */
    private static int build(CommandSourceStack source) {
        CurveBuilder curve = Createplus.FEATURE_MANAGER.get(CurveBuilder.class);
        if (curve != null && curve.isEnabled() && curve.pointCount() >= 2) {
            return curve.build() ? 1 : 0;
        }

        ShapeStudio studio = Createplus.FEATURE_MANAGER.get(ShapeStudio.class);
        if (studio != null && studio.isEnabled()) {
            return studio.build() ? 1 : 0;
        }

        source.sendFailure(literal("§c没有待落地的预览。先启用几何体工坊或样条曲线"));
        return 0;
    }

    private static int paletteGrab(CommandSourceStack source) {
        return withFeature(source, PaintPalette.class, palette -> {
            int count = palette.grabFromHotbar();
            if (count == 0) {
                source.sendFailure(literal("§c快捷栏里一个方块都没有"));
                return;
            }
            source.sendSuccess(() -> literal("§a已抓取 §f" + count + " §a份材质"), false);
            palette.weights().forEach((name, weight) ->
                source.sendSuccess(() -> literal("  §f" + name + " §8×" + weight), false));
            source.sendSuccess(() -> literal("§7同一方块多占几格快捷栏就是加权重"), false);
        });
    }

    private static int paletteInfo(CommandSourceStack source) {
        return withFeature(source, PaintPalette.class, palette -> {
            if (palette.size() == 0) {
                source.sendSuccess(() -> literal(
                    "§7调色板是空的。把要混合的方块摆进快捷栏，再 §f/createplus palette grab"), false);
                return;
            }
            source.sendSuccess(() -> literal("§b调色板 §f" + palette.size() + " §b份"), false);
            palette.weights().forEach((name, weight) ->
                source.sendSuccess(() -> literal("  §f" + name + " §8×" + weight), false));
        });
    }

    private static int selectionInfo(CommandSourceStack source) {
        Selection selection = Selection.shared();
        if (!selection.isComplete()) {
            source.sendSuccess(() -> literal("§7当前没有完整选区，用选区工具左右键各点一次"), false);
            return 0;
        }
        source.sendSuccess(() -> literal("§b选区 §f" + selection.describe()
            + " §7从 " + selection.getMin().toShortString()
            + " 到 " + selection.getMax().toShortString()), false);
        return 1;
    }

    private static int export(CommandSourceStack source, String name) {
        BuildingExport feature = Createplus.FEATURE_MANAGER.get(BuildingExport.class);
        if (feature == null || !feature.isEnabled()) {
            source.sendFailure(literal("§c请先启用建筑导出功能"));
            return 0;
        }
        if (!Selection.shared().isComplete()) {
            source.sendFailure(literal("§c请先用选区工具选择区域"));
            return 0;
        }

        try {
            Path file = FileUtils.getBuildingsDirectory().resolve(name + ".mcfunction");
            int count = feature.export(file);
            if (count < 0) {
                source.sendFailure(literal("§c导出失败"));
                return 0;
            }
            source.sendSuccess(() -> literal("§a已导出 §f" + count
                + " §a个方块 → §7createplus/buildings/" + name + ".mcfunction"), false);
            return 1;
        } catch (IOException e) {
            source.sendFailure(literal("§c导出失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int importBuilding(CommandSourceStack source, String name) {
        if (!BlockEdits.canUseCommands()) {
            source.sendFailure(literal("§c导入需要 OP 2 级权限（要用 setblock 命令）"));
            return 0;
        }

        try {
            Path file = FileUtils.getBuildingsDirectory().resolve(name + ".mcfunction");
            if (!Files.exists(file)) {
                source.sendFailure(literal("§c找不到文件: §f" + name + ".mcfunction"));
                return 0;
            }

            int count = BuildingImport.preview(file, name);
            if (count <= 0) {
                source.sendFailure(literal("§c文件里没有可用的 setblock 指令"));
                return 0;
            }

            source.sendSuccess(() -> literal("§a已载入 §f" + count + " §a个方块，绿框为预览（原点已锁定）"), false);
            if (BuildingImport.getOrigin() != null) {
                BlockPos o = BuildingImport.getOrigin();
                source.sendSuccess(() -> literal(
                    "§7原点 §f" + o.getX() + " " + o.getY() + " " + o.getZ()
                        + " §7（走动不影响，确认后按此建造）"), false);
            }
            source.sendSuccess(() -> literal("§7确认导入: §f/createplus confirm§7   取消: §f/createplus cancel"), false);
            return 1;
        } catch (IOException e) {
            source.sendFailure(literal("§c导入失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int setTime(CommandSourceStack source, String raw) {
        TimeWeatherControl feature = Createplus.FEATURE_MANAGER.get(TimeWeatherControl.class);
        if (feature == null) return 0;
        if (!feature.isEnabled()) {
            Createplus.FEATURE_MANAGER.toggle(feature);
        }

        Integer ticks = switch (raw.toLowerCase(Locale.ROOT)) {
            case "sunrise" -> TimeWeatherControl.SUNRISE;
            case "day" -> TimeWeatherControl.DAY;
            case "noon" -> TimeWeatherControl.NOON;
            case "sunset" -> TimeWeatherControl.SUNSET;
            case "night" -> TimeWeatherControl.NIGHT;
            case "midnight" -> TimeWeatherControl.MIDNIGHT;
            default -> null;
        };

        if (ticks == null) {
            try {
                ticks = Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                source.sendFailure(literal("§c时间要么是预设名，要么是 0-23999 的刻数"));
                return 0;
            }
        }

        feature.setTime(ticks);
        Createplus.FEATURE_MANAGER.saveConfig();
        int applied = feature.getTime();
        source.sendSuccess(() -> literal("§a本地时间已设为 §f" + applied + " §7刻"), false);
        return 1;
    }

    private static int setWeather(CommandSourceStack source, String raw) {
        TimeWeatherControl feature = Createplus.FEATURE_MANAGER.get(TimeWeatherControl.class);
        if (feature == null) return 0;
        if (!feature.isEnabled()) {
            Createplus.FEATURE_MANAGER.toggle(feature);
        }

        TimeWeatherControl.Weather weather;
        try {
            weather = TimeWeatherControl.Weather.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendFailure(literal("§c可用天气: unchanged, clear, rain, thunder"));
            return 0;
        }

        feature.setWeather(weather);
        Createplus.FEATURE_MANAGER.saveConfig();
        source.sendSuccess(() -> literal("§a本地天气已设为 §f" + weather.name().toLowerCase(Locale.ROOT)), false);
        return 1;
    }

    /**
     * 创造模式取物。
     *
     * 用创造物品栏数据包，服务端在创造模式下本来就接受这种请求，
     * 所以不需要 OP 权限，也不像旧的 {@code .give} 那样只改客户端物品栏。
     */
    private static int give(CommandSourceStack source, String itemId, int count) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.getConnection() == null) return 0;

        if (!player.isCreative()) {
            source.sendFailure(literal("§c只能在创造模式下使用"));
            return 0;
        }

        ResourceLocation id = ResourceLocation.tryParse(
            itemId.contains(":") ? itemId : "minecraft:" + itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            source.sendFailure(literal("§c未知物品: §f" + itemId));
            return 0;
        }

        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
        stack.setCount(Math.min(count, stack.getMaxStackSize()));

        int slot = player.getInventory().selected;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                slot = i;
                break;
            }
        }

        player.getInventory().setItem(slot, stack);
        mc.getConnection().send(new ServerboundSetCreativeModeSlotPacket(36 + slot, stack.copy()));
        source.sendSuccess(() -> literal("§a已获得 §f" + stack.getCount() + "x "
            + stack.getHoverName().getString()), false);
        return 1;
    }

    private static MutableComponent literal(String text) {
        return Component.literal(text);
    }
}
