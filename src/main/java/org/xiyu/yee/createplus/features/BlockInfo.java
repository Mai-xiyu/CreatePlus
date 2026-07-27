package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.ui.Theme;
import org.xiyu.yee.createplus.utils.HudLayout;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 显示准星指向的方块或实体的详细信息。
 *
 * F3 只告诉你方块 id，但建筑里真正要对齐的是方块状态——楼梯的 half 和 shape、
 * 台阶的 type、栅栏的连接方向。这些在这里一行一个列出来，照着抄就能拼出
 * 完全一样的形状。
 */
public class BlockInfo extends CreativePlusFeature implements HudRenderFeature {

    private final EnumSetting<MiniHUD.Corner> corner = enumSetting("corner", MiniHUD.Corner.TOP_RIGHT);
    private final IntSetting offsetX = intSetting("offset_x", 4, 0, 200);
    private final IntSetting offsetY = intSetting("offset_y", 4, 0, 200);
    private final IntSetting lineHeight = intSetting("line_height", 10, 8, 16);
    private final IntSetting maxProperties = intSetting("max_properties", 10, 1, 24);

    private final BooleanSetting showId = boolSetting("show_id", true);
    private final BooleanSetting showPos = boolSetting("show_pos", true);
    private final BooleanSetting showState = boolSetting("show_state", true);
    private final BooleanSetting showLight = boolSetting("show_light", true);
    private final BooleanSetting showEntity = boolSetting("show_entity", true);

    private final BooleanSetting background = boolSetting("background", true);
    private final ColorSetting backgroundColor = colorSetting("background_color", 0x90000000);
    private final ColorSetting labelColor = colorSetting("label_color", 0xFF9AA4B2);
    private final ColorSetting valueColor = colorSetting("value_color", 0xFFFFFFFF);

    private record Line(String label, String value) {
    }

    public BlockInfo() {
        super("block_info", "显示准星所指方块的 id、方块状态与光照", Category.HUD);
    }

    @Override
    public void renderHud(GuiGraphics graphics, int screenWidth, int screenHeight, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.hitResult == null) return;

        List<Line> lines = collect(mc);
        if (lines.isEmpty()) return;

        int lh = lineHeight.value();
        int gap = 6;
        int textWidth = 0;
        for (Line line : lines) {
            textWidth = Math.max(textWidth, mc.font.width(line.label()) + gap + mc.font.width(line.value()));
        }

        int padding = background.get() ? 3 : 0;
        int panelWidth = textWidth + padding * 2;
        int panelHeight = lines.size() * lh + padding * 2;

        MiniHUD.Corner where = corner.get();
        boolean right = where == MiniHUD.Corner.TOP_RIGHT || where == MiniHUD.Corner.BOTTOM_RIGHT;
        boolean bottom = where == MiniHUD.Corner.BOTTOM_LEFT || where == MiniHUD.Corner.BOTTOM_RIGHT;

        int x;
        int y;
        if (HudLayout.hasCustom(HudLayout.BLOCK_INFO)) {
            HudLayout.Pos pos = HudLayout.get(HudLayout.BLOCK_INFO);
            x = pos.x;
            y = pos.y;
            right = false;
        } else {
            x = right ? screenWidth - offsetX.value() - panelWidth : offsetX.value();
            y = bottom ? screenHeight - offsetY.value() - panelHeight : offsetY.value();
        }

        if (background.get()) {
            RenderUtils.drawRoundedRect(graphics, x, y, panelWidth, panelHeight,
                Theme.CORNER, backgroundColor.argb());
        }

        int textY = y + padding;
        for (Line line : lines) {
            if (right) {
                RenderUtils.drawRightAlignedText(graphics, line.value(),
                    x + panelWidth - padding, textY, valueColor.argb(), true);
                RenderUtils.drawRightAlignedText(graphics, line.label(),
                    x + panelWidth - padding - mc.font.width(line.value()) - gap, textY,
                    labelColor.argb(), true);
            } else {
                graphics.drawString(mc.font, line.label(), x + padding, textY, labelColor.argb(), true);
                graphics.drawString(mc.font, line.value(),
                    x + padding + mc.font.width(line.label()) + gap, textY, valueColor.argb(), true);
            }
            textY += lh;
        }
    }

    private List<Line> collect(Minecraft mc) {
        List<Line> lines = new ArrayList<>();

        if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
            if (showEntity.get()) {
                collectEntity(((EntityHitResult) mc.hitResult).getEntity(), lines);
            }
            return lines;
        }
        if (mc.hitResult.getType() != HitResult.Type.BLOCK) return lines;

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return lines;

        lines.add(new Line("§f" + state.getBlock().getName().getString(), ""));
        if (showId.get()) {
            lines.add(new Line("ID", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));
        }
        if (showPos.get()) {
            lines.add(new Line("坐标", pos.toShortString()));
        }
        if (showState.get()) {
            int shown = 0;
            for (Property<?> property : state.getProperties()) {
                if (shown++ >= maxProperties.value()) {
                    lines.add(new Line("…", "还有 " + (state.getProperties().size() - shown + 1) + " 项"));
                    break;
                }
                lines.add(new Line(property.getName(), valueOf(state, property)));
            }
        }
        if (showLight.get()) {
            lines.add(new Line("光照", mc.level.getBrightness(LightLayer.BLOCK, pos)
                + " §7天光 §f" + mc.level.getBrightness(LightLayer.SKY, pos)));
        }
        return lines;
    }

    private void collectEntity(Entity entity, List<Line> lines) {
        lines.add(new Line("§f" + entity.getDisplayName().getString(), ""));
        if (showId.get()) {
            lines.add(new Line("ID", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()));
        }
        if (showPos.get()) {
            lines.add(new Line("坐标", String.format("%.1f %.1f %.1f",
                entity.getX(), entity.getY(), entity.getZ())));
        }
        if (entity instanceof LivingEntity living) {
            lines.add(new Line("生命", String.format("%.1f §7/ §f%.1f",
                living.getHealth(), living.getMaxHealth())));
        }
    }

    private static <T extends Comparable<T>> String valueOf(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7位置: §f" + corner.getDisplayValue()
            + "\n§7看向方块时显示方块状态，看向生物时显示生命值";
    }
}
