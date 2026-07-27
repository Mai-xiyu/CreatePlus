package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.RenderUtils;

/**
 * 量尺：Ctrl+右键两点测距，显示曼哈顿 / 欧几里得 / 体积。
 */
public class MeasureRuler extends CreativePlusFeature implements WorldRenderFeature {

    private final ColorSetting color = colorSetting("color", 0xE0FFC24B);

    private BlockPos a;
    private BlockPos b;

    public MeasureRuler() {
        super("measure_ruler", "两点测距与体积（需创造）", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        a = b = null;
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7量尺只在创造模式下生效");
        } else {
            notifyPlayer("§b[CreatePlus] §7量尺：Ctrl+右键点第一点，再点第二点");
        }
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        a = b = null;
    }

    @SubscribeEvent
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isEnabled() || !event.isUseItem() || !CreativeGate.allow()) return;
        if (!KeyBindings.modifierDown()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        if (a == null || (a != null && b != null)) {
            a = pos;
            b = null;
            notifyActionBar("§a量尺 A: §f" + a.toShortString());
        } else {
            b = pos;
            notifyActionBar(describe());
        }
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    private String describe() {
        if (a == null || b == null) return "";
        int dx = Math.abs(a.getX() - b.getX()) + 1;
        int dy = Math.abs(a.getY() - b.getY()) + 1;
        int dz = Math.abs(a.getZ() - b.getZ()) + 1;
        double euclid = Math.sqrt(
            Math.pow(a.getX() - b.getX(), 2)
                + Math.pow(a.getY() - b.getY(), 2)
                + Math.pow(a.getZ() - b.getZ(), 2));
        return String.format("§e量尺 §f%d×%d×%d §7(= %d) §8· §f欧氏 %.1f",
            dx, dy, dz, dx * dy * dz, euclid);
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (!CreativeGate.allow() || a == null) return;
        int c = color.argb();
        float r = ((c >> 16) & 0xFF) / 255.0F;
        float g = ((c >> 8) & 0xFF) / 255.0F;
        float bl = (c & 0xFF) / 255.0F;
        float alpha = ((c >>> 24) & 0xFF) / 255.0F;

        try (RenderUtils.LineBatch batch = RenderUtils.beginWorldLines(poseStack, camera)) {
            batch.box(new AABB(a).inflate(0.02D), r, g, bl, alpha);
            if (b != null) {
                batch.box(new AABB(b).inflate(0.02D), r, g, bl, alpha);
                batch.line(
                    a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5,
                    b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5,
                    r, g, bl, alpha);
                BlockPos min = new BlockPos(
                    Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
                BlockPos max = new BlockPos(
                    Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
                batch.box(new AABB(min, max.offset(1, 1, 1)), r, g, bl, alpha * 0.55F);
            }
        }
    }

    @Override
    public String getDetailedDescription() {
        StringBuilder text = new StringBuilder(getTranslatedDescription());
        text.append("\n§7Ctrl + 右键取点");
        if (a != null && b != null) text.append("\n").append(describe());
        else if (a != null) text.append("\n§7A: §f").append(a.toShortString());
        text.append(CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
        return text.toString();
    }
}
