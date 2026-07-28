package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 沿平滑曲线建造。
 *
 * 手点几个控制点，中间用 Catmull-Rom 样条连成一条穿过所有点的曲线，再按粗细
 * 铺成管道。盘山公路、河道、藤蔓、悬索、螺旋坡道这些东西，用长方体选区是
 * 拼不出来的 —— 一段一段贴出来的"曲线"永远能看出台阶感。
 *
 * 曲线本身与体素预览都是实时的，改一个点立刻能看到整条路重新弯过去。
 */
public class CurveBuilder extends CreativePlusFeature implements WorldRenderFeature {

    public enum Wand {
        BLAZE_ROD(Items.BLAZE_ROD),
        STICK(Items.STICK),
        BONE(Items.BONE),
        FEATHER(Items.FEATHER);

        private final Item item;

        Wand(Item item) {
            this.item = item;
        }

        public Item getItem() {
            return item;
        }
    }

    /** 控制点上限。再多的话一次生成的体素量早就超过可建造上限了。 */
    private static final int MAX_POINTS = 24;

    private final EnumSetting<Wand> wand = enumSetting("wand", Wand.BLAZE_ROD);
    private final IntSetting thickness = intSetting("thickness", 1, 1, 6);
    private final DoubleSetting tension = doubleSetting("tension", 0.0D, 0.0D, 1.0D, 0.1D);
    private final BooleanSetting closed = boolSetting("closed", false);
    private final BooleanSetting showPoints = boolSetting("show_points", true);
    /** 曲线用偏暖的琥珀，和几何体工坊的青色区分开。 */
    private final ColorSetting curveColor = colorSetting("curve_color", 0x66FFC24B);

    private final List<BlockPos> points = new ArrayList<>();
    private final Hologram hologram = new Hologram();
    private List<Vec3> path = List.of();
    private String cachedKey = "";

    public CurveBuilder() {
        super("curve_builder", "点几个控制点连成平滑曲线，再铺成有粗细的管道", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        notifyPlayer("§b[CreatePlus] §7样条曲线已启用：手持 §f"
            + wand.get().getItem().getDescription().getString()
            + " §7右键加控制点、左键撤销上一个");
        notifyPlayer("§7Ctrl + 滚轮调粗细，潜行 + 滚轮清空，§f/createplus build§7 落地");
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        clearPoints();
    }

    private boolean holdingWand() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getMainHandItem().is(wand.get().getItem());
    }

    // ==================================================================
    // 控制点
    // ==================================================================

    @SubscribeEvent
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isEnabled() || !holdingWand()) return;
        if (!event.isAttack() && !event.isUseItem()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        if (event.isAttack()) {
            if (points.isEmpty()) {
                notifyActionBar("§7还没有控制点");
            } else {
                points.remove(points.size() - 1);
                notifyActionBar("§c已撤销一个控制点 §7(剩 " + points.size() + ")");
            }
        } else if (points.size() >= MAX_POINTS) {
            notifyActionBar("§c控制点最多 " + MAX_POINTS + " 个");
        } else {
            BlockHitResult hit = (BlockHitResult) mc.hitResult;
            points.add(hit.getBlockPos().relative(hit.getDirection()));
            notifyActionBar("§a控制点 §f" + points.size()
                + " §7· Ctrl+滚轮调粗细 · /createplus build 落地");
        }

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    /** Ctrl + 滚轮调粗细；潜行 + 滚轮清空整条曲线。 */
    @Override
    public boolean handleScroll(double delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || points.isEmpty()) return false;

        if (mc.player.isShiftKeyDown() && holdingWand()) {
            clearPoints();
            notifyActionBar("§c已清空曲线");
            return true;
        }
        if (!KeyBindings.modifierDown()) return false;

        thickness.cycle(delta > 0 ? 1 : -1);
        notifyActionBar("§b曲线粗细: §f" + thickness.value()
            + " §8· §f" + hologram.size() + " §7个方块");
        return true;
    }

    public void clearPoints() {
        points.clear();
        path = List.of();
        hologram.clear();
        cachedKey = "";
    }

    public int pointCount() {
        return points.size();
    }

    @Override
    public void onTick() {
        String key = points.hashCode() + "/" + points.size() + "/" + thickness.value()
            + "/" + tension.value() + "/" + closed.get();
        if (key.equals(cachedKey)) return;
        cachedKey = key;

        if (points.size() < 2) {
            path = List.of();
            hologram.clear();
            return;
        }

        List<Vec3> centers = new ArrayList<>(points.size());
        for (BlockPos point : points) {
            centers.add(Vec3.atCenterOf(point));
        }
        path = Geometry.spline(centers, tension.value(), closed.get());
        // 曲线用世界绝对坐标，锚点固定为原点
        List<BlockPos> voxels = Geometry.tube(path, thickness.value() - 0.5D);
        if (voxels.isEmpty()) {
            hologram.clear();
            notifyActionBar("§c曲线太粗了（上限 " + BlockEdits.maxVolume()
                + " 个体素），减小粗细或缩短路径");
            return;
        }
        hologram.set(voxels);
    }

    // ==================================================================
    // 落地
    // ==================================================================

    /** @return true 表示确实提交了一次生成 */
    public boolean build() {
        if (hologram.isEmpty()) {
            notifyActionBar("§c至少需要 2 个控制点");
            return false;
        }
        if (EditQueue.isBusy()) {
            notifyActionBar("§c上一次操作还没做完");
            return false;
        }

        BlockState material = heldBlock();
        if (material == null) {
            notifyActionBar("§c副手或快捷栏里需要有方块（主手拿着控制杖）");
            return false;
        }

        int placed = hologram.commit(BlockPos.ZERO, "生成曲线", material);
        if (placed < 0) {
            notifyActionBar("§c曲线太粗了（" + hologram.size() + " > " + BlockEdits.maxVolume() + "）");
            return false;
        }

        notifyActionBar("§7开始生成 §f" + placed + " §7个方块");
        return true;
    }

    /**
     * 曲线的建材。
     *
     * 主手多半正拿着控制杖，所以主手不是方块时退回副手 —— 不然每次落地
     * 都得先换一次手上的东西。
     */
    private static BlockState heldBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;

        for (ItemStack stack : List.of(mc.player.getMainHandItem(), mc.player.getOffhandItem())) {
            if (stack.getItem() instanceof BlockItem item) return item.getBlock().defaultBlockState();
        }
        return PaintPalette.active() != null ? Blocks.STONE.defaultBlockState() : null;
    }

    // ==================================================================

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (points.isEmpty()) return;

        int color = curveColor.argb();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        if (!hologram.isEmpty()) {
            hologram.render(poseStack, camera, BlockPos.ZERO,
                r, g, b, ((color >>> 24) & 0xFF) / 255.0F);
        }

        try (RenderUtils.LineBatch batch = RenderUtils.beginWorldLines(poseStack, camera)) {
            // 中心线：即使管道还没成形（只有 1 个点）也能看出走向
            for (int i = 1; i < path.size(); i++) {
                Vec3 from = path.get(i - 1);
                Vec3 to = path.get(i);
                batch.line(from.x, from.y, from.z, to.x, to.y, to.z, 1.0F, 1.0F, 1.0F, 0.9F);
            }
            if (showPoints.get()) {
                for (BlockPos point : points) {
                    batch.box(new AABB(point).inflate(0.06D), r, g, b, 1.0F);
                }
            }
        }

        BlockPos last = points.get(points.size() - 1);
        RenderUtils.drawWorldLabel(poseStack, camera,
            points.size() + " 点 · " + hologram.size() + " 方块",
            last.getX() + 0.5D, last.getY() + 1.6D, last.getZ() + 0.5D, 0xFFFFFFFF);
    }

    @Override
    public String getDetailedDescription() {
        StringBuilder text = new StringBuilder(getTranslatedDescription());
        text.append("\n§7控制杖: §f").append(wand.get().getItem().getDescription().getString());
        text.append("\n§7控制点: §f").append(points.size())
            .append("§7，预览 §f").append(hologram.size()).append(" §7个方块");
        text.append("\n§7右键加点 §8· §7左键撤销 §8· §7潜行+滚轮清空");
        text.append("\n§7Ctrl + 滚轮调粗细 §8· §7/createplus build 落地");
        if (closed.get()) text.append("\n§7闭合成环");
        return text.toString();
    }
}
