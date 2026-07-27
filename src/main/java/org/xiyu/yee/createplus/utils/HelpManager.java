package org.xiyu.yee.createplus.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CreativePlusFeature;
import org.xiyu.yee.createplus.settings.Setting;

import java.util.List;
import java.util.Map;

/**
 * {@code /createplus help} 的内容。
 *
 * 用功能 id 作为键（旧版本用中文显示名，改一次翻译就全部对不上了），
 * 没有专门写操作步骤的功能会回落到它自己的详细描述。
 */
public final class HelpManager {

    private static final Map<String, List<String>> USAGE = Map.ofEntries(
        Map.entry("build_selection", List.of(
            "手持选区棒（默认木铲）左键点方块 = 起点",
            "右键点方块 = 终点",
            "潜行 + 滚轮 = 清空选区",
            "选区会被复制、导出、填充等功能共用")),
        Map.entry("area_fill", List.of(
            "先用选区工具选好范围",
            "主手拿上要填充的方块",
            "按填充键，或输入 /createplus fill [模式]",
            "模式: fill / hollow / walls / frame / replace_air / clear",
            "/createplus undo 撤销（需要 OP 权限）")),
        Map.entry("area_copy", List.of(
            "/createplus copy 复制当前选区",
            "看向目标位置后 /createplus paste 粘贴",
            "/createplus stack <份数> [方向] 沿一个方向阵列重复选区",
            "不写方向时用当前朝向，间距自动等于选区尺寸",
            "Ctrl + 滚轮切换粘贴旋转角度",
            "镜像与锚点可在设置里调整")),
        Map.entry("placement_restriction", List.of(
            "手持方块按住右键，放下的第一个方块成为锚点",
            "之后不符合限制的位置会被直接拦下，不会发包给服务端",
            "松开右键锚点清除，下次按下重新取",
            "连放过程中 Ctrl + 滚轮切换限制方式",
            "同一水平层 / 首次点击的平面 / 同一朝向的面 / 同一竖列 / 同一直线")),
        Map.entry("snap_aim", List.of(
            "开启后视角只会停在步长的整数倍角度上",
            "摆楼梯、栅栏、告示牌时整排朝向必然一致",
            "默认按住潜行可以临时自由转视角",
            "俯仰角吸附可以单独关掉")),
        Map.entry("shape_studio", List.of(
            "/createplus shape <形状> [半径] [高度] 选形状",
            "形状: sphere ellipsoid cylinder cone pyramid torus dome arch helix",
            "半透明预览会跟着准星走，Ctrl + 滚轮实时调半径",
            "/createplus shape pin 把预览钉在原地，再点一次解锁",
            "手持要用的方块，按生成键或 /createplus build 落地",
            "空心、壁厚、朝向轴在设置里调；螺旋的第二个数字是圈数")),
        Map.entry("curve_builder", List.of(
            "手持控制杖（默认烈焰棒）右键加控制点、左键撤销上一个",
            "两个点以上就会连成穿过所有点的平滑曲线",
            "Ctrl + 滚轮调粗细，潜行 + 滚轮清空整条曲线",
            "主手拿着控制杖时，建材取副手里的方块",
            "按生成键或 /createplus build 落地，/createplus curve clear 清空",
            "张力调大会从圆弧逐渐变成折线，闭合选项可以连成环")),
        Map.entry("paint_palette", List.of(
            "把要混合的方块摆进快捷栏，/createplus palette grab 抓取",
            "同一种方块多占几格快捷栏，就是给它加权重",
            "斑块尺寸决定同种材质连成多大一片，换种子换分布",
            "对区域填充、几何体工坊、样条曲线同时生效",
            "/createplus palette list 查看，clear 清空")),
        Map.entry("block_info", List.of(
            "看向方块即可显示 id、坐标、方块状态与光照",
            "楼梯的 half/shape、台阶的 type 都会逐行列出",
            "看向生物时显示实体 id 与生命值",
            "显示位置与各行开关都可以在设置里改")),
        Map.entry("mirror_build", List.of(
            "Ctrl + 右键设置镜像面",
            "Ctrl + 左键清除镜像面",
            "Ctrl + 滚轮切换对称轴 (X / Z / 四向)",
            "之后正常放置方块即可自动对称")),
        Map.entry("area_place", List.of(
            "手持方块，按住右键铺开一片",
            "Ctrl + 滚轮调整半径",
            "形状: 立方体 / 球体 / 圆盘 / 十字")),
        Map.entry("nucker", List.of(
            "按住左键破坏准星周围的方块",
            "Ctrl + 滚轮调整半径",
            "默认会保留脚下那一格，避免踩空")),
        Map.entry("block_color_swap", List.of(
            "手持羊毛、混凝土、玻璃等成组方块",
            "按住修饰键（默认左 Ctrl）滚动滚轮切换",
            "支持羊毛、地毯、混凝土、陶瓦、玻璃、蜡烛、木材等")),
        Map.entry("building_export", List.of(
            "选好区域后 /createplus export <名称>",
            "/createplus import <名称> 载入并预览",
            "/createplus confirm 确认导入，/createplus cancel 取消",
            "文件位于 .minecraft/createplus/buildings/",
            "导入依赖 setblock 命令，需要 OP 2 级权限")),
        Map.entry("freecam", List.of(
            "按 F6 进入/退出（可在控制里改键）",
            "WASD + 空格/潜行 自由飞行，可穿墙",
            "滚轮调节飞行速度",
            "身体停留在原地，服务端看到的位置不变")),
        Map.entry("zoom", List.of(
            "按住缩放键（默认 C）放大视角",
            "缩放时滚动滚轮调节倍率",
            "通过 FOV 事件实现，不会污染视频设置")),
        Map.entry("extended_reach", List.of(
            "开启后自动加长方块与实体交互距离",
            "原版服务端最多允许约 6 格",
            "单人存档可以放到很远")),
        Map.entry("noclip", List.of(
            "关闭碰撞后可直接穿过方块",
            "联机时服务端会把你拉回合法位置",
            "建议只在单人存档使用")),
        Map.entry("light_overlay", List.of(
            "红叉 = 任何时候都能刷怪",
            "黄叉 = 只有夜晚/黑暗时能刷",
            "半径与刷新间隔都可以调，范围越大越吃性能")),
        Map.entry("chunk_borders", List.of(
            "显示周围区块的边界线",
            "可选显示 4x4 网格与子区块分层",
            "红石与刷怪塔对齐时很有用")),
        Map.entry("time_weather", List.of(
            "/createplus time <预设|刻数>",
            "预设: sunrise day noon sunset night midnight",
            "/createplus weather <clear|rain|thunder|unchanged>",
            "只改客户端显示，不影响服务器")),
        Map.entry("performance", List.of(
            "在 选项 → CreatePlus 性能优化 里逐项配置",
            "关闭粒子与实体渲染对大型存档帧数提升明显"))
    );

    private HelpManager() {
    }

    public static void showGeneralHelp(CommandSourceStack source) {
        line(source, "§b=== CreatePlus 帮助 ===");
        line(source, "§7作者 §fMai-xiyu(Mai-xiyu)§7 · 交流群 §f691870136");
        line(source, "§c请勿用于破坏他人存档或服务器");

        line(source, "");
        line(source, "§e按键");
        line(source, "  §fF9§7 快捷菜单   §f右 Shift§7 功能面板");
        line(source, "  §fF6§7 灵魂出窍   §fC§7 视角缩放   §f左 Ctrl§7 修饰键");
        line(source, "  §7生成键（填充选区 / 落地预览）与撤销默认未绑定");
        line(source, "  §7可在 选项 → 控制 → CreatePlus 中设置");

        line(source, "");
        line(source, "§e命令");
        for (String command : List.of(
            "/createplus            列出所有功能",
            "/createplus gui        打开功能面板",
            "/createplus toggle <功能>",
            "/createplus set <功能> <设置项> <值>",
            "/createplus reset <功能>",
            "/createplus sel [clear]  查看/清空选区",
            "/createplus copy | paste | fill [模式] | undo | cancel",
            "/createplus stack <份数> [方向]  沿一个方向阵列重复选区",
            "/createplus shape <形状> [半径] [高度]  参数化几何体",
            "/createplus build            把当前预览落成方块",
            "/createplus palette grab | list | clear",
            "/createplus curve clear      清空样条控制点",
            "/createplus export <名称> | import <名称> | confirm",
            "/createplus time <预设|刻数> | weather <类型>",
            "/createplus give <物品> [数量]",
            "/createplus help [功能]")) {
            line(source, "  §7" + command);
        }

        line(source, "");
        line(source, "§7用 §f/createplus help <功能 id>§7 查看单个功能的用法");
    }

    public static void showFeatureHelp(CommandSourceStack source, String name) {
        CreativePlusFeature feature = Createplus.FEATURE_MANAGER.getFeature(name);
        if (feature == null) {
            source.sendFailure(Component.literal("§c未找到功能: §f" + name));
            return;
        }

        line(source, "§b=== " + feature.getTranslatedName() + " §8(" + feature.getName() + ") §b===");
        line(source, feature.isEnabled() ? "§a当前已开启" : "§7当前已关闭");
        line(source, "");

        for (String descLine : feature.getDetailedDescription().split("\n")) {
            line(source, "§7" + descLine);
        }

        List<String> usage = USAGE.get(feature.getName());
        if (usage != null) {
            line(source, "");
            line(source, "§e用法");
            for (String step : usage) {
                line(source, "  §7" + step);
            }
        }

        if (feature.hasSettings()) {
            line(source, "");
            line(source, "§e设置项");
            for (Setting<?> setting : feature.getSettings()) {
                line(source, "  §f" + setting.getId() + " §8= §7" + setting.getDisplayValue()
                    + " §8(" + setting.getDisplayName() + ")");
            }
            line(source, "  §7修改: §f/createplus set " + feature.getName() + " <设置项> <值>");
        }
    }

    private static void line(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}
