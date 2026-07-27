package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.utils.BlockEdits;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.RenderUtils;
import org.xiyu.yee.createplus.utils.Shapes;

import java.util.List;

/**
 * 画笔式范围放置：按住右键就在准星周围铺一片方块。
 *
 * 旧实现是"只要副手拿着方块就每 tick 无条件放置"，
 * 走过路过都在刷方块，而且没有任何范围提示。现在必须按住使用键，
 * 并且会把即将影响的范围画出来。
 */
public class AreaPlace extends CreativePlusFeature implements WorldRenderFeature {
    /** 预览最多勾几个格子，再多就只是糊成一团还掉帧。 */
    private static final int PREVIEW_LIMIT = 96;

    private final EnumSetting<Shapes.Shape> shape = enumSetting("shape", Shapes.Shape.SPHERE);
    private final IntSetting radius = intSetting("radius", 2, 1, 8);
    private final IntSetting delay = intSetting("delay", 2, 0, 20);
    private final BooleanSetting showPreview = boolSetting("show_preview", true);
    private final BooleanSetting offhand = boolSetting("use_offhand", false);

    private int cooldown;

    public AreaPlace() {
        super("area_place", "按住使用键在准星周围范围放置方块", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        cooldown = 0;
        notifyPlayer("§b[CreatePlus] §7范围放置已启用：手持方块按住右键，Ctrl + 滚轮调半径");
    }

    /** Ctrl + 滚轮调节半径。 */
    @Override
    public boolean handleScroll(double delta) {
        if (!KeyBindings.modifierDown()) return false;
        radius.cycle(delta > 0 ? 1 : -1);
        notifyActionBar("§b范围放置半径: §f" + radius.value());
        return true;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.isCreative()) return;
        if (mc.screen != null) return;

        if (!mc.options.keyUse.isDown()) {
            cooldown = 0;
            return;
        }

        InteractionHand hand = offhand.get() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!(mc.player.getItemInHand(hand).getItem() instanceof BlockItem)) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }
        cooldown = delay.value();

        BlockPos center = targetCenter(mc);
        if (center == null) return;

        double reach = mc.player.getBlockReach() + radius.value();
        for (BlockPos pos : Shapes.collect(shape.get(), center, radius.value())) {
            if (!mc.level.getBlockState(pos).isAir()) continue;
            if (mc.player.position().distanceToSqr(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > reach * reach) continue;
            BlockEdits.placeWithHeldItem(pos, hand);
        }
    }

    private static BlockPos targetCenter(Minecraft mc) {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;
        BlockHitResult hit = (BlockHitResult) mc.hitResult;
        return hit.getBlockPos().relative(hit.getDirection());
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (!showPreview.get()) return;

        Minecraft mc = Minecraft.getInstance();
        BlockPos center = targetCenter(mc);
        if (center == null) return;

        int r = radius.value();
        List<BlockPos> positions = Shapes.collect(shape.get(), center, r);

        // 一次批次画完所有格子：逐个调用 drawWorldBox 会提交上百个绘制批次
        try (RenderUtils.LineBatch batch = RenderUtils.beginWorldLines(poseStack, camera)) {
            batch.box(new AABB(center).inflate(r), 0.2F, 0.8F, 1.0F, 0.45F);

            int drawn = 0;
            for (BlockPos pos : positions) {
                if (drawn >= PREVIEW_LIMIT) break;
                if (mc.level == null || !mc.level.getBlockState(pos).isAir()) continue;
                batch.box(new AABB(pos).deflate(0.02D), 0.4F, 1.0F, 0.6F, 0.25F);
                drawn++;
            }
        }
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7形状: §f" + shape.getDisplayValue() + " §7半径: §f" + radius.value()
            + "\n§7按住右键生效，Ctrl + 滚轮调半径"
            + (offhand.get() ? "\n§7使用副手物品" : "");
    }
}
