package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.DoubleSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.utils.BlockEdits;
import org.xiyu.yee.createplus.utils.EditQueue;
import org.xiyu.yee.createplus.utils.Geometry;
import org.xiyu.yee.createplus.utils.Hologram;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.util.List;

/**
 * 参数化几何体，先在世界里看到再落地。
 *
 * 球、圆环、螺旋、拱门这些形状手工摆要按着计算器一层层数半径，而 WorldEdit
 * 的 //sphere 是盲发 —— 建出来不对只能撤销重来。这里的预览是半透明实体，
 * 跟着准星走，尺寸滚轮实时调，满意了再按生成键落成真方块。
 */
public class ShapeStudio extends CreativePlusFeature implements WorldRenderFeature {

    public enum Shape {
        /** 正球体。 */
        SPHERE,
        /** 可以压扁拉长的椭球。 */
        ELLIPSOID,
        /** 圆柱，塔楼和烟囱。 */
        CYLINDER,
        /** 圆锥，尖顶。 */
        CONE,
        /** 方底棱锥。 */
        PYRAMID,
        /** 圆环，甜甜圈。 */
        TORUS,
        /** 半球穹顶。 */
        DOME,
        /** 椭圆拱门，两条腿自然落地。 */
        ARCH,
        /** 螺旋线，旋转楼梯与藤蔓。 */
        HELIX
    }

    private final EnumSetting<Shape> shape = enumSetting("shape", Shape.SPHERE);
    private final IntSetting radius = intSetting("radius", 8, 1, 64);
    private final IntSetting height = intSetting("height", 12, 1, 128);
    private final IntSetting thickness = intSetting("thickness", 1, 1, 16);
    private final DoubleSetting turns = doubleSetting("turns", 3.0D, 0.5D, 16.0D, 0.5D);
    private final BooleanSetting hollow = boolSetting("hollow", true);
    private final EnumSetting<Direction.Axis> axis = enumSetting("axis", Direction.Axis.Y);

    private final BooleanSetting followCrosshair = boolSetting("follow_crosshair", true);
    /** 默认贴近投影的青色 ghost，alpha 约 40%。 */
    private final ColorSetting previewColor = colorSetting("preview_color", 0x6600C8FF);

    private final Hologram hologram = new Hologram();
    /** 上一次生成用的参数指纹，变了才重算体素。 */
    private String cachedKey = "";
    /** 锁定的锚点；为 null 时预览跟着准星走。 */
    private BlockPos pinned;
    /** 上一次生成是否因为超上限被拒。避免每 tick 刷同一条提示。 */
    private boolean overflow;

    public ShapeStudio() {
        super("shape_studio", "球体、圆环、螺旋、拱门等参数化几何体，带实时全息预览", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        notifyPlayer("§b[CreatePlus] §7几何体工坊已启用：§f/createplus shape <形状> [半径] [高度]");
        notifyPlayer("§7Ctrl + 滚轮调半径，按生成键或 §f/createplus build§7 落地");
    }

    @Override
    public void onDisable() {
        hologram.clear();
        cachedKey = "";
        pinned = null;
        overflow = false;
    }

    // ==================================================================
    // 参数
    // ==================================================================

    public void configure(Shape newShape, Integer newRadius, Integer newHeight) {
        shape.set(newShape);
        if (newRadius != null) radius.set(newRadius);
        if (newHeight != null) newHeightOrTurns(newHeight);
        pinned = null;
        notifyActionBar("§b" + shape.getDisplayValue() + " §7" + describeParams());
    }

    private void newHeightOrTurns(int value) {
        // 螺旋没有"高度"这个直觉参数，第二个数字理解成圈数更顺手
        if (shape.get() == Shape.HELIX) {
            turns.set((double) value);
        } else {
            height.set(value);
        }
    }

    /** 把预览钉在当前位置，或解除钉住。 */
    public void togglePin() {
        if (pinned != null) {
            pinned = null;
            notifyActionBar("§7预览已解锁，重新跟随准星");
            return;
        }
        BlockPos anchor = resolveAnchor();
        if (anchor == null) {
            notifyActionBar("§c请先看向一个方块");
            return;
        }
        pinned = anchor;
        notifyActionBar("§a预览已钉在 §f" + anchor.toShortString());
    }

    /** Ctrl + 滚轮调半径。有预览时优先于方块变色，因为这时候玩家显然在调形状。 */
    @Override
    public boolean handleScroll(double delta) {
        if (!KeyBindings.modifierDown() || hologram.isEmpty()) return false;
        radius.cycle(delta > 0 ? 1 : -1);
        notifyActionBar("§b" + shape.getDisplayValue() + " §7" + describeParams()
            + " §8· §f" + hologram.size() + " §7个方块");
        return true;
    }

    @Override
    public void onTick() {
        String key = cacheKey();
        if (key.equals(cachedKey)) return;
        cachedKey = key;

        List<BlockPos> voxels = generate();
        // Geometry 超限时返回空而不是截断，避免画出半个球让人以为尺寸对。
        if (voxels.isEmpty()) {
            hologram.clear();
            if (!overflow) {
                overflow = true;
                notifyActionBar("§c形状太大了（上限 " + BlockEdits.maxVolume()
                    + " 个体素），缩小半径或改成空心");
            }
            return;
        }
        overflow = false;
        hologram.set(voxels);
    }

    private String cacheKey() {
        return shape.get() + "/" + radius.value() + "/" + height.value() + "/" + thickness.value()
            + "/" + turns.value() + "/" + hollow.get() + "/" + axis.get();
    }

    private List<BlockPos> generate() {
        int r = radius.value();
        int h = height.value();
        int t = thickness.value();
        boolean isHollow = hollow.get();

        return switch (shape.get()) {
            case SPHERE -> Geometry.sphere(r, isHollow, t);
            case ELLIPSOID -> Geometry.ellipsoid(r, Math.max(1, h / 2), r, isHollow, t);
            case CYLINDER -> Geometry.cylinder(r, h, axis.get(), isHollow, t);
            case CONE -> Geometry.cone(r, h, isHollow, t);
            case PYRAMID -> Geometry.pyramid(r, h, isHollow, t);
            case TORUS -> Geometry.torus(r, Math.max(1, t), axis.get());
            case DOME -> Geometry.dome(r, isHollow, t);
            case ARCH -> Geometry.arch(r * 2, h, Math.max(1, t), Math.max(1, t));
            case HELIX -> Geometry.helix(r, h, turns.value(), t);
        };
    }

    // ==================================================================
    // 落地
    // ==================================================================

    /** @return true 表示确实提交了一次生成 */
    public boolean build() {
        if (hologram.isEmpty()) {
            notifyActionBar("§c还没有预览，先用 /createplus shape 选一个形状");
            return false;
        }
        if (EditQueue.isBusy()) {
            notifyActionBar("§c上一次操作还没做完");
            return false;
        }

        BlockPos anchor = resolveAnchor();
        if (anchor == null) {
            notifyActionBar("§c请先看向一个方块");
            return false;
        }

        BlockState material = heldBlock();
        if (material == null) {
            notifyActionBar("§c主手需要手持一个方块");
            return false;
        }

        int placed = hologram.commit(anchor, "生成" + shape.getDisplayValue(), material);
        if (placed < 0) {
            notifyActionBar("§c形状太大了（" + hologram.size() + " > " + BlockEdits.maxVolume() + "）");
            return false;
        }

        notifyActionBar("§7开始生成 §f" + placed + " §7个方块");
        return true;
    }

    private static BlockState heldBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;

        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof BlockItem item) return item.getBlock().defaultBlockState();
        // 调色板自己带材质，这时候手上拿什么都无所谓
        return PaintPalette.active() != null ? Blocks.STONE.defaultBlockState() : null;
    }

    /**
     * 预览的落点。
     *
     * 球、椭球、圆环以准星方块为中心，其余以它为底面 —— 塔楼和拱门要是也
     * 从中心长，一半会埋进地里。
     */
    private BlockPos resolveAnchor() {
        if (pinned != null) return pinned;

        Minecraft mc = Minecraft.getInstance();
        if (!followCrosshair.get() && mc.player != null) return mc.player.blockPosition();
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;

        BlockHitResult hit = (BlockHitResult) mc.hitResult;
        return hit.getBlockPos().relative(hit.getDirection());
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (hologram.isEmpty()) return;

        BlockPos anchor = resolveAnchor();
        if (anchor == null) return;

        int color = previewColor.argb();
        hologram.render(poseStack, camera, anchor,
            ((color >> 16) & 0xFF) / 255.0F,
            ((color >> 8) & 0xFF) / 255.0F,
            (color & 0xFF) / 255.0F,
            ((color >>> 24) & 0xFF) / 255.0F);

        RenderUtils.drawWorldLabel(poseStack, camera,
            shape.getDisplayValue() + " · " + hologram.size() + " 方块"
                + (pinned != null ? " · 已钉住" : ""),
            anchor.getX() + 0.5D, anchor.getY() + 1.6D, anchor.getZ() + 0.5D, 0xFFFFFFFF);
    }

    private String describeParams() {
        return switch (shape.get()) {
            case SPHERE, DOME -> "半径 " + radius.value();
            case ELLIPSOID -> "半径 " + radius.value() + " 高 " + height.value();
            case CYLINDER, CONE, PYRAMID -> "半径 " + radius.value() + " 高 " + height.value();
            case TORUS -> "环半径 " + radius.value() + " 管半径 " + thickness.value();
            case ARCH -> "跨度 " + radius.value() * 2 + " 高 " + height.value();
            case HELIX -> "半径 " + radius.value() + " 高 " + height.value()
                + " " + turns.getDisplayValue() + " 圈";
        };
    }

    @Override
    public String getDetailedDescription() {
        StringBuilder text = new StringBuilder(getTranslatedDescription());
        text.append("\n§7形状: §f").append(shape.getDisplayValue())
            .append(" §7(").append(describeParams()).append(")");
        text.append("\n§7预览: §f").append(hologram.isEmpty() ? "无" : hologram.size() + " 个方块");
        text.append("\n§7/createplus shape <形状> [半径] [高度]");
        text.append("\n§7/createplus build 落地 §8· §7/createplus shape pin 钉住预览");
        text.append("\n§7Ctrl + 滚轮调半径");
        if (PaintPalette.active() != null) {
            text.append("\n§a调色板生效中，将按噪声混合材质");
        }
        return text.toString();
    }
}
