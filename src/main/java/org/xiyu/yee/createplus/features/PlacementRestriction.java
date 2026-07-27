package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.RenderUtils;

/**
 * 按住右键连续放置时，把方块锁在一个平面、一条线或一列上。
 *
 * 徒手铺地板最烦的就是准星滑到侧面，方块直接砌上去一格。按住右键时
 * 第一次放置的位置会成为锚点，之后任何不符合限制的位置都会被拦下来，
 * 松开右键锚点自动清除。
 *
 * 拦截用的是 {@link InputEvent.InteractionKeyMappingTriggered} 而不是
 * {@code PlayerInteractEvent.RightClickBlock}：后者在 1.20.1 里被取消之后
 * 客户端依然会把 ServerboundUseItemOnPacket 发出去，服务端照样会放方块。
 */
public class PlacementRestriction extends CreativePlusFeature implements WorldRenderFeature {

    public enum Mode {
        /** 只能放在锚点所在的水平层。 */
        LAYER,
        /** 只能放在锚点那个点击面所决定的平面上。 */
        PLANE,
        /** 只能点朝向相同的面。 */
        FACE,
        /** 只能放在锚点正上下的那一竖列。 */
        COLUMN,
        /** 只能沿着离开锚点的第一个方向铺一条直线。 */
        LINE
    }

    private final EnumSetting<Mode> mode = enumSetting("mode", Mode.LAYER);
    private final BooleanSetting showAnchor = boolSetting("show_anchor", true);
    private final ColorSetting anchorColor = colorSetting("anchor_color", 0xFF52D273);

    private BlockPos anchor;
    private Direction anchorFace;
    /** LINE 模式下第一次离开锚点时锁定的轴，之后只认这个方向。 */
    private Direction.Axis lineAxis;

    public PlacementRestriction() {
        super("placement_restriction", "按住右键连放时把方块锁在同一层、同一面或同一条线上", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        notifyPlayer("§b[CreatePlus] §7放置限制已启用：§f" + mode.getDisplayValue()
            + "§7，按住右键时 Ctrl + 滚轮切换模式");
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        clearAnchor();
    }

    @Override
    public void onTick() {
        // 松开右键就结束这一次连放，下次按下重新取锚点
        if (anchor != null && !Minecraft.getInstance().options.keyUse.isDown()) {
            clearAnchor();
        }
    }

    /**
     * Ctrl + 滚轮切换限制模式。
     *
     * 只在连放过程中接管滚轮：拿着方块按 Ctrl 滚轮是"方块变色"的触发条件，
     * 平时抢过来的话就没法换材质了。
     */
    @Override
    public boolean handleScroll(double delta) {
        if (anchor == null || !KeyBindings.modifierDown()) return false;
        mode.cycle(delta > 0 ? 1 : -1);
        // 换模式相当于重新起一条线，否则旧锚点的朝向会限制新模式
        lineAxis = null;
        notifyActionBar("§b放置限制: §f" + mode.getDisplayValue());
        return true;
    }

    @SubscribeEvent
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isEnabled() || !event.isUseItem()) return;
        // Ctrl + 点击是模组里"工具操作"的约定（设镜像面之类），那不是一次放置，
        // 让它取到锚点的话接下来整串连放都会对着一个错位置做判断
        if (KeyBindings.modifierDown()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        // 只管放方块，右键箱子、拉杆这些交互不受影响
        ItemStack held = player.getItemInHand(event.getHand());
        if (!(held.getItem() instanceof BlockItem)) return;

        BlockHitResult hit = (BlockHitResult) mc.hitResult;
        // 走 BlockPlaceContext 而不是 hit.relative()，草和水这类可替换方块
        // 是原地替换的，直接偏移会算错一格
        BlockPos target = new BlockPlaceContext(player, event.getHand(), held, hit).getClickedPos();

        if (anchor == null) {
            anchor = target;
            anchorFace = hit.getDirection();
            lineAxis = null;
            return;
        }
        if (allows(target, hit.getDirection())) return;

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    private boolean allows(BlockPos target, Direction face) {
        return switch (mode.get()) {
            case LAYER -> target.getY() == anchor.getY();
            case PLANE -> sameOnAxis(anchorFace.getAxis(), target);
            case FACE -> face == anchorFace;
            case COLUMN -> target.getX() == anchor.getX() && target.getZ() == anchor.getZ();
            case LINE -> onLockedLine(target);
        };
    }

    private boolean sameOnAxis(Direction.Axis axis, BlockPos target) {
        return axis.choose(target.getX(), target.getY(), target.getZ())
            == axis.choose(anchor.getX(), anchor.getY(), anchor.getZ());
    }

    private boolean onLockedLine(BlockPos target) {
        int dx = target.getX() - anchor.getX();
        int dy = target.getY() - anchor.getY();
        int dz = target.getZ() - anchor.getZ();

        int offAxes = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
        if (offAxes == 0) return true;
        if (offAxes > 1) return false;

        Direction.Axis axis = dx != 0 ? Direction.Axis.X : dy != 0 ? Direction.Axis.Y : Direction.Axis.Z;
        if (lineAxis == null) lineAxis = axis;
        return axis == lineAxis;
    }

    private void clearAnchor() {
        anchor = null;
        anchorFace = null;
        lineAxis = null;
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (anchor == null || !showAnchor.get()) return;

        int color = anchorColor.argb();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        RenderUtils.drawWorldBox(poseStack, camera, new AABB(anchor).inflate(0.003D), r, g, b, 0.9F);
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7模式: §f" + mode.getDisplayValue()
            + "\n§7锚点: §f" + (anchor == null ? "按住右键放第一个方块时确定" : anchor.toShortString())
            + "\n§7连放过程中 Ctrl + 滚轮切换模式";
    }
}
