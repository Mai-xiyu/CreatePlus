package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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

/**
 * 范围破坏。
 *
 * 和范围放置一样，必须按住攻击键才会生效 —— 旧实现只要功能开着就一直在
 * 挖玩家看向的一切，走到哪拆到哪。
 */
public class Nucker extends CreativePlusFeature implements WorldRenderFeature {

    private final EnumSetting<Shapes.Shape> shape = enumSetting("shape", Shapes.Shape.SPHERE);
    private final IntSetting radius = intSetting("radius", 2, 1, 8);
    private final IntSetting delay = intSetting("delay", 1, 0, 20);
    private final BooleanSetting showPreview = boolSetting("show_preview", true);
    private final BooleanSetting keepFloor = boolSetting("keep_floor", true);

    private int cooldown;

    public Nucker() {
        super("nucker", "按住攻击键范围破坏方块", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        cooldown = 0;
        notifyPlayer("§b[CreatePlus] §7范围破坏已启用：按住左键生效，Ctrl + 滚轮调半径");
    }

    @Override
    public boolean handleScroll(double delta) {
        if (!KeyBindings.modifierDown()) return false;
        radius.cycle(delta > 0 ? 1 : -1);
        notifyActionBar("§b范围破坏半径: §f" + radius.value());
        return true;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.isCreative()) return;
        if (mc.screen != null || !mc.options.keyAttack.isDown()) {
            cooldown = 0;
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }
        cooldown = delay.value();

        BlockPos center = targetBlock(mc);
        if (center == null) return;

        BlockPos feet = mc.player.blockPosition().below();
        double reach = mc.player.getBlockReach() + radius.value();

        for (BlockPos pos : Shapes.collect(shape.get(), center, radius.value())) {
            if (mc.level.getBlockState(pos).isAir()) continue;
            // 别把自己脚下挖穿，否则一边飞一边挖会直接掉下去
            if (keepFloor.get() && pos.equals(feet)) continue;
            if (mc.player.position().distanceToSqr(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > reach * reach) continue;
            BlockEdits.breakBlock(pos);
        }
    }

    private static BlockPos targetBlock(Minecraft mc) {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return null;
        return ((BlockHitResult) mc.hitResult).getBlockPos();
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (!showPreview.get()) return;

        Minecraft mc = Minecraft.getInstance();
        BlockPos center = targetBlock(mc);
        if (center == null) return;

        RenderUtils.drawWorldBox(poseStack, camera,
            new AABB(center).inflate(radius.value()), 1.0F, 0.35F, 0.35F, 0.5F);
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7形状: §f" + shape.getDisplayValue() + " §7半径: §f" + radius.value()
            + "\n§7按住左键生效，Ctrl + 滚轮调半径";
    }
}
