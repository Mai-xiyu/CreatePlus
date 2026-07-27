package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.utils.BlockEdits;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.RenderUtils;

/**
 * 对称建造：放置方块时自动在镜像位置也放一个。
 *
 * Ctrl + 右键设置镜像面，Ctrl + 左键清除。镜像面会在世界里画出来，
 * 免得建到一半忘了对称轴在哪。
 */
public class MirrorBuild extends CreativePlusFeature implements WorldRenderFeature {

    public enum Axis {
        /** 东西方向对称（镜像 X）。 */
        X,
        /** 南北方向对称（镜像 Z）。 */
        Z,
        /** 四向对称。 */
        BOTH
    }

    private static final int PLANE_HALF_LENGTH = 24;

    private final EnumSetting<Axis> axis = enumSetting("axis", Axis.X);
    private final BooleanSetting showPlane = boolSetting("show_plane", true);
    private final ColorSetting planeColor = colorSetting("plane_color", 0xFFFFC24B);

    private BlockPos mirrorPoint;
    /** 自己发出的镜像放置也会触发交互事件，用这个标记避免无限递归。 */
    private boolean mirroring;

    public MirrorBuild() {
        super("mirror_build", "放置方块时在对称位置同步放置", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        notifyPlayer("§b[CreatePlus] §7对称建造已启用：§fCtrl + 右键§7 设置镜像面，§fCtrl + 左键§7 清除");
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        mirrorPoint = null;
    }

    /** Ctrl + 滚轮切换对称轴。 */
    @Override
    public boolean handleScroll(double delta) {
        if (!KeyBindings.modifierDown() || mirrorPoint == null) return false;
        axis.cycle(delta > 0 ? 1 : -1);
        notifyActionBar("§b对称轴: §f" + axis.getDisplayValue());
        return true;
    }

    /**
     * Ctrl + 左右键设置/清除镜像面，并且不让这次点击真的动到方块。
     *
     * 拦截点必须是按键映射事件：{@code PlayerInteractEvent} 被取消之后，
     * 客户端仍然会把交互数据包发给服务端，创造模式下照样会破坏或放置方块。
     */
    @SubscribeEvent
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isEnabled() || !KeyBindings.modifierDown()) return;
        if (!event.isAttack() && !event.isUseItem()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        if (event.isAttack()) {
            mirrorPoint = null;
            notifyActionBar("§c已清除镜像面");
        } else {
            mirrorPoint = ((BlockHitResult) mc.hitResult).getBlockPos();
            notifyActionBar("§a镜像面: §f" + mirrorPoint.toShortString()
                + " §7(" + axis.getDisplayValue() + ")");
        }

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!isEnabled() || mirroring || !event.getLevel().isClientSide()) return;
        if (KeyBindings.modifierDown()) return;
        if (mirrorPoint == null) return;
        if (!(event.getItemStack().getItem() instanceof BlockItem)) return;

        BlockHitResult hit = event.getHitVec();
        BlockPos placed = hit.getBlockPos().relative(hit.getDirection());

        mirroring = true;
        try {
            switch (axis.get()) {
                case X -> mirrorPlace(mirrorX(placed));
                case Z -> mirrorPlace(mirrorZ(placed));
                case BOTH -> {
                    mirrorPlace(mirrorX(placed));
                    mirrorPlace(mirrorZ(placed));
                    mirrorPlace(mirrorZ(mirrorX(placed)));
                }
            }
        } finally {
            mirroring = false;
        }
    }

    private void mirrorPlace(BlockPos target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!mc.level.getBlockState(target).isAir()) return;
        BlockEdits.placeWithHeldItem(target, InteractionHand.MAIN_HAND);
    }

    private BlockPos mirrorX(BlockPos pos) {
        return new BlockPos(2 * mirrorPoint.getX() - pos.getX(), pos.getY(), pos.getZ());
    }

    private BlockPos mirrorZ(BlockPos pos) {
        return new BlockPos(pos.getX(), pos.getY(), 2 * mirrorPoint.getZ() - pos.getZ());
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (mirrorPoint == null || !showPlane.get()) return;

        int color = planeColor.argb();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        double cx = mirrorPoint.getX() + 0.5D;
        double cz = mirrorPoint.getZ() + 0.5D;
        double y = mirrorPoint.getY() + 0.02D;

        if (axis.get() != Axis.Z) {
            RenderUtils.drawWorldLine(poseStack, camera,
                cx, y, cz - PLANE_HALF_LENGTH, cx, y, cz + PLANE_HALF_LENGTH, r, g, b, 0.9F);
        }
        if (axis.get() != Axis.X) {
            RenderUtils.drawWorldLine(poseStack, camera,
                cx - PLANE_HALF_LENGTH, y, cz, cx + PLANE_HALF_LENGTH, y, cz, r, g, b, 0.9F);
        }
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7Ctrl + 右键设置镜像面，Ctrl + 左键清除，Ctrl + 滚轮换轴"
            + "\n§7对称轴: §f" + axis.getDisplayValue()
            + "\n§7镜像面: §f" + (mirrorPoint == null ? "未设置" : mirrorPoint.toShortString());
    }
}
