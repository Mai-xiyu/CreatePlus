package org.xiyu.yee.createplus.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CreativePlusFeature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelpManager {
    private static final Map<String, FeatureHelp> featureHelps = new HashMap<>();

    // 功能帮助的数据结构
    private record FeatureHelp(
        List<String> usage,      // 使用方法
        List<String> notes,      // 注意事项（可选）
        List<String> examples    // 示例（可选）
    ) {}

    // 初始化所有功能的帮助信息
    static {
        // 建筑导出功能帮助
        featureHelps.put("建筑导出", new FeatureHelp(
            List.of(
                "1. 使用木铲右键选择第一个点",
                "2. 使用木铲右键选择第二个点",
                "3. 输入 /exportbuilding <名称>",
                "4. 建筑会保存到 buildings/<名称>.mcfunction"
            ),
            List.of("导出的建筑文件可以在其他存档中使用"),
            List.of(
                "/exportbuilding myhouse",
                "/exportbuilding tower1"
            )
        ));

        // 镜像建造功能帮助
        featureHelps.put("镜像建造", new FeatureHelp(
            List.of(
                "1. 使用木剑右键选择镜像点",
                "2. Shift+右键切换镜像轴(X/Y/Z)",
                "3. 放置方块时会自动在对称位置放置",
                "4. 再次右键镜像点可以移动位置"
            ),
            List.of("镜像点会以蓝色粒子效果显示"),
            List.of("适合建造对称的建筑，如城堡、神殿等")
        ));

        // 方块变色功能帮助
        featureHelps.put("方块变色", new FeatureHelp(
            List.of(
                "1. 手持可变色方块(如羊毛、玻璃等)",
                "2. 按住左CTRL并滚动鼠标滚轮",
                "3. 方块会在同类型中循环切换"
            ),
            List.of(
                "支持的方块类型:",
                "- 羊毛、混凝土、玻璃",
                "- 地毯、陶瓦、床",
                "- 旗帜、潜影盒等"
            ),
            null
        ));

        // 快速建造功能帮助
        featureHelps.put("快速建造", new FeatureHelp(
            List.of(
                "1. 开启功能后自动生效",
                "2. 移除方块放置的冷却时间",
                "3. 可以快速连续放置方块"
            ),
            List.of("仅在创造模式下有效"),
            null
        ));

        // 灵魂出窍功能帮助
        featureHelps.put("灵魂出窍", new FeatureHelp(
            List.of(
                "1. 按 F6 开启/关闭灵魂出窍",
                "2. 使用 WASD 和空格键飞行",
                "3. 使用鼠标滚轮调节飞行速度",
                "4. 再次按 F6 或死亡时自动退出"
            ),
            List.of(
                "- 可以穿墙观察",
                "- 不会影响玩家实际位置",
                "- 适合查看地形和建筑"
            ),
            List.of("观察地下建筑或复杂结构时特别有用")
        ));

        // 长臂猿功能帮助
        featureHelps.put("长臂猿", new FeatureHelp(
            List.of(
                "1. 开启功能后自动装备特殊裤子",
                "2. 增加放置和破坏方块的距离",
                "3. 关闭功能后自动恢复原装备"
            ),
            List.of(
                "- 仅在创造模式下有效",
                "- 不会影响生存模式的平衡性"
            ),
            null
        ));

        // 夜视功能帮助
        featureHelps.put("夜视", new FeatureHelp(
            List.of(
                "1. 开启功能后自动生效",
                "2. 提高游戏整体亮度",
                "3. 在黑暗处也能看清周围"
            ),
            List.of("不需要使用夜视药水效果"),
            null
        ));

        // 缩放功能帮助
        featureHelps.put("缩放", new FeatureHelp(
            List.of(
                "1. 按住 C 键进行缩放",
                "2. 使用鼠标滚轮调节缩放倍率",
                "3. 松开 C 键恢复正常视角"
            ),
            List.of(
                "- 默认缩放倍率为 4 倍",
                "- 可在 1-10 倍之间调节"
            ),
            null
        ));

        // 速度调节功能帮助
        featureHelps.put("速度调节", new FeatureHelp(
            List.of(
                "1. 开启功能后使用快捷键调节",
                "2. Alt + 鼠标滚轮调节行走速度",
                "3. Alt + Shift + 鼠标滚轮调节飞行速度"
            ),
            List.of(
                "- 仅在创造模式下有效",
                "- 可以精确控制移动速度",
                "- 适合精细建造时使用"
            ),
            List.of(
                "建议在需要精确移动时降低速度",
                "在长距离移动时提高速度"
            )
        ));

        // 范围建造功能帮助
        featureHelps.put("范围建造", new FeatureHelp(
            List.of(
                "1. 副手持方块时自动启用",
                "2. 在目标位置形成球形放置区域",
                "3. 使用鼠标滚轮调节范围大小",
                "4. 左键放置方块，右键清除方块"
            ),
            List.of(
                "- 适合快速填充大面积区域",
                "- 可用于创建球形或圆形结构",
                "- 支持所有类型的方块"
            ),
            List.of(
                "创建球形建筑",
                "快速填充空洞",
                "批量清除方块"
            )
        ));

        // 建筑导入功能帮助
        featureHelps.put("建筑导入", new FeatureHelp(
            List.of(
                "1. 使用 /importbuilding <名称> 选择建筑",
                "2. 会显示绿色预览框",
                "3. 使用 /confirmimport 确认导入",
                "4. 使用 /cancelimport 取消导入"
            ),
            List.of(
                "- 需要OP权限(2级)才能导入",
                "- 建筑文件必须位于 buildings 文件夹",
                "- 支持导入其他存档的建筑"
            ),
            List.of(
                "/importbuilding myhouse",
                "/confirmimport",
                "/cancelimport"
            )
        ));
    }

    // 显示通用帮助
    public static void showGeneralHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== CreatePlus 功能帮助 ==="), false);
        
        // 作者信息
        source.sendSuccess(() -> Component.literal("\n§d作者信息:"), false);
        source.sendSuccess(() -> Component.literal("§7作者: §b饩雨(God_xiyu)"), false);
        source.sendSuccess(() -> Component.literal("§7交流群: §b691870136"), false);
        source.sendSuccess(() -> Component.literal("§c请勿使用本模组进行恶意破坏，违者将被云黑名单封禁！"), false);
        
        // 命令帮助
        source.sendSuccess(() -> Component.literal("\n§e命令帮助:"), false);
        showCommandHelp(source);

        source.sendSuccess(() -> Component.literal("\n§e使用 §6/features help <功能名> §e查看具体功能的详细帮助"), false);
    }

    // 显示命令帮助
    public static void showCommandHelp(CommandSourceStack source) {
        List<String> commands = List.of(
            "/features - 显示所有功能",
            "/features <功能名> - 开启/关闭功能",
            "/features help - 显示此帮助",
            "/features help <功能名> - 显示指定功能的详细帮助",
            ".give <玩家> <物品>[{nbt}] [数量] - 快速给予物品",
            "/exportbuilding <名称> - 导出选区建筑",
            "/importbuilding <名称> - 导入建筑",
            "/confirmimport - 确认导入建筑",
            "/cancelimport - 取消导入建筑"
        );

        for (String cmd : commands) {
            source.sendSuccess(() -> Component.literal("§7" + cmd), false);
        }
    }

    // 显示特定功能的帮助
    public static void showFeatureHelp(CommandSourceStack source, String featureName) {
        var features = Createplus.FEATURE_MANAGER.getFeatures();
        CreativePlusFeature targetFeature = features.stream()
            .filter(f -> f.getName().equals(featureName))
            .findFirst()
            .orElse(null);

        if (targetFeature == null) {
            source.sendFailure(Component.literal("§c未找到功能: §f" + featureName));
            return;
        }

        // 显示功能标题
        source.sendSuccess(() -> Component.literal("§6=== " + featureName + " 功能帮助 ==="), false);
        
        // 显示功能描述
        source.sendSuccess(() -> Component.literal("\n§e功能描述:"), false);
        source.sendSuccess(() -> Component.literal("§7" + targetFeature.getDescription()), false);

        // 获取并显示功能的详细帮助
        FeatureHelp help = featureHelps.get(featureName);
        if (help != null) {
            // 显示使用方法
            source.sendSuccess(() -> Component.literal("\n§e使用方法:"), false);
            help.usage.forEach(line -> 
                source.sendSuccess(() -> Component.literal("§7" + line), false));

            // 显示注意事项（如果有）
            if (help.notes != null && !help.notes.isEmpty()) {
                source.sendSuccess(() -> Component.literal("\n§e注意事项:"), false);
                help.notes.forEach(line -> 
                    source.sendSuccess(() -> Component.literal("§7" + line), false));
            }

            // 显示示例（如果有）
            if (help.examples != null && !help.examples.isEmpty()) {
                source.sendSuccess(() -> Component.literal("\n§e示例:"), false);
                help.examples.forEach(line -> 
                    source.sendSuccess(() -> Component.literal("§7" + line), false));
            }
        } else {
            // 对于没有特定帮助信息的功能，显示默认帮助
            source.sendSuccess(() -> Component.literal("\n§e使用方法:"), false);
            source.sendSuccess(() -> Component.literal("§7使用 /features " + featureName + " 开启/关闭此功能"), false);
        }

        // 显示当前状态
        String status = targetFeature.isEnabled() ? "§a[已启用]" : "§7[已禁用]";
        source.sendSuccess(() -> Component.literal("\n§e当前状态: " + status), false);
    }
} 