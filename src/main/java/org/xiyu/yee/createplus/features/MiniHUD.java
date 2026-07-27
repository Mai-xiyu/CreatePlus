package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.DoubleSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.ui.FeatureScreen;
import org.xiyu.yee.createplus.ui.Theme;
import org.xiyu.yee.createplus.utils.HudLayout;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 屏幕角落的信息面板。
 *
 * 每一行都能单独开关，位置、颜色、背景也都可配置。旧实现是把 5 行硬编码在
 * 左上角，还用 {@code .hudcolor} 这种聊天指令改颜色，改完重启就丢。
 */
public class MiniHUD extends CreativePlusFeature implements HudRenderFeature {

    public enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private static final SimpleDateFormat CLOCK = new SimpleDateFormat("HH:mm:ss");
    private static final String[] FACINGS = {"南 +Z", "西 -X", "北 -Z", "东 +X"};

    private final EnumSetting<Corner> corner = enumSetting("corner", Corner.TOP_LEFT);
    private final IntSetting offsetX = intSetting("offset_x", 4, 0, 200);
    private final IntSetting offsetY = intSetting("offset_y", 4, 0, 200);
    private final IntSetting lineHeight = intSetting("line_height", 10, 8, 16);

    private final BooleanSetting background = boolSetting("background", true);
    private final BooleanSetting shadow = boolSetting("shadow", true);
    private final ColorSetting backgroundColor = colorSetting("background_color", 0x90000000);
    private final ColorSetting labelColor = colorSetting("label_color", 0xFF9AA4B2);
    private final ColorSetting valueColor = colorSetting("value_color", 0xFFFFFFFF);
    /** F9 快捷菜单 / 信息栏整体透明度，从 ClickGUI 顶栏挪到这里。 */
    private final DoubleSetting hudOpacity = doubleSetting("hud_opacity", 0.95D, 0.2D, 1.0D, 0.05D);

    private final BooleanSetting showFps = boolSetting("show_fps", true);
    private final BooleanSetting showCoords = boolSetting("show_coords", true);
    private final BooleanSetting showChunk = boolSetting("show_chunk", false);
    private final BooleanSetting showFacing = boolSetting("show_facing", true);
    private final BooleanSetting showBiome = boolSetting("show_biome", true);
    private final BooleanSetting showDimension = boolSetting("show_dimension", false);
    private final BooleanSetting showLight = boolSetting("show_light", false);
    private final BooleanSetting showGameTime = boolSetting("show_game_time", true);
    private final BooleanSetting showRealTime = boolSetting("show_real_time", false);
    private final BooleanSetting showSpeed = boolSetting("show_speed", false);
    private final BooleanSetting showEntities = boolSetting("show_entities", false);

    private record Line(String label, String value) {
    }

    public MiniHUD() {
        super("mini_hud", "屏幕角落显示坐标、区块、群系等信息", Category.HUD);
        hudOpacity.onChange(v -> FeatureScreen.setOpacity(v.floatValue()));
    }

    /** 与配置加载同步：把信息栏透明度写回 F9 菜单。 */
    public void syncMenuOpacity() {
        FeatureScreen.setOpacity((float) hudOpacity.value());
    }

    public float getHudOpacity() {
        return (float) hudOpacity.value();
    }

    @Override
    public void renderHud(GuiGraphics graphics, int screenWidth, int screenHeight, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        List<Line> lines = collect(mc, player);
        if (lines.isEmpty()) return;

        float opacity = getHudOpacity();
        int lh = lineHeight.value();
        int gap = 6;
        int textWidth = 0;
        for (Line line : lines) {
            int width = mc.font.width(line.label()) + gap + mc.font.width(line.value());
            textWidth = Math.max(textWidth, width);
        }

        int padding = background.get() ? 3 : 0;
        int panelWidth = textWidth + padding * 2;
        int panelHeight = lines.size() * lh + padding * 2;

        boolean right = corner.get() == Corner.TOP_RIGHT || corner.get() == Corner.BOTTOM_RIGHT;
        boolean bottom = corner.get() == Corner.BOTTOM_LEFT || corner.get() == Corner.BOTTOM_RIGHT;

        int x;
        int y;
        if (HudLayout.hasCustom(HudLayout.MINI_HUD)) {
            HudLayout.Pos pos = HudLayout.get(HudLayout.MINI_HUD);
            x = pos.x;
            y = pos.y;
            right = false; // 自定义布局按左对齐画
        } else {
            x = right ? screenWidth - offsetX.value() - panelWidth : offsetX.value();
            y = bottom ? screenHeight - offsetY.value() - panelHeight : offsetY.value();
        }

        if (background.get()) {
            RenderUtils.drawRoundedRect(graphics, x, y, panelWidth, panelHeight,
                Theme.CORNER, RenderUtils.withAlpha(backgroundColor.argb(), opacity));
        }

        int textY = y + padding;
        int labelArgb = RenderUtils.withAlpha(labelColor.argb(), opacity);
        int valueArgb = RenderUtils.withAlpha(valueColor.argb(), opacity);
        for (Line line : lines) {
            if (right) {
                RenderUtils.drawRightAlignedText(graphics, line.value(),
                    x + panelWidth - padding, textY, valueArgb, shadow.get());
                int valueWidth = mc.font.width(line.value());
                RenderUtils.drawRightAlignedText(graphics, line.label(),
                    x + panelWidth - padding - valueWidth - gap, textY, labelArgb, shadow.get());
            } else {
                graphics.drawString(mc.font, line.label(), x + padding, textY, labelArgb, shadow.get());
                graphics.drawString(mc.font, line.value(),
                    x + padding + mc.font.width(line.label()) + gap, textY, valueArgb, shadow.get());
            }
            textY += lh;
        }
    }

    private List<Line> collect(Minecraft mc, LocalPlayer player) {
        List<Line> lines = new ArrayList<>();
        BlockPos pos = player.blockPosition();

        if (showFps.get()) {
            lines.add(new Line("FPS", String.valueOf(mc.getFps())));
        }
        if (showCoords.get()) {
            lines.add(new Line("坐标", String.format("%.1f %.1f %.1f",
                player.getX(), player.getY(), player.getZ())));
        }
        if (showChunk.get()) {
            lines.add(new Line("区块", String.format("%d %d §7内 §f%d %d",
                pos.getX() >> 4, pos.getZ() >> 4, pos.getX() & 15, pos.getZ() & 15)));
        }
        if (showFacing.get()) {
            lines.add(new Line("朝向", FACINGS[Math.floorMod(
                Math.round(player.getYRot() / 90.0F), 4)]));
        }
        if (showBiome.get()) {
            lines.add(new Line("群系", mc.level.getBiome(pos).unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("unknown")));
        }
        if (showDimension.get()) {
            lines.add(new Line("维度", mc.level.dimension().location().getPath()));
        }
        if (showLight.get()) {
            lines.add(new Line("光照", mc.level.getBrightness(LightLayer.BLOCK, pos)
                + " §7天光 §f" + mc.level.getBrightness(LightLayer.SKY, pos)));
        }
        if (showGameTime.get()) {
            long dayTime = mc.level.getDayTime();
            lines.add(new Line("时间", String.format("第 %d 天 %s",
                dayTime / 24000L, formatDayTime(dayTime % 24000L))));
        }
        if (showRealTime.get()) {
            lines.add(new Line("现实", CLOCK.format(new Date())));
        }
        if (showSpeed.get()) {
            double vx = player.getX() - player.xo;
            double vz = player.getZ() - player.zo;
            lines.add(new Line("速度", String.format("%.2f m/s",
                Math.sqrt(vx * vx + vz * vz) * 20.0D)));
        }
        if (showEntities.get()) {
            int count = 0;
            for (var ignored : mc.level.entitiesForRendering()) {
                count++;
            }
            lines.add(new Line("实体", String.valueOf(count)));
        }
        return lines;
    }

    /** 把 0~23999 的游戏刻转换成 24 小时制。游戏内 0 刻是早上 6 点。 */
    private static String formatDayTime(long ticks) {
        long minutesTotal = (ticks * 60L / 1000L + 360L) % 1440L;
        return String.format("%02d:%02d", minutesTotal / 60L, minutesTotal % 60L);
    }

    @Override
    public String getDetailedDescription() {
        int active = 0;
        for (var setting : getSettings()) {
            if (setting.getId().startsWith("show_") && Boolean.TRUE.equals(setting.get())) active++;
        }
        return getTranslatedDescription()
            + "\n§7位置: §f" + corner.getDisplayValue()
            + "\n§7已启用 §f" + active + " §7行信息"
            + "\n§7在设置界面里可以逐行开关";
    }
}
